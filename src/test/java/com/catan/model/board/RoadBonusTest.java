package com.catan.model.board;

import com.catan.model.building.Road;
import com.catan.model.building.Settlement;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoadBonusTest {

    static class FakeLogger implements IGameLogger {
        List<String> logs = new ArrayList<>();

        @Override public void log(String message) { logs.add(message); }
        @Override public void error(String message) {}
    }

    private FakeLogger logger;
    private RoadBonus roadBonus;
    private Player playerA;
    private Player playerB;

    @BeforeEach
    void setUp() {
        logger = new FakeLogger();
        roadBonus = new RoadBonus(logger);
        playerA = new Player(1, "Alice", "red");
        playerB = new Player(2, "Bob", "blue");
    }

    private List<Edge> buildLinearRoad(Player player, int length) {
        List<Edge> edges = new ArrayList<>();
        Vertex prev = new Vertex(0, player.getId() * 100.0);
        for (int i = 1; i <= length; i++) {
            Vertex curr = new Vertex(i * 10.0, player.getId() * 100.0);
            Edge edge = new Edge(prev, curr);
            edge.setBuilding(new Road(player, edge));
            edges.add(edge);
            prev = curr;
        }
        return edges;
    }

    // --- updateLongestRoad ---

    @Test
    void noBonusWhenRoadIsLessThanFive() {
        List<Edge> edges = buildLinearRoad(playerA, 4);

        roadBonus.updateLongestRoad(playerA, edges);

        assertNull(roadBonus.getCurrentHolder());
        assertEquals(4, roadBonus.getCurrentLongest());
        assertTrue(logger.logs.isEmpty());
    }

    @Test
    void bonusWhenRoadIsExactlyFive() {
        List<Edge> edges = buildLinearRoad(playerA, 5);

        roadBonus.updateLongestRoad(playerA, edges);

        assertEquals(playerA, roadBonus.getCurrentHolder());
        assertEquals(5, roadBonus.getCurrentLongest());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(1, logger.logs.size());
        assertTrue(logger.logs.get(0).contains("conquistou"));
    }

    @Test
    void holderExtendsRoadDoesNotGainMoreVictoryPoints() {
        roadBonus.updateLongestRoad(playerA, buildLinearRoad(playerA, 5));

        roadBonus.updateLongestRoad(playerA, buildLinearRoad(playerA, 6));

        assertEquals(playerA, roadBonus.getCurrentHolder());
        assertEquals(6, roadBonus.getCurrentLongest());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(1, logger.logs.size());
    }

    @Test
    void otherPlayerTakesLongestRoad() {
        roadBonus.updateLongestRoad(playerA, buildLinearRoad(playerA, 5));

        roadBonus.updateLongestRoad(playerB, buildLinearRoad(playerB, 6));

        assertEquals(playerB, roadBonus.getCurrentHolder());
        assertEquals(6, roadBonus.getCurrentLongest());
        assertEquals(0, playerA.getVictoryPoints());
        assertEquals(2, playerB.getVictoryPoints());
        assertTrue(logger.logs.get(1).contains("pegou a Longest Road"));
    }

    @Test
    void playerWithEqualLengthDoesNotTakeLongestRoad() {
        roadBonus.updateLongestRoad(playerA, buildLinearRoad(playerA, 5));

        roadBonus.updateLongestRoad(playerB, buildLinearRoad(playerB, 5));

        assertEquals(playerA, roadBonus.getCurrentHolder());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(0, playerB.getVictoryPoints());
    }

    @Test
    void playerRoadLengthIsStoredOnPlayer() {
        List<Edge> edges = buildLinearRoad(playerA, 5);

        roadBonus.updateLongestRoad(playerA, edges);

        assertEquals(5, playerA.getLongestRoad());
    }

    @Test
    void roadIsBlockedByOpponentSettlement() {
        Vertex v0 = new Vertex(0, 0);
        Vertex v1 = new Vertex(10, 0);
        Vertex v2 = new Vertex(20, 0);
        Vertex v3 = new Vertex(30, 0);
        Vertex v4 = new Vertex(40, 0);
        Vertex v5 = new Vertex(50, 0);

        Edge e1 = new Edge(v0, v1);
        Edge e2 = new Edge(v1, v2);
        Edge e3 = new Edge(v2, v3);
        Edge e4 = new Edge(v3, v4);
        Edge e5 = new Edge(v4, v5);

        e1.setBuilding(new Road(playerA, e1));
        e2.setBuilding(new Road(playerA, e2));
        e3.setBuilding(new Road(playerA, e3));
        e4.setBuilding(new Road(playerA, e4));
        e5.setBuilding(new Road(playerA, e5));

        v2.setBuilding(new Settlement(playerB, v2));

        roadBonus.updateLongestRoad(playerA, List.of(e1, e2, e3, e4, e5));

        assertNull(roadBonus.getCurrentHolder());
        assertEquals(3, playerA.getLongestRoad());
    }

    // --- reevaluateAllPlayers ---

    @Test
    void reevaluateNobodyHasFiveRoadsNoHolder() {
        List<Edge> edges = buildLinearRoad(playerA, 4);

        roadBonus.reevaluateAllPlayers(List.of(playerA), edges);

        assertNull(roadBonus.getCurrentHolder());
    }

    @Test
    void reevaluateSingleLeaderGetsBonusWhenNoPreviousHolder() {
        List<Edge> edges = buildLinearRoad(playerA, 5);

        roadBonus.reevaluateAllPlayers(List.of(playerA), edges);

        assertEquals(playerA, roadBonus.getCurrentHolder());
        assertEquals(5, roadBonus.getCurrentLongest());
        assertEquals(2, playerA.getVictoryPoints());
        assertTrue(logger.logs.get(0).contains("conquistou"));
    }

    @Test
    void reevaluateTieKeepsCurrentHolder() {
        List<Edge> playerAEdges = buildLinearRoad(playerA, 5);
        roadBonus.updateLongestRoad(playerA, playerAEdges);

        List<Edge> allEdges = new ArrayList<>(playerAEdges);
        allEdges.addAll(buildLinearRoad(playerB, 5));

        roadBonus.reevaluateAllPlayers(List.of(playerA, playerB), allEdges);

        assertEquals(playerA, roadBonus.getCurrentHolder());
        assertEquals(5, roadBonus.getCurrentLongest());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(0, playerB.getVictoryPoints());
    }

    @Test
    void reevaluateTieWithNoHolderNobodyGetsBonus() {
        List<Edge> allEdges = new ArrayList<>();
        allEdges.addAll(buildLinearRoad(playerA, 5));
        allEdges.addAll(buildLinearRoad(playerB, 5));

        roadBonus.reevaluateAllPlayers(List.of(playerA, playerB), allEdges);

        assertNull(roadBonus.getCurrentHolder());
        assertEquals(0, playerA.getVictoryPoints());
        assertEquals(0, playerB.getVictoryPoints());
    }

    @Test
    void reevaluateClearsHolderWhenNoLongerQualifies() {
        roadBonus.updateLongestRoad(playerA, buildLinearRoad(playerA, 5));

        roadBonus.reevaluateAllPlayers(List.of(playerA), buildLinearRoad(playerA, 3));

        assertNull(roadBonus.getCurrentHolder());
        assertEquals(0, playerA.getVictoryPoints());
        assertEquals(4, roadBonus.getCurrentLongest());
    }

    @Test
    void reevaluateNewLeaderTakesFromCurrentHolder() {
        List<Edge> playerAEdges = buildLinearRoad(playerA, 5);
        roadBonus.updateLongestRoad(playerA, playerAEdges);

        List<Edge> allEdges = new ArrayList<>(playerAEdges);
        allEdges.addAll(buildLinearRoad(playerB, 6));

        roadBonus.reevaluateAllPlayers(List.of(playerA, playerB), allEdges);

        assertEquals(playerB, roadBonus.getCurrentHolder());
        assertEquals(0, playerA.getVictoryPoints());
        assertEquals(2, playerB.getVictoryPoints());
        assertTrue(logger.logs.get(1).contains("quebrada"));
    }

    @Test
    void reevaluateCurrentHolderKeepsWhenStillLeader() {
        List<Edge> playerAEdges = buildLinearRoad(playerA, 5);
        roadBonus.updateLongestRoad(playerA, playerAEdges);

        roadBonus.reevaluateAllPlayers(List.of(playerA), playerAEdges);

        assertEquals(playerA, roadBonus.getCurrentHolder());
        assertEquals(2, playerA.getVictoryPoints());
        assertEquals(1, logger.logs.size());
    }
}
