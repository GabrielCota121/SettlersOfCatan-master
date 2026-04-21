package com.catan.model.player;

import com.catan.model.game.ResourceType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ResourceWallet {

    private final Map<ResourceType, Integer> playerStock;
    private Runnable onWalletChanged;

    public ResourceWallet() {
        playerStock = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                playerStock.put(type, 0);
            }
        }
    }

    // método pro listebner que vai pra GUI dps
    public void setOnWalletChangedListener(Runnable listener) {
        this.onWalletChanged = listener;
    }

    private void notifyChange() {
        if (onWalletChanged != null) {
            onWalletChanged.run(); // Dispara o gatilho!
        }
    }

    public void addResource(ResourceType type, int amount) {
        int currentAmount = playerStock.get(type);
        playerStock.put(type, currentAmount + amount);
        notifyChange();
    }

    public boolean removeResource(ResourceType type, int amount) {
        int currentAmount = playerStock.get(type);
        if (currentAmount >= amount) {
            playerStock.put(type, currentAmount - amount);
            notifyChange();
            return true;
        }
        return false;
    }

    public ResourceType removeRandomResource() {
        if (getTotalCards() == 0) return null;

        List<ResourceType> pool = new ArrayList<>();

        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;
            int amount = getResourceAmount(type);
            for (int i = 0; i < amount; i++) {
                pool.add(type);
            }
        }

        java.util.Collections.shuffle(pool);
        ResourceType stolen = pool.get(0);
        removeResource(stolen, 1);
        return stolen;
    }

    public int getResourceAmount(ResourceType type) {
        return playerStock.get(type);
    }

    // os dois são bem bons, já passa o mapa ao invés de checar recurso por recurso
    // dps escolho qual usar
    public boolean hasEnoughResources(int wood, int brick, int wool, int wheat, int ore) {
        return getResourceAmount(ResourceType.WOOD) >= wood &&
                getResourceAmount(ResourceType.BRICK) >= brick &&
                getResourceAmount(ResourceType.WOOL) >= wool &&
                getResourceAmount(ResourceType.WHEAT) >= wheat &&
                getResourceAmount(ResourceType.ORE) >= ore;
    }

    public boolean hasEnoughResources(Map<ResourceType, Integer> cost) {
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            if (getResourceAmount(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // pra facilitar o ladrão pra ver se o player vai ter que descartar ou não
    public int getTotalCards() {
        int total = 0;
        for (int amount : playerStock.values()) {
            total += amount;
        }
        return total;
    }

    // Explicando o payCost abaixo usando o enum BuildingCost:
    // vai ficar mais ou menos assim:

    // Map<ResourceType, Integer> precoEstrada = BuildingCost.ROAD.getCost();
    //  if (player.getWallet().payCost(precoEstrada)) {
    //    GameLogger.log(player.getName() + " construiu uma estrada!");
    //  else
    //    não tem recursos suficientes.");
    //

    public boolean payCost(Map<ResourceType, Integer> cost) {
        if (!hasEnoughResources(cost)) {
            return false;
        }
        for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
            int amountToPay = entry.getValue();
            if (amountToPay > 0) {
                removeResource(entry.getKey(), amountToPay);
            }
        }
        return true;
    }
}