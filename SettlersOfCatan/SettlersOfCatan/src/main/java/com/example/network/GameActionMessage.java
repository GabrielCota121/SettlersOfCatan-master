package com.example.network;

public class GameActionMessage {
    private String action;     // "ROLL_DICE", "BUILD_ROAD", "BUILD_SETTLEMENT", "END_TURN"
    private String playerName; // Quem está enviando
    private Integer targetId;  // O ID da Aresta, Vértice ou Hexágono clicado (se houver)

    // Construtores, Getters e Setters
    public GameActionMessage() {}

    public GameActionMessage(String action, String playerName, Integer targetId) {
        this.action = action;
        this.playerName = playerName;
        this.targetId = targetId;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }
}