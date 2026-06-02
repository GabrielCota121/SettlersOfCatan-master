package com.example.network.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Estado de um jogador dentro da partida (parte do {@link GameStateDTO}). */
public class PlayerStateDTO {

    private String name;
    private String color;
    private int victoryPoints;
    private int numKnights;
    private int longestRoad;
    private int numSettlements;
    private int numCities;
    private int numRoads;

    /**
     * Quando {@code true}, os campos {@link #resources} e {@link #devCards} estão
     * propositalmente vazios porque este jogador não é o destinatário do snapshot.
     * O cliente deve exibir o total via {@link #numResources} e {@link #numDevCardsTotal}
     * sem revelar o detalhamento — garantindo que ninguém veja a mão alheia.
     */
    private boolean hiddenResources = false;
    private int numResources = 0;       // total real, sempre enviado (mesmo quando hidden)
    private int numDevCardsTotal = 0;   // idem

    private Map<String, Integer> resources = new HashMap<>();
    private List<String> devCards = new ArrayList<>();
    private List<String> playableDevCards = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getColor() { return color; }
    public void setColor(String v) { this.color = v; }

    public int getVictoryPoints() { return victoryPoints; }
    public void setVictoryPoints(int v) { this.victoryPoints = v; }

    public int getNumKnights() { return numKnights; }
    public void setNumKnights(int v) { this.numKnights = v; }

    public int getLongestRoad() { return longestRoad; }
    public void setLongestRoad(int v) { this.longestRoad = v; }

    public int getNumSettlements() { return numSettlements; }
    public void setNumSettlements(int v) { this.numSettlements = v; }

    public int getNumCities() { return numCities; }
    public void setNumCities(int v) { this.numCities = v; }

    public int getNumRoads() { return numRoads; }
    public void setNumRoads(int v) { this.numRoads = v; }

    public Map<String, Integer> getResources() { return resources; }
    public void setResources(Map<String, Integer> v) { this.resources = v == null ? new HashMap<>() : v; }

    public List<String> getDevCards() { return devCards; }
    public void setDevCards(List<String> v) { this.devCards = v == null ? new ArrayList<>() : v; }

    public List<String> getPlayableDevCards() { return playableDevCards; }
    public void setPlayableDevCards(List<String> v) { this.playableDevCards = v == null ? new ArrayList<>() : v; }

    public boolean isHiddenResources() { return hiddenResources; }
    public void setHiddenResources(boolean v) { this.hiddenResources = v; }

    public int getNumResources() { return numResources; }
    public void setNumResources(int v) { this.numResources = v; }

    public int getNumDevCardsTotal() { return numDevCardsTotal; }
    public void setNumDevCardsTotal(int v) { this.numDevCardsTotal = v; }
}
