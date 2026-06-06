package com.example.network.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateDTOTest {

    private PlayerStateDTO dto;

    @BeforeEach
    void setUp() {
        dto = new PlayerStateDTO();
    }

    @Test
    void name_setAndGet() {
        dto.setName("Marcelle");
        assertEquals("Marcelle", dto.getName());
    }

    @Test
    void color_setAndGet() {
        dto.setColor("RED");
        assertEquals("RED", dto.getColor());
    }

    @Test
    void victoryPoints_default_zero() {
        assertEquals(0, dto.getVictoryPoints());
    }

    @Test
    void victoryPoints_setAndGet() {
        dto.setVictoryPoints(5);
        assertEquals(5, dto.getVictoryPoints());
    }

    @Test
    void numKnights_setAndGet() {
        dto.setNumKnights(3);
        assertEquals(3, dto.getNumKnights());
    }

    @Test
    void longestRoad_setAndGet() {
        dto.setLongestRoad(7);
        assertEquals(7, dto.getLongestRoad());
    }

    @Test
    void numSettlements_setAndGet() {
        dto.setNumSettlements(2);
        assertEquals(2, dto.getNumSettlements());
    }

    @Test
    void numCities_setAndGet() {
        dto.setNumCities(1);
        assertEquals(1, dto.getNumCities());
    }

    @Test
    void numRoads_setAndGet() {
        dto.setNumRoads(8);
        assertEquals(8, dto.getNumRoads());
    }

    @Test
    void hiddenResources_defaultFalse() {
        assertFalse(dto.isHiddenResources());
    }

    @Test
    void hiddenResources_setTrue() {
        dto.setHiddenResources(true);
        assertTrue(dto.isHiddenResources());
    }

    @Test
    void numResources_default_zero() {
        assertEquals(0, dto.getNumResources());
    }

    @Test
    void numResources_setAndGet() {
        dto.setNumResources(6);
        assertEquals(6, dto.getNumResources());
    }

    @Test
    void numDevCardsTotal_default_zero() {
        assertEquals(0, dto.getNumDevCardsTotal());
    }

    @Test
    void numDevCardsTotal_setAndGet() {
        dto.setNumDevCardsTotal(3);
        assertEquals(3, dto.getNumDevCardsTotal());
    }

    @Test
    void resources_defaultEmpty() {
        assertNotNull(dto.getResources());
    }

    @Test
    void resources_setNull_returnsEmpty() {
        dto.setResources(null);
        assertNotNull(dto.getResources());
    }

    @Test
    void resources_setAndGet() {
        Map<String, Integer> res = new HashMap<>();
        res.put("WOOD", 3);
        dto.setResources(res);
        assertEquals(3, dto.getResources().get("WOOD"));
    }

    @Test
    void devCards_defaultEmpty() {
        assertNotNull(dto.getDevCards());
        assertTrue(dto.getDevCards().isEmpty());
    }

    @Test
    void devCards_setNull_returnsEmpty() {
        dto.setDevCards(null);
        assertNotNull(dto.getDevCards());
    }

    @Test
    void devCards_setAndGet() {
        dto.setDevCards(List.of("Knight", "Victory Point"));
        assertEquals(2, dto.getDevCards().size());
    }

    @Test
    void playableDevCards_defaultEmpty() {
        assertNotNull(dto.getPlayableDevCards());
        assertTrue(dto.getPlayableDevCards().isEmpty());
    }

    @Test
    void playableDevCards_setNull_returnsEmpty() {
        dto.setPlayableDevCards(null);
        assertNotNull(dto.getPlayableDevCards());
    }

    @Test
    void playableDevCards_setAndGet() {
        dto.setPlayableDevCards(List.of("Knight"));
        assertEquals(1, dto.getPlayableDevCards().size());
    }

    @Test
    void tradeRates_defaultEmpty() {
        assertNotNull(dto.getTradeRates());
    }

    @Test
    void tradeRates_setNull_returnsEmpty() {
        dto.setTradeRates(null);
        assertNotNull(dto.getTradeRates());
    }

    @Test
    void tradeRates_setAndGet() {
        Map<String, Integer> rates = new HashMap<>();
        rates.put("WOOD", 3);
        rates.put("ORE", 4);
        dto.setTradeRates(rates);
        assertEquals(3, dto.getTradeRates().get("WOOD"));
        assertEquals(4, dto.getTradeRates().get("ORE"));
    }
}
