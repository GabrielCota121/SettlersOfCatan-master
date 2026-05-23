package com.catan.model.building;

import com.catan.model.board.Vertex;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettlementTest {

    private Settlement settlement;

    @BeforeEach
    void setUp() {
        Player player = new Player(1, "Marcelle", "red");
        Vertex vertex = new Vertex(0, 0);
        settlement = new Settlement(player, vertex);
    }

    @Test
    void getVictoryPointsReturnsOne() {
        assertEquals(1, settlement.getVictoryPoints());
    }

    @Test
    void getResourceYieldReturnsOne() {
        assertEquals(1, settlement.getResourceYield());
    }
}
