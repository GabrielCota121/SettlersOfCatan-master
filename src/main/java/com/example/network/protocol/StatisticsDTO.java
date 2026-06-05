package com.example.network.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Estatísticas da partida, enviadas no snapshot quando o jogo termina,
 * para alimentar o overlay de fim de jogo nos clientes.
 */
public class StatisticsDTO {
    private int totalTurns;
    // número do dado (2..12) -> quantas vezes saiu
    private Map<Integer, Integer> diceRollCounts = new HashMap<>();
    // nome do jogador -> (recurso -> quantidade ganha)
    private Map<String, Map<String, Integer>> resourcesGained = new HashMap<>();
    // nome do jogador -> dev cards compradas
    private Map<String, Integer> devCardsDrawn = new HashMap<>();
    // nome do jogador -> trocas com banco
    private Map<String, Integer> bankTrades = new HashMap<>();
    // nome do jogador -> trocas com jogadores
    private Map<String, Integer> playerTrades = new HashMap<>();

    public int getTotalTurns() { return totalTurns; }
    public void setTotalTurns(int v) { this.totalTurns = v; }
    public Map<Integer, Integer> getDiceRollCounts() { return diceRollCounts; }
    public void setDiceRollCounts(Map<Integer, Integer> v) {
        this.diceRollCounts = v == null ? new HashMap<>() : v; }
    public Map<String, Map<String, Integer>> getResourcesGained() { return resourcesGained; }
    public void setResourcesGained(Map<String, Map<String, Integer>> v) {
        this.resourcesGained = v == null ? new HashMap<>() : v; }
    public Map<String, Integer> getDevCardsDrawn() { return devCardsDrawn; }
    public void setDevCardsDrawn(Map<String, Integer> v) {
        this.devCardsDrawn = v == null ? new HashMap<>() : v; }
    public Map<String, Integer> getBankTrades() { return bankTrades; }
    public void setBankTrades(Map<String, Integer> v) {
        this.bankTrades = v == null ? new HashMap<>() : v; }
    public Map<String, Integer> getPlayerTrades() { return playerTrades; }
    public void setPlayerTrades(Map<String, Integer> v) {
        this.playerTrades = v == null ? new HashMap<>() : v; }
}
