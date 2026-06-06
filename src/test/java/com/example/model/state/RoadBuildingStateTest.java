package com.example.model.state;

import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.building.Road;
import com.example.model.building.Settlement;
import com.example.model.game.CatanGameManager;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoadBuildingStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player player;
    private CatanGameManager gameManager;
    private MainState previousState;
    private RoadBuildingState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Marcelle", "red");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
        previousState = new MainState();
        state = new RoadBuildingState(player, previousState);
        turn = new Turn(player, gameManager);
        turn.setState(state);
    }

    private Vertex getVertexWithSettlement() {
        Vertex v = gameManager.getBoard().getVertices().get(0);
        v.setBuilding(new Settlement(player, v));
        player.incrementSettlements();
        return v;
    }

    @Test
    void getName_returnsRoadBuildingState() {
        assertEquals("RoadBuildingState", state.getName());
    }

    @Test
    void buildRoad_firstRoad_returnsTrue() {
        Vertex v = getVertexWithSettlement();
        Edge edge = v.getAdjacentEdges().get(0);

        assertTrue(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_firstRoad_placesBuilding() {
        Vertex v = getVertexWithSettlement();
        Edge edge = v.getAdjacentEdges().get(0);

        state.buildRoad(edge, turn);

        assertFalse(edge.isEmpty());
    }

    @Test
    void buildRoad_firstRoad_stateRemainsRoadBuilding() {
        Vertex v = getVertexWithSettlement();
        Edge edge = v.getAdjacentEdges().get(0);

        state.buildRoad(edge, turn);

        assertInstanceOf(RoadBuildingState.class, turn.getState());
    }

    @Test
    void buildRoad_secondRoad_restoresPreviousState() {
        Vertex v = getVertexWithSettlement();
        List<Edge> edges = v.getAdjacentEdges();
        Edge edge1 = edges.get(0);
        Edge edge2 = edges.size() > 1 ? edges.get(1) : gameManager.getBoard().getEdges().stream()
                .filter(e -> e != edge1 && (e.getV1().equals(v) || e.getV2().equals(v)))
                .findFirst().orElseThrow();

        state.buildRoad(edge1, turn);
        state.buildRoad(edge2, turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void buildRoad_occupiedEdge_returnsFalse() {
        Vertex v = getVertexWithSettlement();
        Edge edge = v.getAdjacentEdges().get(0);
        edge.setBuilding(new Road(player, edge));

        assertFalse(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_noConnection_returnsFalse() {
        Vertex v1 = new Vertex(999, 999);
        Vertex v2 = new Vertex(998, 998);
        Edge isolatedEdge = new Edge(v1, v2);

        assertFalse(state.buildRoad(isolatedEdge, turn));
    }

    @Test
    void buildRoad_atMaxRoads_returnsFalse() {
        player.setNumRoads(15);
        Vertex v = getVertexWithSettlement();
        Edge edge = v.getAdjacentEdges().get(0);

        assertFalse(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_atMaxRoads_restoresPreviousState() {
        player.setNumRoads(15);
        Vertex v = getVertexWithSettlement();
        Edge edge = v.getAdjacentEdges().get(0);

        state.buildRoad(edge, turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void buildRoad_connectsViaAdjacentRoad_returnsTrue() {
        Vertex v = getVertexWithSettlement();
        Edge firstEdge = v.getAdjacentEdges().get(0);
        firstEdge.setBuilding(new Road(player, firstEdge));
        player.incrementRoads();

        Vertex otherVertex = firstEdge.getV1().equals(v) ? firstEdge.getV2() : firstEdge.getV1();
        Edge secondEdge = otherVertex.getAdjacentEdges().stream()
                .filter(e -> e != firstEdge && e.isEmpty())
                .findFirst().orElse(null);

        if (secondEdge != null) {
            state = new RoadBuildingState(player, previousState);
            turn.setState(state);
            assertTrue(state.buildRoad(secondEdge, turn));
        }
    }

    @Test
    void buildSettlement_returnsFalse() {
        assertFalse(state.buildSettlement(new Vertex(0, 0), turn));
    }

    @Test
    void buildCity_returnsFalse() {
        assertFalse(state.buildCity(new Vertex(0, 0), turn));
    }

    @Test
    void buyDevelopmentCard_returnsFalse() {
        assertFalse(state.buyDevelopmentCard(turn));
    }

    @Test
    void rollDice_returnsFalse() {
        assertFalse(state.rollDice(turn));
    }

    @Test
    void playDevelopmentCard_returnsFalse() {
        assertFalse(state.playDevelopmentCard(null, turn));
    }

    @Test
    void canEndTurn_returnsFalse() {
        assertFalse(state.canEndTurn());
    }

    @Test
    void canRollDice_returnsFalse() {
        assertFalse(state.canRollDice());
    }

    @Test
    void endTurn_returnsFalse() {
        assertFalse(state.endTurn(turn));
    }
}
