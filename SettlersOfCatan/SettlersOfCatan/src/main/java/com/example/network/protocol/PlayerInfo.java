package com.example.network.protocol;

/**
 * Visão serializável de um jogador dentro de uma sala, enviada ao cliente
 * para montar a tela de lobby. Não contém dados sensíveis (mão, cartas).
 */
public class PlayerInfo {

    private String name;
    private String color;
    private boolean ready;
    private boolean host;

    public PlayerInfo() {}

    public PlayerInfo(String name, String color, boolean ready, boolean host) {
        this.name = name;
        this.color = color;
        this.ready = ready;
        this.host = host;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }
}
