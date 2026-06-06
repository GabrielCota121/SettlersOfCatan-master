package com.example.network.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TradeStatusDTOTest {

    private TradeStatusDTO dto;

    @BeforeEach
    void setUp() {
        dto = new TradeStatusDTO();
    }

    @Test
    void proposerName_defaultNull() {
        assertNull(dto.getProposerName());
    }

    @Test
    void proposerName_setAndGet() {
        dto.setProposerName("Marcelle");
        assertEquals("Marcelle", dto.getProposerName());
    }

    @Test
    void give_defaultEmpty() {
        assertNotNull(dto.getGive());
    }

    @Test
    void give_setAndGet() {
        Map<String, Integer> give = new HashMap<>();
        give.put("WOOD", 2);
        dto.setGive(give);
        assertEquals(2, dto.getGive().get("WOOD"));
    }

    @Test
    void want_defaultEmpty() {
        assertNotNull(dto.getWant());
    }

    @Test
    void want_setAndGet() {
        Map<String, Integer> want = new HashMap<>();
        want.put("ORE", 1);
        dto.setWant(want);
        assertEquals(1, dto.getWant().get("ORE"));
    }

    @Test
    void responses_defaultEmpty() {
        assertNotNull(dto.getResponses());
    }

    @Test
    void responses_setAndGet() {
        Map<String, String> responses = new HashMap<>();
        responses.put("Bob", "PENDING");
        responses.put("Carol", "ACCEPTED");
        dto.setResponses(responses);
        assertEquals("PENDING", dto.getResponses().get("Bob"));
        assertEquals("ACCEPTED", dto.getResponses().get("Carol"));
    }

    @Test
    void active_defaultFalse() {
        assertFalse(dto.isActive());
    }

    @Test
    void active_setTrue() {
        dto.setActive(true);
        assertTrue(dto.isActive());
    }

    @Test
    void resolvedWithPlayer_defaultNull() {
        assertNull(dto.getResolvedWithPlayer());
    }

    @Test
    void resolvedWithPlayer_setAndGet() {
        dto.setResolvedWithPlayer("Bob");
        assertEquals("Bob", dto.getResolvedWithPlayer());
    }

    @Test
    void fullTradeDTOSetup_correctlyStoresAllFields() {
        dto.setProposerName("Marcelle");
        dto.setGive(Map.of("WOOD", 2, "BRICK", 1));
        dto.setWant(Map.of("ORE", 2));
        Map<String, String> responses = new HashMap<>();
        responses.put("Bob", "ACCEPTED");
        dto.setResponses(responses);
        dto.setActive(true);
        dto.setResolvedWithPlayer("Bob");

        assertEquals("Marcelle", dto.getProposerName());
        assertEquals(2, dto.getGive().get("WOOD"));
        assertEquals(1, dto.getGive().get("BRICK"));
        assertEquals(2, dto.getWant().get("ORE"));
        assertEquals("ACCEPTED", dto.getResponses().get("Bob"));
        assertTrue(dto.isActive());
        assertEquals("Bob", dto.getResolvedWithPlayer());
    }
}
