package com.catan.model.game;

import com.catan.model.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of match statistics used by UI overlays and tests.
 */
public class StatisticsManager {
    private final Map<Integer, Integer> diceRollCounts = new HashMap<>();
    private final Map<Player, Map<ResourceType, Integer>> resourcesGained = new HashMap<>();
    private final Map<Player, Integer> devCardsDrawn = new HashMap<>();
    private final Map<Player, Integer> bankTrades = new HashMap<>();
    private final Map<Player, Integer> playerTrades = new HashMap<>();
    private int totalTurns;

    public void recordTurn() {
        totalTurns++;
    }

    public void recordDiceRoll(int total) {
        diceRollCounts.merge(total, 1, Integer::sum);
    }

    public void recordResourceGained(Player player, ResourceType type, int amount) {
        if (amount <= 0) {
            return;
        }
        resourcesGained
                .computeIfAbsent(player, ignored -> new HashMap<>())
                .merge(type, amount, Integer::sum);
    }

    public void recordDevCardDrawn(Player player) {
        devCardsDrawn.merge(player, 1, Integer::sum);
    }

    public void recordBankTrade(Player player) {
        bankTrades.merge(player, 1, Integer::sum);
    }

    public void recordPlayerTrade(Player player) {
        playerTrades.merge(player, 1, Integer::sum);
    }

    public int getTotalTurns() {
        return totalTurns;
    }

    public Map<Integer, Integer> getDiceRollCounts() {
        return Collections.unmodifiableMap(diceRollCounts);
    }

    public Map<ResourceType, Integer> getResourcesGainedBy(Player player) {
        return resourcesGained.getOrDefault(player, Collections.emptyMap());
    }

    public int getTotalResourcesGainedBy(Player player) {
        return getResourcesGainedBy(player).values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getDevCardsDrawnBy(Player player) {
        return devCardsDrawn.getOrDefault(player, 0);
    }

    public int getBankTradesBy(Player player) {
        return bankTrades.getOrDefault(player, 0);
    }

    public int getPlayerTradesBy(Player player) {
        return playerTrades.getOrDefault(player, 0);
    }
}

