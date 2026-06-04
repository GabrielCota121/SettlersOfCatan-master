package com.example.network;

import com.example.network.protocol.MessageType;
import com.example.network.protocol.NetworkMessage;
import com.example.network.room.GameSession;
import com.example.network.room.Room;
import com.example.network.room.RoomManager;
import com.example.network.room.RoomPlayer;
import com.example.network.room.RoomStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WebSocket do servidor. Recebe {@link NetworkMessage} dos clientes,
 * gerencia salas via {@link RoomManager} e faz broadcast apenas para os membros
 * da sala correta (não mais um eco para todos).
 *
 * <p>Cada sessão guarda em {@code session.getAttributes()} o {@code roomId} e o
 * {@code playerName} associados, usados na desconexão para limpar a sala.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_ROOM = "roomId";
    private static final String ATTR_NAME = "playerName";

    /** Paleta padrão do Catan, usada quando o cliente não escolhe uma cor livre. */
    private static final String[] DEFAULT_COLORS = {"RED", "BLUE", "ORANGE", "GREEN", "WHITE", "PURPLE"};

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, GameSession> games = new ConcurrentHashMap<>(); // roomId -> partida autoritativa
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoomManager roomManager;

    public GameWebSocketHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("Cliente conectado: " + session.getId() + " | Total: " + sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        NetworkMessage msg;
        try {
            msg = objectMapper.readValue(message.getPayload(), NetworkMessage.class);
        } catch (Exception e) {
            System.out.println("Mensagem JSON inválida: " + e.getMessage());
            sendTo(session, error("Mensagem inválida."));
            return;
        }

        String type = msg.getType();
        if (type == null) {
            sendTo(session, error("Mensagem sem 'type'."));
            return;
        }

        try {
            switch (type) {
                case MessageType.LIST_ROOMS  -> handleListRooms(session);
                case MessageType.CREATE_ROOM -> handleCreateRoom(session, msg);
                case MessageType.JOIN_ROOM   -> handleJoinRoom(session, msg);
                case MessageType.LEAVE_ROOM  -> handleLeaveRoom(session);
                case MessageType.SET_READY   -> handleSetReady(session, msg);
                case MessageType.START_GAME  -> handleStartGame(session);
                case MessageType.GAME_ACTION -> handleGameAction(session, msg);
                default -> sendTo(session, error("Tipo desconhecido: " + type));
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar " + type + ": " + e.getMessage());
            sendTo(session, error("Falha ao processar " + type + "."));
        }
    }

    // ----------------- Handlers de lobby -----------------

    private void handleListRooms(WebSocketSession session) {
        sendTo(session, NetworkMessage.of(MessageType.ROOM_LIST).put("rooms", roomManager.listRooms()));
    }

    private void handleCreateRoom(WebSocketSession session, NetworkMessage msg) {
        String playerName = requireName(session, msg);
        if (playerName == null) return;

        String roomName = msg.getString("roomName");
        int maxPlayers = msg.getInt("maxPlayers", 4);

        Room room = roomManager.createRoom(roomName, maxPlayers);
        String color = pickColor(room, msg.getString("color"));
        room.addPlayer(new RoomPlayer(session.getId(), playerName, color));

        bindSessionToRoom(session, room.getId(), playerName);
        sendTo(session, NetworkMessage.of(MessageType.ROOM_JOINED).put("room", room.toInfo()));
        broadcastRoomList();
        System.out.println(playerName + " criou a sala " + room.getId() + " (" + room.getName() + ")");
    }

    private void handleJoinRoom(WebSocketSession session, NetworkMessage msg) {
        String playerName = requireName(session, msg);
        if (playerName == null) return;

        Room room = roomManager.getRoom(msg.getRoomId());
        if (room == null) {
            sendTo(session, error("Sala não encontrada."));
            return;
        }
        if (room.getStatus() != RoomStatus.WAITING) {
            sendTo(session, error("A partida nesta sala já começou."));
            return;
        }
        if (room.isFull()) {
            sendTo(session, error("Sala cheia."));
            return;
        }

        String color = pickColor(room, msg.getString("color"));
        boolean ok = room.addPlayer(new RoomPlayer(session.getId(), playerName, color));
        if (!ok) {
            sendTo(session, error("Não foi possível entrar (nome em uso ou sala cheia)."));
            return;
        }

        bindSessionToRoom(session, room.getId(), playerName);
        sendTo(session, NetworkMessage.of(MessageType.ROOM_JOINED).put("room", room.toInfo()));
        broadcastToRoom(room, NetworkMessage.of(MessageType.ROOM_UPDATE).put("room", room.toInfo()), session.getId());
        broadcastRoomList();
        System.out.println(playerName + " entrou na sala " + room.getId());
    }

    private void handleLeaveRoom(WebSocketSession session) {
        Room room = currentRoom(session);
        if (room == null) return;

        room.removePlayer(session.getId());
        clearSessionRoom(session);
        sendTo(session, NetworkMessage.of(MessageType.ROOM_LEFT));

        if (room.isEmpty()) {
            games.remove(room.getId());
            roomManager.removeIfEmpty(room.getId());
        } else {
            broadcastToRoom(room, NetworkMessage.of(MessageType.ROOM_UPDATE).put("room", room.toInfo()), null);
        }
        broadcastRoomList();
    }

    private void handleSetReady(WebSocketSession session, NetworkMessage msg) {
        Room room = currentRoom(session);
        if (room == null) return;
        RoomPlayer player = room.findBySession(session.getId());
        if (player == null) return;

        player.setReady(msg.getBoolean("ready", !player.isReady()));
        broadcastToRoom(room, NetworkMessage.of(MessageType.ROOM_UPDATE).put("room", room.toInfo()), null);
    }

    private void handleStartGame(WebSocketSession session) {
        Room room = currentRoom(session);
        if (room == null) return;
        if (!room.isHost(session.getId())) {
            sendTo(session, error("Apenas o host pode iniciar a partida."));
            return;
        }
        if (!room.allReady()) {
            sendTo(session, error("Todos os jogadores precisam estar prontos (mínimo 2)."));
            return;
        }
        // Se a sala não está cheia, completa as vagas com bots
        room.fillWithBots();
        room.setStatus(RoomStatus.IN_GAME);

        // Instancia a partida AUTORITATIVA da sala. A seed determina o tabuleiro,
        // que os clientes reconstroem idêntico ao receber GAME_STARTED.
        long seed = System.nanoTime();
        final String roomId = room.getId();
        GameSession game = new GameSession(room.snapshotPlayers(), seed,
                logLine -> broadcastToRoom(room, NetworkMessage.of(MessageType.GAME_EVENT).put("message", logLine), null));
        games.put(roomId, game);

        broadcastToRoom(room, NetworkMessage.of(MessageType.GAME_STARTED)
                .put("room", room.toInfo())
                .put("seed", seed), null);
        broadcastPersonalizedState(room, game);
        // Se o primeiro jogador da ordem for bot, ele já joga.
        processBotTurns(room, game);
        broadcastRoomList();
        System.out.println("Partida iniciada na sala " + roomId + " (seed=" + seed + ")");
    }

    private void handleGameAction(WebSocketSession session, NetworkMessage msg) {
        Room room = currentRoom(session);
        if (room == null) return;
        GameSession game = games.get(room.getId());
        if (game == null) {
            sendTo(session, error("Partida não iniciada nesta sala."));
            return;
        }

        String sender = (String) session.getAttributes().get(ATTR_NAME);
        boolean applied = game.applyAction(sender, msg.getString("action"), msg.getString("targetId"));

        if (applied) {
            // Notifica todos com estado atualizado
            broadcastPersonalizedState(room, game);

            // Processa turnos de bots que sejam o jogador da vez, em sequência.
            processBotTurns(room, game);

            // Se o estado mudou para WaitingDiscard, avisa explicitamente quem precisa descartar
            com.example.network.protocol.GameStateDTO snap = game.snapshot();
            if (!snap.getDiscardPendingPlayers().isEmpty()) {
                broadcastToRoom(room,
                    NetworkMessage.of(MessageType.DISCARD_REQUIRED)
                        .put("players", snap.getDiscardPendingPlayers())
                        .put("amounts", snap.getDiscardAmounts()),
                    null);
            }
            // Se ainda há pendentes após um SUBMIT_DISCARD, avisa quem falta
            if ("SUBMIT_DISCARD".equals(msg.getString("action")) &&
                    !snap.getDiscardPendingPlayers().isEmpty()) {
                broadcastToRoom(room,
                    NetworkMessage.of(MessageType.DISCARD_WAITING)
                        .put("remaining", snap.getDiscardPendingPlayers()),
                    null);
            }
            // Broadcast do estado da negociação para todos os jogadores da sala
            if (snap.getActiveTrade() != null) {
                broadcastToRoom(room,
                    NetworkMessage.of(MessageType.TRADE_UPDATE)
                        .put("trade", snap.getActiveTrade()),
                    null);
            } else {
                // Se uma troca acabou de ser resolvida/cancelada, envia o
                // estado final UMA vez para os clientes fecharem o painel
                com.example.network.protocol.TradeStatusDTO resolved =
                    game.consumeLastResolvedTrade();
                if (resolved != null) {
                    broadcastToRoom(room,
                        NetworkMessage.of(MessageType.TRADE_UPDATE)
                            .put("trade", resolved),
                        null);
                }
            }
        } else {
            sendTo(session,
                NetworkMessage.of(MessageType.GAME_STATE).put("state", game.snapshot(sender)));
        }
    }

    private void processBotTurns(Room room, GameSession game) {
        int seguranca = 0;
        while (game.isCurrentPlayerBot() && seguranca++ < 20) {
            boolean jogou = game.runBotTurnIfNeeded();
            if (!jogou) break; // bot em estado especial (setup/ladrão/descarte)

            try { Thread.sleep(800); } catch (InterruptedException ignored) {}

            broadcastPersonalizedState(room, game);
        }
    }

    /** Envia um snapshot personalizado para cada membro da sala (cada um vê só a própria mão). */
    private void broadcastPersonalizedState(Room room, GameSession game) {
        for (RoomPlayer rp : room.snapshotPlayers()) {
            WebSocketSession s = sessions.get(rp.getSessionId());
            if (s != null && s.isOpen()) {
                sendTo(s, NetworkMessage.of(MessageType.GAME_STATE).put("state", game.snapshot(rp.getName())));
            }
        }
    }

    // ----------------- Conexão encerrada -----------------

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        Room room = currentRoom(session);
        if (room != null) {
            room.removePlayer(session.getId());
            if (room.isEmpty()) {
                games.remove(room.getId());
                roomManager.removeIfEmpty(room.getId());
            } else {
                broadcastToRoom(room, NetworkMessage.of(MessageType.ROOM_UPDATE).put("room", room.toInfo()), null);
            }
            broadcastRoomList();
        }
        System.out.println("Cliente desconectado: " + session.getId() + " | Restam: " + sessions.size());
    }

    // ----------------- Auxiliares -----------------

    private String requireName(WebSocketSession session, NetworkMessage msg) {
        String name = msg.getSenderName();
        if (name == null || name.isBlank()) {
            sendTo(session, error("Informe um nome de jogador."));
            return null;
        }
        return name.trim();
    }

    private String pickColor(Room room, String requested) {
        if (requested != null && !requested.isBlank() && isColorFree(room, requested)) {
            return requested.toUpperCase();
        }
        for (String c : DEFAULT_COLORS) {
            if (isColorFree(room, c)) return c;
        }
        return DEFAULT_COLORS[0];
    }

    private boolean isColorFree(Room room, String color) {
        for (RoomPlayer p : room.snapshotPlayers()) {
            if (p.getColor() != null && p.getColor().equalsIgnoreCase(color)) return false;
        }
        return true;
    }

    private void bindSessionToRoom(WebSocketSession session, String roomId, String playerName) {
        session.getAttributes().put(ATTR_ROOM, roomId);
        session.getAttributes().put(ATTR_NAME, playerName);
    }

    private void clearSessionRoom(WebSocketSession session) {
        session.getAttributes().remove(ATTR_ROOM);
    }

    private Room currentRoom(WebSocketSession session) {
        Object roomId = session.getAttributes().get(ATTR_ROOM);
        return roomId == null ? null : roomManager.getRoom(roomId.toString());
    }

    private NetworkMessage error(String text) {
        return NetworkMessage.of(MessageType.ERROR).put("message", text);
    }

    private void sendTo(WebSocketSession session, NetworkMessage msg) {
        if (session == null || !session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(msg);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            System.out.println("Falha ao enviar para " + session.getId() + ": " + e.getMessage());
        }
    }

    /** Envia para todos os membros da sala, opcionalmente excluindo uma sessão. */
    private void broadcastToRoom(Room room, NetworkMessage msg, String excludeSessionId) {
        List<RoomPlayer> players = room.snapshotPlayers();
        String json;
        try {
            json = objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            System.out.println("Falha ao serializar broadcast: " + e.getMessage());
            return;
        }
        for (RoomPlayer p : players) {
            if (p.getSessionId().equals(excludeSessionId)) continue;
            WebSocketSession s = sessions.get(p.getSessionId());
            if (s != null && s.isOpen()) {
                try {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    System.out.println("Falha ao enviar broadcast para " + p.getSessionId());
                }
            }
        }
    }

    /** Atualiza a lista de salas para todos os clientes conectados (úteis no lobby). */
    private void broadcastRoomList() {
        NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST).put("rooms", roomManager.listRooms());
        String json;
        try {
            json = objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            return;
        }
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try {
                    synchronized (s) {
                        s.sendMessage(new TextMessage(json));
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }
}
