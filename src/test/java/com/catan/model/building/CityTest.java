package com.catan.model.building;

import com.catan.model.board.Vertex;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityTest {

    private City city;

    @BeforeEach
    void setUp() {
        Player player = new Player(1, "Marcelle", "red");
        Vertex vertex = new Vertex(0, 0);
        city = new City(player, vertex);
    }

    @Test
    void getVictoryPointsReturnsTwo() {
        assertEquals(2, city.getVictoryPoints());
    }

    @Test
    void getResourceYieldReturnsTwo() {
        assertEquals(2, city.getResourceYield());
    }
}
