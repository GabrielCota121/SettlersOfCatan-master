package com.example.network;

import com.example.network.protocol.MessageType;
import com.example.network.protocol.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.function.Consumer;

/**
 * Cliente WebSocket usado pela interface JavaFX. Conecta ao servidor Catan,
 * envia {@link NetworkMessage} tipadas (lobby e jogo) e entrega as mensagens
 * recebidas a um listener — sempre na thread do JavaFX.
 */
public class GameWebSocketClient {

    private WebSocketSession session;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Recebe cada mensagem do servidor, já na thread JavaFX. Configurável a quente. */
    private volatile Consumer<NetworkMessage> onMessage;
    private volatile Runnable onDisconnected;

    // Identidade do jogador neste cliente, anexada às mensagens enviadas.
    private String playerName;
    private String currentRoomId;

    public GameWebSocketClient() {}

    public void setOnMessage(Consumer<NetworkMessage> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnDisconnected(Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() { return playerName; }
    public String getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Conecta de forma síncrona. Diferente da versão anterior, AGORA guarda a
     * {@link WebSocketSession} retornada — sem isso o envio nunca funcionava.
     */
    public boolean connect(String url) {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            this.session = client.execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                    dispatch(message.getPayload());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                    if (onDisconnected != null) Platform.runLater(onDisconnected);
                }
            }, null, URI.create(url)).get(); // bloqueia até conectar e ARMAZENA a sessão

            System.out.println("Conectado ao servidor de Catan: " + url);
            return true;
        } catch (Exception e) {
            System.out.println("Erro ao conectar ao WebSocket: " + e.getMessage());
            return false;
        }
    }

    private void dispatch(String payload) {
        try {
            NetworkMessage msg = objectMapper.readValue(payload, NetworkMessage.class);
            // Mantém o roomId local em dia ao entrar/sair de salas.
            if (MessageType.ROOM_JOINED.equals(msg.getType())) {
                Object room = msg.getData().get("room");
                if (room instanceof java.util.Map<?, ?> m && m.get("id") != null) {
                    this.currentRoomId = m.get("id").toString();
                }
            } else if (MessageType.ROOM_LEFT.equals(msg.getType())) {
                this.currentRoomId = null;
            }
            // Lê o handler DENTRO do runLater: assim, se a troca de tela (lobby ->
            // jogo) trocar o listener, as mensagens seguintes (ex.: o GAME_STATE
            // inicial) já caem no handler novo, na ordem correta da fila do JavaFX.
            Platform.runLater(() -> {
                Consumer<NetworkMessage> handler = this.onMessage;
                if (handler != null) handler.accept(msg);
            });
        } catch (Exception e) {
            System.out.println("Erro ao interpretar mensagem do servidor: " + e.getMessage());
        }
    }

    // ----------------- Envio -----------------

    public void send(NetworkMessage msg) {
        if (!isConnected()) {
            System.out.println("Sem conexão: mensagem '" + msg.getType() + "' não enviada.");
            return;
        }
        try {
            if (msg.getSenderName() == null) msg.sender(playerName);
            String json = objectMapper.writeValueAsString(msg);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            System.out.println("Erro ao enviar mensagem de rede: " + e.getMessage());
        }
    }

    // ----------------- Conveniências de lobby -----------------

    public void listRooms() {
        send(NetworkMessage.of(MessageType.LIST_ROOMS));
    }

    public void createRoom(String roomName, int maxPlayers, String color) {
        send(NetworkMessage.of(MessageType.CREATE_ROOM)
                .put("roomName", roomName)
                .put("maxPlayers", maxPlayers)
                .put("color", color));
    }

    public void joinRoom(String roomId, String color) {
        send(NetworkMessage.of(MessageType.JOIN_ROOM).room(roomId).put("color", color));
    }

    public void leaveRoom() {
        send(NetworkMessage.of(MessageType.LEAVE_ROOM).room(currentRoomId));
    }

    public void setReady(boolean ready) {
        send(NetworkMessage.of(MessageType.SET_READY).room(currentRoomId).put("ready", ready));
    }

    public void startGame() {
        send(NetworkMessage.of(MessageType.START_GAME).room(currentRoomId));
    }

    // ----------------- Conveniências de jogo -----------------

    /**
     * Envia uma intenção de jogo (GAME_ACTION) ao servidor autoritativo.
     * {@code targetId} é o id estável de um vértice/aresta (ou null).
     */
    public void sendIntent(String action, String targetId) {
        send(NetworkMessage.of(MessageType.GAME_ACTION)
                .room(currentRoomId)
                .sender(playerName)
                .put("action", action)
                .put("targetId", targetId));
    }

    public void disconnect() {
        try {
            if (session != null && session.isOpen()) session.close();
        } catch (Exception ignored) {
        }
    }
}
