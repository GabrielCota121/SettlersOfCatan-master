package com.example.model.game;

import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsManagerTest {

    private StatisticsManager stats;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        stats = new StatisticsManager();
        player1 = new Player(1, "Marcelle", "RED");
        player2 = new Player(2, "Lucas", "BLUE");
    }

    @Test
    void initialTotalTurnsIsZero() {
        assertEquals(0, stats.getTotalTurns());
    }

    @Test
    void recordTurn_incrementsTotalTurns() {
        stats.recordTurn();
        assertEquals(1, stats.getTotalTurns());
    }

    @Test
    void recordTurn_multiple_accumulatesCorrectly() {
        stats.recordTurn();
        stats.recordTurn();
        stats.recordTurn();
        assertEquals(3, stats.getTotalTurns());
    }

    @Test
    void recordDiceRoll_singleRoll_isTracked() {
        stats.recordDiceRoll(7);
        assertEquals(1, stats.getDiceRollCounts().get(7));
    }

    @Test
    void recordDiceRoll_sameRollMultipleTimes_accumulates() {
        stats.recordDiceRoll(6);
        stats.recordDiceRoll(6);
        stats.recordDiceRoll(6);
        assertEquals(3, stats.getDiceRollCounts().get(6));
    }

    @Test
    void recordDiceRoll_differentRolls_trackedSeparately() {
        stats.recordDiceRoll(5);
        stats.recordDiceRoll(7);
        stats.recordDiceRoll(9);
        assertEquals(1, stats.getDiceRollCounts().get(5));
        assertEquals(1, stats.getDiceRollCounts().get(7));
        assertEquals(1, stats.getDiceRollCounts().get(9));
    }

    @Test
    void getDiceRollCounts_returnsUnmodifiableMap() {
        stats.recordDiceRoll(7);
        assertThrows(UnsupportedOperationException.class, () ->
            stats.getDiceRollCounts().put(6, 1));
    }

    @Test
    void recordResourceGained_positiveAmount_isTracked() {
        stats.recordResourceGained(player1, ResourceType.WOOD, 2);
        assertEquals(2, stats.getResourcesGainedBy(player1).get(ResourceType.WOOD));
    }

    @Test
    void recordResourceGained_zeroAmount_isIgnored() {
        stats.recordResourceGained(player1, ResourceType.WOOD, 0);
        assertFalse(stats.getResourcesGainedBy(player1).containsKey(ResourceType.WOOD));
    }

    @Test
    void recordResourceGained_negativeAmount_isIgnored() {
        stats.recordResourceGained(player1, ResourceType.WOOD, -1);
        assertFalse(stats.getResourcesGainedBy(player1).containsKey(ResourceType.WOOD));
    }

    @Test
    void recordResourceGained_accumulates() {
        stats.recordResourceGained(player1, ResourceType.ORE, 3);
        stats.recordResourceGained(player1, ResourceType.ORE, 2);
        assertEquals(5, stats.getResourcesGainedBy(player1).get(ResourceType.ORE));
    }

    @Test
    void getResourcesGainedBy_unknownPlayer_returnsEmptyMap() {
        assertTrue(stats.getResourcesGainedBy(player1).isEmpty());
    }

    @Test
    void getTotalResourcesGainedBy_multipleTypes_sumsAll() {
        stats.recordResourceGained(player1, ResourceType.WOOD, 2);
        stats.recordResourceGained(player1, ResourceType.ORE, 3);
        assertEquals(5, stats.getTotalResourcesGainedBy(player1));
    }

    @Test
    void getTotalResourcesGainedBy_noResources_returnsZero() {
        assertEquals(0, stats.getTotalResourcesGainedBy(player1));
    }

    @Test
    void recordDevCardDrawn_isTracked() {
        stats.recordDevCardDrawn(player1);
        assertEquals(1, stats.getDevCardsDrawnBy(player1));
    }

    @Test
    void recordDevCardDrawn_multiple_accumulates() {
        stats.recordDevCardDrawn(player1);
        stats.recordDevCardDrawn(player1);
        assertEquals(2, stats.getDevCardsDrawnBy(player1));
    }

    @Test
    void getDevCardsDrawnBy_unknownPlayer_returnsZero() {
        assertEquals(0, stats.getDevCardsDrawnBy(player1));
    }

    @Test
    void recordBankTrade_isTracked() {
        stats.recordBankTrade(player1);
        assertEquals(1, stats.getBankTradesBy(player1));
    }

    @Test
    void recordBankTrade_multiple_accumulates() {
        stats.recordBankTrade(player1);
        stats.recordBankTrade(player1);
        stats.recordBankTrade(player1);
        assertEquals(3, stats.getBankTradesBy(player1));
    }

    @Test
    void getBankTradesBy_unknownPlayer_returnsZero() {
        assertEquals(0, stats.getBankTradesBy(player1));
    }

    @Test
    void recordPlayerTrade_isTracked() {
        stats.recordPlayerTrade(player1);
        assertEquals(1, stats.getPlayerTradesBy(player1));
    }

    @Test
    void recordPlayerTrade_multiple_accumulates() {
        stats.recordPlayerTrade(player1);
        stats.recordPlayerTrade(player2);
        stats.recordPlayerTrade(player1);
        assertEquals(2, stats.getPlayerTradesBy(player1));
        assertEquals(1, stats.getPlayerTradesBy(player2));
    }

    @Test
    void getPlayerTradesBy_unknownPlayer_returnsZero() {
        assertEquals(0, stats.getPlayerTradesBy(player2));
    }

    @Test
    void stats_areIndependentPerPlayer() {
        stats.recordResourceGained(player1, ResourceType.WOOD, 5);
        stats.recordResourceGained(player2, ResourceType.WOOD, 3);
        assertEquals(5, stats.getResourcesGainedBy(player1).get(ResourceType.WOOD));
        assertEquals(3, stats.getResourcesGainedBy(player2).get(ResourceType.WOOD));
    }

    @Test
    void recordResourceGained_multipleResources_trackedSeparately() {
        stats.recordResourceGained(player1, ResourceType.WOOD, 1);
        stats.recordResourceGained(player1, ResourceType.ORE, 2);
        stats.recordResourceGained(player1, ResourceType.WHEAT, 3);
        assertEquals(6, stats.getTotalResourcesGainedBy(player1));
        assertEquals(1, stats.getResourcesGainedBy(player1).get(ResourceType.WOOD));
        assertEquals(2, stats.getResourcesGainedBy(player1).get(ResourceType.ORE));
        assertEquals(3, stats.getResourcesGainedBy(player1).get(ResourceType.WHEAT));
    }
}
