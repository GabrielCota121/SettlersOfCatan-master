package com.catan.model.player;

import com.catan.model.building.BuildingCost;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.ResourceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private int id;
    private String name;
    private String color;
    private final ResourceWallet wallet;
    private final Map<ResourceType, Integer> tradeRates;
    private int numSettlements;
    private int numCities;
    private int numRoads;
    private int victoryPoints;
    private int numKnights;
    private int longestRoad;
    private final List<IDevelopmentCard> playableCards = new ArrayList<>();
    private final List<IDevelopmentCard> newCards = new ArrayList<>();

    public Player(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.wallet = new ResourceWallet();
        this.tradeRates = new HashMap<>();
        this.numSettlements = 0;
        this.numCities = 0;
        this.numRoads = 0;
        this.victoryPoints = 0;
        this.numKnights = 0;
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                tradeRates.put(type, 4);
            }
        }
    }

    public void setTradeRate(ResourceType type, int newRate) {
        if (tradeRates.getOrDefault(type, 4) > newRate) {
            tradeRates.put(type, newRate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id == player.id;
    }

    @Override
    public int hashCode() {return java.util.Objects.hash(id);}

    // Só vai usar isso quando for trocar com o banco, pra poder ver quantas cartas de um tipo precisa pra trocar!
    public int getTradeRate(ResourceType type) {
        return tradeRates.getOrDefault(type, 4);
    }

    // Usnado polimorfismo da IDevelopmentCard pra decidir em qual mão vai entrar!
    public void buyDevelopmentCard(IDevelopmentCard card) {
        card.onPurchase(this);
    }

    public void addNewCard(IDevelopmentCard card) {
        newCards.add(card);
    }

    public void addPlayableCard(IDevelopmentCard card) {
        playableCards.add(card);
    }

    public void incrementKnightsPlayed() {this.numKnights++;}

    public void removeCard(IDevelopmentCard card) {
        playableCards.remove(card);
    }
    public void makeNewCardsPlayable() {
        if (!newCards.isEmpty()) {
            playableCards.addAll(newCards);
            newCards.clear();
        }
    }

    public boolean canAfford(BuildingCost building) {
        return wallet.hasEnoughResources(building.getCost());
    }

    public int getId() { return id; }
    public void setLongestRoad(int longestRoad) {this.longestRoad = longestRoad;}
    public int getLongestRoad() {return longestRoad;}
    public String getName() { return name; }
    public String getColor() { return color; }
    public ResourceWallet getWallet() { return wallet; }
    public int getNumSettlements() {return numSettlements;}
    public int getNumCities() {return numCities;}
    public int getNumRoads() {return numRoads;}
    public int getVictoryPoints(){return victoryPoints;}
    public int getNumKnights(){return numKnights;}
    public void incrementSettlements(){numSettlements++;}
    public void incrementCities(){numCities++;}
    public void incrementRoads(){numRoads++;}
    public void decrementSettlements(){numSettlements--;}
    public void incrementVictoryPoints(){victoryPoints++;}
    public void decrementVictoryPoints(){victoryPoints--;}
    public List<IDevelopmentCard> getPlayableCards() {return playableCards;}
    public List<IDevelopmentCard> getNewCards() {return newCards;}
    public Map<ResourceType, Integer> getTradeRates() {return tradeRates;}
}