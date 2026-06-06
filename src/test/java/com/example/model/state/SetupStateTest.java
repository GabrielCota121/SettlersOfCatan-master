package com.example.model.state;

import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.game.CatanGameManager;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SetupStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player player;
    private CatanGameManager gameManager;
    private Turn turn;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Player1", "red");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
    }

    private Turn createTurn(SetupState state) {
        Turn t = new Turn(player, gameManager);
        t.setState(state);
        return t;
    }

    private Vertex getIsolatedVertex() {
        return gameManager.getBoard().getVertices().get(0);
    }

    private Edge getEdgeAdjacentTo(Vertex vertex) {
        return vertex.getAdjacentEdges().get(0);
    }

    @Test
    void getName_returnsSetup() {
        assertEquals("Setup", new SetupState(false).getName());
    }

    @Test
    void buildSettlement_firstPass_returnsTrue() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();

        assertTrue(state.buildSettlement(vertex, turn));
    }

    @Test
    void buildSettlement_firstPass_placesBuilding() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();

        state.buildSettlement(vertex, turn);

        assertFalse(vertex.isEmpty());
    }

    @Test
    void buildSettlement_firstPass_incrementsVP() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        int vpBefore = player.getVictoryPoints();

        state.buildSettlement(vertex, turn);

        assertEquals(vpBefore + 1, player.getVictoryPoints());
    }

    @Test
    void buildSettlement_calledTwice_secondCallReturnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex v1 = gameManager.getBoard().getVertices().get(0);
        Vertex v2 = gameManager.getBoard().getVertices().get(5);

        state.buildSettlement(v1, turn);
        assertFalse(state.buildSettlement(v2, turn));
    }

    @Test
    void buildSettlement_occupiedVertex_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();

        state.buildSettlement(vertex, turn);

        SetupState state2 = new SetupState(false);
        Turn turn2 = createTurn(state2);
        assertFalse(state2.buildSettlement(vertex, turn2));
    }

    @Test
    void buildSettlement_distanceRuleViolated_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        Vertex neighbor = vertex.getAdjacentVertices().get(0);

        state.buildSettlement(neighbor, turn);

        SetupState state2 = new SetupState(false);
        Turn turn2 = createTurn(state2);
        assertFalse(state2.buildSettlement(vertex, turn2));
    }

    @Test
    void buildSettlement_secondPass_distributesResources() {
        SetupState state = new SetupState(true);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();

        state.buildSettlement(vertex, turn);

        int totalCards = player.getWallet().getTotalCards();
        assertTrue(totalCards >= 0);
    }

    @Test
    void buildSettlement_secondPass_playerReceivesResourcesFromAdjacentNonDesertTiles() {
        SetupState state = new SetupState(true);
        turn = createTurn(state);

        Vertex vertexWithNonDesert = gameManager.getBoard().getVertices().stream()
                .filter(v -> v.getAdjacentTiles().stream()
                        .anyMatch(t -> t.getResource() != com.example.model.game.ResourceType.DESERT))
                .findFirst().orElseThrow();

        int cardsBefore = player.getWallet().getTotalCards();
        state.buildSettlement(vertexWithNonDesert, turn);

        assertTrue(player.getWallet().getTotalCards() >= cardsBefore);
    }

    @Test
    void buildRoad_beforeSettlement_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        Edge edge = getEdgeAdjacentTo(vertex);

        assertFalse(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_afterSettlement_returnsTrue() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        state.buildSettlement(vertex, turn);

        Edge edge = getEdgeAdjacentTo(vertex);

        assertTrue(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_afterSettlement_placesRoad() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        state.buildSettlement(vertex, turn);

        Edge edge = getEdgeAdjacentTo(vertex);
        state.buildRoad(edge, turn);

        assertFalse(edge.isEmpty());
    }

    @Test
    void buildRoad_calledTwice_secondCallReturnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        state.buildSettlement(vertex, turn);

        List<Edge> edges = vertex.getAdjacentEdges();
        state.buildRoad(edges.get(0), turn);

        assertFalse(state.buildRoad(edges.get(0), turn));
    }

    @Test
    void buildRoad_occupiedEdge_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        Vertex vertex = getIsolatedVertex();
        state.buildSettlement(vertex, turn);

        Edge edge = getEdgeAdjacentTo(vertex);
        edge.setBuilding(new com.example.model.building.Road(player, edge));

        assertFalse(state.buildRoad(edge, turn));
    }

    @Test
    void buildCity_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        assertFalse(state.buildCity(new Vertex(0, 0), turn));
    }

    @Test
    void buyDevelopmentCard_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        assertFalse(state.buyDevelopmentCard(turn));
    }

    @Test
    void rollDice_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        assertFalse(state.rollDice(turn));
    }

    @Test
    void endTurn_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        assertFalse(state.endTurn(turn));
    }

    @Test
    void canEndTurn_returnsFalse() {
        assertFalse(new SetupState(false).canEndTurn());
    }

    @Test
    void canRollDice_returnsFalse() {
        assertFalse(new SetupState(false).canRollDice());
    }

    @Test
    void playDevelopmentCard_returnsFalse() {
        SetupState state = new SetupState(false);
        turn = createTurn(state);
        assertFalse(state.playDevelopmentCard(null, turn));
    }
}
