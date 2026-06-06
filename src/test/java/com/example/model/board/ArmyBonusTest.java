package com.example.model.board;

import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArmyBonusTest {

    static class FakeLogger implements IGameLogger {
        List<String> logs = new ArrayList<>();

        @Override public void log(String message) { logs.add(message); }
        @Override public void error(String message) {}
    }

    private FakeLogger logger;
    private ArmyBonus armyBonus;
    private Player playerA;
    private Player playerB;

    @BeforeEach
    void setUp() {
        logger = new FakeLogger();
        armyBonus = new ArmyBonus(logger);
        playerA = new Player(1, "Marcelle", "red");
        playerB = new Player(2, "Lucas", "blue");
    }

    @Test
    void noBonusWhenLessThanTreeKnightsPlayed() {
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();

        armyBonus.updateLargestArmy(playerA);

        assertNull(armyBonus.getCurrentHolder());
        assertEquals(2, armyBonus.getCurrentLargest());
        assertTrue(logger.logs.isEmpty());
    }

    @Test
    void bonusWhenHasTreeKnightsPlayed() {
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();

        armyBonus.updateLargestArmy(playerA);

        assertEquals(playerA, armyBonus.getCurrentHolder());
        assertEquals(3, armyBonus.getCurrentLargest());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(1, logger.logs.size());
        assertTrue(logger.logs.get(0).contains("conquistou"));
    }

    @Test
    void noBonusWhenHasMoreThenTreeKnightsPlayed() {
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        armyBonus.updateLargestArmy(playerA);

        playerA.incrementKnightsPlayed();
        armyBonus.updateLargestArmy(playerA);

        assertEquals(playerA, armyBonus.getCurrentHolder());
        assertEquals(4, armyBonus.getCurrentLargest());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(1, logger.logs.size());
    }

    @Test
    void otherPlayerTakesBonus() {
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        armyBonus.updateLargestArmy(playerA);

        playerB.incrementKnightsPlayed();
        playerB.incrementKnightsPlayed();
        playerB.incrementKnightsPlayed();
        playerB.incrementKnightsPlayed();
        armyBonus.updateLargestArmy(playerB);

        assertEquals(playerB, armyBonus.getCurrentHolder());
        assertEquals(4, armyBonus.getCurrentLargest());
        assertEquals(0, playerA.getVictoryPoints());
        assertEquals(2, playerB.getVictoryPoints());
        assertTrue(logger.logs.get(1).contains("tomou"));
    }

    @Test
    void playerWithEqualKnightsDoesNotTake() {
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        playerA.incrementKnightsPlayed();
        armyBonus.updateLargestArmy(playerA);

        playerB.incrementKnightsPlayed();
        playerB.incrementKnightsPlayed();
        playerB.incrementKnightsPlayed();
        armyBonus.updateLargestArmy(playerB);

        assertEquals(playerA, armyBonus.getCurrentHolder());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(0, playerB.getVictoryPoints());
    }
}