package com.example.network.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsDTOTest {

    private StatisticsDTO dto;

    @BeforeEach
    void setUp() {
        dto = new StatisticsDTO();
    }

    @Test
    void totalTurns_default_zero() {
        assertEquals(0, dto.getTotalTurns());
    }

    @Test
    void totalTurns_setAndGet() {
        dto.setTotalTurns(42);
        assertEquals(42, dto.getTotalTurns());
    }

    @Test
    void diceRollCounts_defaultEmpty() {
        assertNotNull(dto.getDiceRollCounts());
    }

    @Test
    void diceRollCounts_setNull_returnsEmpty() {
        dto.setDiceRollCounts(null);
        assertNotNull(dto.getDiceRollCounts());
    }

    @Test
    void diceRollCounts_setAndGet() {
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(7, 5);
        counts.put(6, 3);
        dto.setDiceRollCounts(counts);
        assertEquals(5, dto.getDiceRollCounts().get(7));
        assertEquals(3, dto.getDiceRollCounts().get(6));
    }

    @Test
    void resourcesGained_defaultEmpty() {
        assertNotNull(dto.getResourcesGained());
    }

    @Test
    void resourcesGained_setNull_returnsEmpty() {
        dto.setResourcesGained(null);
        assertNotNull(dto.getResourcesGained());
    }

    @Test
    void resourcesGained_setAndGet() {
        Map<String, Map<String, Integer>> resources = new HashMap<>();
        Map<String, Integer> MarcelleResources = new HashMap<>();
        MarcelleResources.put("WOOD", 5);
        resources.put("Marcelle", MarcelleResources);
        dto.setResourcesGained(resources);
        assertEquals(5, dto.getResourcesGained().get("Marcelle").get("WOOD"));
    }

    @Test
    void devCardsDrawn_defaultEmpty() {
        assertNotNull(dto.getDevCardsDrawn());
    }

    @Test
    void devCardsDrawn_setNull_returnsEmpty() {
        dto.setDevCardsDrawn(null);
        assertNotNull(dto.getDevCardsDrawn());
    }

    @Test
    void devCardsDrawn_setAndGet() {
        dto.setDevCardsDrawn(Map.of("Marcelle", 3, "Bob", 1));
        assertEquals(3, dto.getDevCardsDrawn().get("Marcelle"));
        assertEquals(1, dto.getDevCardsDrawn().get("Bob"));
    }

    @Test
    void bankTrades_defaultEmpty() {
        assertNotNull(dto.getBankTrades());
    }

    @Test
    void bankTrades_setNull_returnsEmpty() {
        dto.setBankTrades(null);
        assertNotNull(dto.getBankTrades());
    }

    @Test
    void bankTrades_setAndGet() {
        dto.setBankTrades(Map.of("Marcelle", 2));
        assertEquals(2, dto.getBankTrades().get("Marcelle"));
    }

    @Test
    void playerTrades_defaultEmpty() {
        assertNotNull(dto.getPlayerTrades());
    }

    @Test
    void playerTrades_setNull_returnsEmpty() {
        dto.setPlayerTrades(null);
        assertNotNull(dto.getPlayerTrades());
    }

    @Test
    void playerTrades_setAndGet() {
        dto.setPlayerTrades(Map.of("Marcelle", 4, "Bob", 2));
        assertEquals(4, dto.getPlayerTrades().get("Marcelle"));
        assertEquals(2, dto.getPlayerTrades().get("Bob"));
    }
}
