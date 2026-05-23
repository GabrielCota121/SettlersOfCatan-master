package com.catan.model.building;

import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EdgeBuildingTest {

    private Player player;
    private Edge edge;
    private EdgeBuilding building;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Marcelle", "red");
        Vertex v1 = new Vertex(0, 0);
        Vertex v2 = new Vertex(1, 1);
        edge = new Edge(v1, v2);
        building = new Road(player, edge);
    }

    @Test
    void getOwnerReturnsPlayer() {
        assertEquals(player, building.getOwner());
    }

    @Test
    void getLocationReturnsEdge() {
        assertEquals(edge, building.getLocation());
    }
}
