package com.example.network.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameStateDTOTest {

    private GameStateDTO dto;

    @BeforeEach
    void setUp() {
        dto = new GameStateDTO();
    }

    @Test
    void currentPlayerName_defaultNull() {
        assertNull(dto.getCurrentPlayerName());
    }

    @Test
    void currentPlayerName_setAndGet() {
        dto.setCurrentPlayerName("Marcelle");
        assertEquals("Marcelle", dto.getCurrentPlayerName());
    }

    @Test
    void stateName_setAndGet() {
        dto.setStateName("WaitingRoll");
        assertEquals("WaitingRoll", dto.getStateName());
    }

    @Test
    void setupPhase_defaultFalse() {
        assertFalse(dto.isSetupPhase());
    }

    @Test
    void setupPhase_setTrue() {
        dto.setSetupPhase(true);
        assertTrue(dto.isSetupPhase());
    }

    @Test
    void setupSecondPass_defaultFalse() {
        assertFalse(dto.isSetupSecondPass());
    }

    @Test
    void setupSecondPass_setTrue() {
        dto.setSetupSecondPass(true);
        assertTrue(dto.isSetupSecondPass());
    }

    @Test
    void canRollDice_setAndGet() {
        dto.setCanRollDice(true);
        assertTrue(dto.isCanRollDice());
    }

    @Test
    void canEndTurn_setAndGet() {
        dto.setCanEndTurn(true);
        assertTrue(dto.isCanEndTurn());
    }

    @Test
    void dice1_defaultZero() {
        assertEquals(0, dto.getDice1());
    }

    @Test
    void dice1_setAndGet() {
        dto.setDice1(4);
        assertEquals(4, dto.getDice1());
    }

    @Test
    void dice2_setAndGet() {
        dto.setDice2(6);
        assertEquals(6, dto.getDice2());
    }

    @Test
    void robberTileId_defaultMinus1() {
        assertEquals(-1, dto.getRobberTileId());
    }

    @Test
    void robberTileId_setAndGet() {
        dto.setRobberTileId(5);
        assertEquals(5, dto.getRobberTileId());
    }

    @Test
    void winnerName_defaultNull() {
        assertNull(dto.getWinnerName());
    }

    @Test
    void winnerName_setAndGet() {
        dto.setWinnerName("Bob");
        assertEquals("Bob", dto.getWinnerName());
    }

    @Test
    void players_defaultEmpty() {
        assertNotNull(dto.getPlayers());
        assertTrue(dto.getPlayers().isEmpty());
    }

    @Test
    void players_setAndGet() {
        List<PlayerStateDTO> list = List.of(new PlayerStateDTO());
        dto.setPlayers(list);
        assertEquals(1, dto.getPlayers().size());
    }

    @Test
    void players_setNull_returnsEmpty() {
        dto.setPlayers(null);
        assertNotNull(dto.getPlayers());
        assertTrue(dto.getPlayers().isEmpty());
    }

    @Test
    void buildings_defaultEmpty() {
        assertNotNull(dto.getBuildings());
        assertTrue(dto.getBuildings().isEmpty());
    }

    @Test
    void buildings_setNull_returnsEmpty() {
        dto.setBuildings(null);
        assertNotNull(dto.getBuildings());
    }

    @Test
    void roads_defaultEmpty() {
        assertNotNull(dto.getRoads());
        assertTrue(dto.getRoads().isEmpty());
    }

    @Test
    void roads_setNull_returnsEmpty() {
        dto.setRoads(null);
        assertNotNull(dto.getRoads());
    }

    @Test
    void buildings_setAndGet() {
        List<BuildingDTO> list = List.of(new BuildingDTO("v1", "SETTLEMENT", "RED", "Marcelle"));
        dto.setBuildings(list);
        assertEquals(1, dto.getBuildings().size());
    }

    @Test
    void bank_defaultEmpty() {
        assertNotNull(dto.getBank());
    }

    @Test
    void bank_setNull_returnsEmpty() {
        dto.setBank(null);
        assertNotNull(dto.getBank());
    }

    @Test
    void bank_setAndGet() {
        Map<String, Integer> bank = new HashMap<>();
        bank.put("WOOD", 19);
        dto.setBank(bank);
        assertEquals(19, dto.getBank().get("WOOD"));
    }

    @Test
    void discardPendingPlayers_defaultEmpty() {
        assertNotNull(dto.getDiscardPendingPlayers());
    }

    @Test
    void discardPendingPlayers_setNull_returnsEmpty() {
        dto.setDiscardPendingPlayers(null);
        assertNotNull(dto.getDiscardPendingPlayers());
    }

    @Test
    void discardPendingPlayers_setAndGet() {
        dto.setDiscardPendingPlayers(List.of("Marcelle", "Bob"));
        assertEquals(2, dto.getDiscardPendingPlayers().size());
    }

    @Test
    void discardAmounts_setNull_returnsEmpty() {
        dto.setDiscardAmounts(null);
        assertNotNull(dto.getDiscardAmounts());
    }

    @Test
    void discardAmounts_setAndGet() {
        Map<String, Integer> amounts = new HashMap<>();
        amounts.put("Marcelle", 3);
        dto.setDiscardAmounts(amounts);
        assertEquals(3, dto.getDiscardAmounts().get("Marcelle"));
    }

    @Test
    void robberVictims_defaultEmpty() {
        assertNotNull(dto.getRobberVictims());
    }

    @Test
    void robberVictims_setNull_returnsEmpty() {
        dto.setRobberVictims(null);
        assertNotNull(dto.getRobberVictims());
    }

    @Test
    void robberVictims_setAndGet() {
        dto.setRobberVictims(List.of("Bob", "Carol"));
        assertEquals(2, dto.getRobberVictims().size());
    }

    @Test
    void activeTrade_defaultNull() {
        assertNull(dto.getActiveTrade());
    }

    @Test
    void activeTrade_setAndGet() {
        TradeStatusDTO trade = new TradeStatusDTO();
        trade.setProposerName("Marcelle");
        dto.setActiveTrade(trade);
        assertEquals("Marcelle", dto.getActiveTrade().getProposerName());
    }

    @Test
    void statistics_defaultNull() {
        assertNull(dto.getStatistics());
    }

    @Test
    void statistics_setAndGet() {
        StatisticsDTO stats = new StatisticsDTO();
        stats.setTotalTurns(10);
        dto.setStatistics(stats);
        assertEquals(10, dto.getStatistics().getTotalTurns());
    }
}
