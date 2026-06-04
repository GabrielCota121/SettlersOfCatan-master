package com.example.network.room;

import com.example.network.protocol.PlayerInfo;
import com.example.network.protocol.RoomInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Sala de jogo (lado servidor). Guarda os jogadores conectados e o estado do
 * lobby. O primeiro jogador a entrar é o "host" e pode iniciar a partida.
 *
 * <p>Fase futura (servidor autoritativo): esta classe passará a guardar também
 * o {@code CatanGameManager} autoritativo da partida desta sala.
 *
 * <p>Thread-safety: todos os mutadores são {@code synchronized}; iterações
 * externas devem usar a cópia retornada por {@link #snapshotPlayers()}.
 */
public class Room {

    private final String id;
    private final String name;
    private final int maxPlayers;
    private final List<RoomPlayer> players = new ArrayList<>();
    private RoomStatus status = RoomStatus.WAITING;

    public Room(String id, String name, int maxPlayers) {
        this.id = id;
        this.name = name;
        this.maxPlayers = Math.max(2, Math.min(maxPlayers, 4)); // Catan: 2 a 4 jogadores
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getMaxPlayers() { return maxPlayers; }
    public synchronized RoomStatus getStatus() { return status; }
    public synchronized void setStatus(RoomStatus status) { this.status = status; }

    public synchronized boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    /** Tenta adicionar um jogador. Retorna false se cheia, em jogo ou nome/cor duplicados. */
    public synchronized boolean addPlayer(RoomPlayer player) {
        if (status != RoomStatus.WAITING || isFull()) return false;
        for (RoomPlayer p : players) {
            if (p.getName().equalsIgnoreCase(player.getName())) return false;
        }
        players.add(player);
        return true;
    }

    public synchronized RoomPlayer removePlayer(String sessionId) {
        RoomPlayer found = findBySession(sessionId);
        if (found != null) players.remove(found);
        return found;
    }

    public synchronized RoomPlayer findBySession(String sessionId) {
        for (RoomPlayer p : players) {
            if (p.getSessionId().equals(sessionId)) return p;
        }
        return null;
    }

    /** O host é sempre o jogador mais antigo ainda presente na sala. */
    public synchronized String getHostSessionId() {
        return players.isEmpty() ? null : players.get(0).getSessionId();
    }

    public synchronized boolean isHost(String sessionId) {
        return sessionId != null && sessionId.equals(getHostSessionId());
    }

    public synchronized boolean allReady() {
        if (players.size() < 2) return false;
        for (RoomPlayer p : players) {
            // O host não precisa marcar "pronto"; ele dispara o início.
            if (!p.getSessionId().equals(getHostSessionId()) && !p.isReady()) return false;
        }
        return true;
    }

    /** Cópia imutável da lista de jogadores, segura para iterar fora do lock. */
    public synchronized List<RoomPlayer> snapshotPlayers() {
        return new ArrayList<>(players);
    }

    /** Preenche vagas restantes com bots usando cores únicas não ocupadas por humanos. */
    public synchronized void fillWithBots() {
        String[] coresPossiveis = {"RED", "BLUE", "GREEN", "ORANGE", "PURPLE", "BLACK"};
        java.util.Set<String> usadas = new java.util.HashSet<>();
        for (RoomPlayer p : players) usadas.add(p.getColor());

        int botNum = 1;
        while (players.size() < maxPlayers) {
            String cor = null;
            for (String c : coresPossiveis) {
                if (!usadas.contains(c)) { cor = c; break; }
            }
            if (cor == null) break;
            usadas.add(cor);
            String nome = "Bot " + botNum++;
            players.add(new RoomPlayer("BOT-" + nome, nome, cor, true));
        }
    }

    /** Converte para o DTO enviado aos clientes. */
    public synchronized RoomInfo toInfo() {
        RoomInfo info = new RoomInfo();
        info.setId(id);
        info.setName(name);
        info.setMaxPlayers(maxPlayers);
        info.setStatus(status.name());
        String hostId = getHostSessionId();
        List<PlayerInfo> infos = new ArrayList<>();
        for (RoomPlayer p : players) {
            infos.add(p.toInfo(p.getSessionId().equals(hostId)));
        }
        info.setPlayers(infos);
        return info;
    }
}
