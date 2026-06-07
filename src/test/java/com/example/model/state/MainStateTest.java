package com.example.model.state;

import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.building.Road;
import com.example.model.building.Settlement;
import com.example.model.cards.KnightCard;
import com.example.model.cards.VictoryPointCard;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MainStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player player;
    private Player opponent;
    private CatanGameManager gameManager;
    private MainState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Marcelle", "red");
        opponent = new Player(2, "Lucas", "blue");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player, opponent), SILENT_LOGGER);
        state = new MainState();
        turn = new Turn(player, gameManager);
        turn.setState(state);
    }

    private void giveSettlementResources() {
        player.getWallet().addResource(ResourceType.WOOD, 1);
        player.getWallet().addResource(ResourceType.BRICK, 1);
        player.getWallet().addResource(ResourceType.WOOL, 1);
        player.getWallet().addResource(ResourceType.WHEAT, 1);
    }

    private void giveCityResources() {
        player.getWallet().addResource(ResourceType.WHEAT, 2);
        player.getWallet().addResource(ResourceType.ORE, 3);
    }

    private void giveRoadResources() {
        player.getWallet().addResource(ResourceType.WOOD, 1);
        player.getWallet().addResource(ResourceType.BRICK, 1);
    }

    private void giveDevCardResources() {
        player.getWallet().addResource(ResourceType.ORE, 1);
        player.getWallet().addResource(ResourceType.WHEAT, 1);
        player.getWallet().addResource(ResourceType.WOOL, 1);
    }

    private Vertex getEmptyVertexWithRoad() {
        for (Vertex v : gameManager.getBoard().getVertices()) {
            if (v.getAdjacentEdges().isEmpty()) continue;
            Edge edge = v.getAdjacentEdges().get(0);
            edge.setBuilding(new Road(player, edge));
            return v;
        }
        throw new IllegalStateException("No suitable vertex found");
    }

    // --- buildSettlement ---

    @Test
    void buildSettlement_withResourcesAndRoad_returnsTrue() {
        Vertex vertex = getEmptyVertexWithRoad();
        giveSettlementResources();
        assertTrue(state.buildSettlement(vertex, turn));
    }

    @Test
    void buildSettlement_withResourcesAndRoad_incrementsVictoryPoints() {
        Vertex vertex = getEmptyVertexWithRoad();
        giveSettlementResources();
        int vpBefore = player.getVictoryPoints();
        state.buildSettlement(vertex, turn);
        assertEquals(vpBefore + 1, player.getVictoryPoints());
    }

    @Test
    void buildSettlement_withResourcesAndRoad_placesBuilding() {
        Vertex vertex = getEmptyVertexWithRoad();
        giveSettlementResources();
        state.buildSettlement(vertex, turn);
        assertFalse(vertex.isEmpty());
    }

    @Test
    void buildSettlement_noResources_returnsFalse() {
        Vertex vertex = getEmptyVertexWithRoad();
        assertFalse(state.buildSettlement(vertex, turn));
    }

    @Test
    void buildSettlement_occupiedVertex_returnsFalse() {
        Vertex vertex = getEmptyVertexWithRoad();
        vertex.setBuilding(new Settlement(player, vertex));
        giveSettlementResources();
        assertFalse(state.buildSettlement(vertex, turn));
    }

    @Test
    void buildSettlement_distanceRuleViolated_returnsFalse() {
        Vertex vertex = getEmptyVertexWithRoad();
        Vertex neighbor = vertex.getAdjacentVertices().get(0);
        neighbor.setBuilding(new Settlement(opponent, neighbor));
        giveSettlementResources();
        assertFalse(state.buildSettlement(vertex, turn));
    }

    @Test
    void buildSettlement_noConnectingRoad_returnsFalse() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        giveSettlementResources();
        assertFalse(state.buildSettlement(vertex, turn));
    }

    @Test
    void buildSettlement_maxSettlementsReached_returnsFalse() {
        player.setNumSettlements(5);
        Vertex vertex = getEmptyVertexWithRoad();
        giveSettlementResources();
        assertFalse(state.buildSettlement(vertex, turn));
    }

    // --- buildCity ---

    @Test
    void buildCity_success_returnsTrue() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        player.incrementSettlements();
        giveCityResources();
        assertTrue(state.buildCity(vertex, turn));
    }

    @Test
    void buildCity_success_incrementsVictoryPoints() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        player.incrementSettlements();
        player.incrementVictoryPoints();
        int vpBefore = player.getVictoryPoints();
        giveCityResources();
        state.buildCity(vertex, turn);
        assertEquals(vpBefore + 1, player.getVictoryPoints());
    }

    @Test
    void buildCity_emptyVertex_returnsFalse() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        giveCityResources();
        assertFalse(state.buildCity(vertex, turn));
    }

    @Test
    void buildCity_opponentSettlement_returnsFalse() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(opponent, vertex));
        giveCityResources();
        assertFalse(state.buildCity(vertex, turn));
    }

    @Test
    void buildCity_noResources_returnsFalse() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        player.incrementSettlements();
        assertFalse(state.buildCity(vertex, turn));
    }

    @Test
    void buildCity_maxCitiesReached_returnsFalse() {
        player.setNumCities(4);
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        player.incrementSettlements();
        giveCityResources();
        assertFalse(state.buildCity(vertex, turn));
    }

    // --- buildRoad ---

    @Test
    void buildRoad_connectsToOwnSettlement_returnsTrue() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        Edge edge = vertex.getAdjacentEdges().get(0);
        giveRoadResources();
        assertTrue(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_occupiedEdge_returnsFalse() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        Edge edge = vertex.getAdjacentEdges().get(0);
        edge.setBuilding(new Road(opponent, edge));
        giveRoadResources();
        assertFalse(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_noConnection_returnsFalse() {
        Vertex v1 = new Vertex(100, 100);
        Vertex v2 = new Vertex(200, 200);
        Edge isolatedEdge = new Edge(v1, v2);
        giveRoadResources();
        assertFalse(state.buildRoad(isolatedEdge, turn));
    }

    @Test
    void buildRoad_noResources_returnsFalse() {
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        Edge edge = vertex.getAdjacentEdges().get(0);
        assertFalse(state.buildRoad(edge, turn));
    }

    @Test
    void buildRoad_maxRoadsReached_returnsFalse() {
        player.setNumRoads(15);
        Vertex vertex = gameManager.getBoard().getVertices().get(0);
        vertex.setBuilding(new Settlement(player, vertex));
        Edge edge = vertex.getAdjacentEdges().get(0);
        giveRoadResources();
        assertFalse(state.buildRoad(edge, turn));
    }

    // --- buyDevelopmentCard ---

    @Test
    void buyDevelopmentCard_withResources_returnsTrue() {
        giveDevCardResources();
        assertTrue(state.buyDevelopmentCard(turn));
    }

    @Test
    void buyDevelopmentCard_noResources_returnsFalse() {
        assertFalse(state.buyDevelopmentCard(turn));
    }

    // --- endTurn / canEndTurn / canRollDice / rollDice ---

    @Test
    void endTurn_returnsTrue() {
        assertTrue(state.endTurn(turn));
    }

    @Test
    void canEndTurn_returnsTrue() {
        assertTrue(state.canEndTurn());
    }

    @Test
    void canRollDice_returnsFalse() {
        assertFalse(state.canRollDice());
    }

    @Test
    void rollDice_returnsFalse() {
        assertFalse(state.rollDice(turn));
    }

    // --- playDevelopmentCard ---

    @Test
    void playDevelopmentCard_victoryPointCard_returnsTrue() {
        VictoryPointCard card = new VictoryPointCard();
        player.addPlayableCard(card);
        assertTrue(state.playDevelopmentCard(card, turn));
    }

    @Test
    void playDevelopmentCard_victoryPointCard_incrementsVP() {
        VictoryPointCard card = new VictoryPointCard();
        player.addPlayableCard(card);
        int vpBefore = player.getVictoryPoints();
        state.playDevelopmentCard(card, turn);
        assertEquals(vpBefore + 1, player.getVictoryPoints());
    }

    @Test
    void playDevelopmentCard_secondNonVPCard_returnsFalse() {
        KnightCard card1 = new KnightCard();
        KnightCard card2 = new KnightCard();
        player.addPlayableCard(card1);
        player.addPlayableCard(card2);
        state.playDevelopmentCard(card1, turn);
        turn.markDevCardAsPlayed();
        assertFalse(state.playDevelopmentCard(card2, turn));
    }

    // --- checkWinCondition ---

    @Test
    void buildSettlement_triggersGameOver_whenPlayerReaches10VP() {
        for (int i = 0; i < 9; i++) player.incrementVictoryPoints();
        Vertex vertex = getEmptyVertexWithRoad();
        giveSettlementResources();
        state.buildSettlement(vertex, turn);
        assertInstanceOf(GameOverState.class, turn.getState());
    }

    @Test
    void buildSettlement_noGameOver_whenPlayerBelow10VP() {
        for (int i = 0; i < 8; i++) player.incrementVictoryPoints();
        Vertex vertex = getEmptyVertexWithRoad();
        giveSettlementResources();
        state.buildSettlement(vertex, turn);
        assertInstanceOf(MainState.class, turn.getState());
    }
}
