package com.example.network.room;

import com.example.network.protocol.PlayerInfo;

/**
 * Um jogador conectado dentro de uma sala (lado servidor). Liga a identidade
 * de jogo (nome/cor) à sessão WebSocket que a transporta.
 */
public class RoomPlayer {

    private final String sessionId;
    private String name;
    private String color;
    private boolean ready;
    private boolean bot;

    public RoomPlayer(String sessionId, String name, String color) {
        this.sessionId = sessionId;
        this.name = name;
        this.color = color;
        this.ready = false;
        this.bot = false;
    }

    public RoomPlayer(String sessionId, String name, String color, boolean bot) {
        this.sessionId = sessionId;
        this.name = name;
        this.color = color;
        this.ready = true; // bots já entram prontos
        this.bot = bot;
    }

    public String getSessionId() { return sessionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isBot() { return bot; }
    public void setBot(boolean bot) { this.bot = bot; }

    public PlayerInfo toInfo(boolean isHost) {
        return new PlayerInfo(name, color, ready, isHost);
    }
}
