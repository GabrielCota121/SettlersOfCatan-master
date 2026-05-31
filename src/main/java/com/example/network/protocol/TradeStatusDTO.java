package com.example.network.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Estado atual de uma negociação de troca, enviado a todos os jogadores
 * via TRADE_UPDATE para que cada um saiba quem aceitou, recusou ou ainda
 * não respondeu.
 */
public class TradeStatusDTO {

    private String proposerName;
    private Map<String, Integer> give = new HashMap<>(); // o que o proponente oferece
    private Map<String, Integer> want = new HashMap<>(); // o que o proponente quer

    // nome -> "PENDING" | "ACCEPTED" | "DECLINED"
    private Map<String, String> responses = new HashMap<>();

    private boolean active; // false quando a troca foi concluída ou cancelada
    private String resolvedWithPlayer; // nome de quem a troca foi feita (null se cancelada)

    public String getProposerName() { return proposerName; }
    public void setProposerName(String v) { this.proposerName = v; }

    public Map<String, Integer> getGive() { return give; }
    public void setGive(Map<String, Integer> v) { this.give = v; }

    public Map<String, Integer> getWant() { return want; }
    public void setWant(Map<String, Integer> v) { this.want = v; }

    public Map<String, String> getResponses() { return responses; }
    public void setResponses(Map<String, String> v) { this.responses = v; }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }

    public String getResolvedWithPlayer() { return resolvedWithPlayer; }
    public void setResolvedWithPlayer(String v) { this.resolvedWithPlayer = v; }
}
