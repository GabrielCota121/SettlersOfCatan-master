package com.example.model.building;

import com.example.model.board.Vertex;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VertexBuildingTest {

    private Player player;
    private Vertex vertex;
    private Settlement building;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Marcelle", "red");
        vertex = new Vertex(0, 0);
        building = new Settlement(player, vertex);
    }

    @Test
    void getOwnerReturnsPlayer() {
        assertEquals(player, building.getOwner());
    }

    @Test
    void getLocationReturnsVertex() {
        assertEquals(vertex, building.getLocation());
    }
}
