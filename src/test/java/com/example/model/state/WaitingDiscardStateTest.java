package com.example.model.state;

import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WaitingDiscardStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player player1;
    private Player player2;
    private CatanGameManager gameManager;
    private WaitingDiscardState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        player1 = new Player(1, "Marcelle", "red");
        player2 = new Player(2, "Lucas", "blue");
        player1.getWallet().addResource(ResourceType.WOOD, 8);
        player2.getWallet().addResource(ResourceType.BRICK, 8);

        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player1, player2), SILENT_LOGGER);

        state = new WaitingDiscardState(List.of(player1, player2));
        turn = new Turn(player1, gameManager);
        turn.setState(state);
    }

    private Map<ResourceType, Integer> discardMap(ResourceType type, int amount) {
        Map<ResourceType, Integer> map = new EnumMap<>(ResourceType.class);
        map.put(type, amount);
        return map;
    }

    @Test
    void getName_returnsNonEmpty() {
        assertFalse(state.getName().isEmpty());
    }

    @Test
    void getPendingPlayers_initiallyContainsBothPlayers() {
        assertTrue(state.getPendingPlayers().contains(player1));
        assertTrue(state.getPendingPlayers().contains(player2));
    }

    @Test
    void submitDiscard_validAmount_returnsTrue() {
        Map<ResourceType, Integer> discard = discardMap(ResourceType.WOOD, 4);
        assertTrue(state.submitDiscard(player1, discard, turn));
    }

    @Test
    void submitDiscard_validAmount_removesPlayerFromPending() {
        Map<ResourceType, Integer> discard = discardMap(ResourceType.WOOD, 4);
        state.submitDiscard(player1, discard, turn);

        assertFalse(state.getPendingPlayers().contains(player1));
    }

    @Test
    void submitDiscard_wrongCount_returnsFalse() {
        Map<ResourceType, Integer> discard = discardMap(ResourceType.WOOD, 3);
        assertFalse(state.submitDiscard(player1, discard, turn));
    }

    @Test
    void submitDiscard_wrongCount_playerRemainsInPending() {
        Map<ResourceType, Integer> discard = discardMap(ResourceType.WOOD, 3);
        state.submitDiscard(player1, discard, turn);

        assertTrue(state.getPendingPlayers().contains(player1));
    }

    @Test
    void submitDiscard_playerNotInList_returnsFalse() {
        Player uninvited = new Player(3, "Uninvited", "white");
        Map<ResourceType, Integer> discard = discardMap(ResourceType.ORE, 0);
        assertFalse(state.submitDiscard(uninvited, discard, turn));
    }

    @Test
    void submitDiscard_validAmount_removesResourcesFromPlayer() {
        int woodBefore = player1.getWallet().getResourceAmount(ResourceType.WOOD);
        Map<ResourceType, Integer> discard = discardMap(ResourceType.WOOD, 4);
        state.submitDiscard(player1, discard, turn);
        state.submitDiscard(player2, discardMap(ResourceType.BRICK, 4), turn);

        assertEquals(woodBefore - 4, player1.getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void submitDiscard_allPlayersDiscard_stateChangeToMoveRobber() {
        state.submitDiscard(player1, discardMap(ResourceType.WOOD, 4), turn);
        state.submitDiscard(player2, discardMap(ResourceType.BRICK, 4), turn);

        assertInstanceOf(MoveRobberState.class, turn.getState());
    }

    @Test
    void submitDiscard_allPlayersDiscard_bankReceivesResources() {
        int bankWoodBefore = gameManager.getBank().getWallet().getResourceAmount(ResourceType.WOOD);

        state.submitDiscard(player1, discardMap(ResourceType.WOOD, 4), turn);
        state.submitDiscard(player2, discardMap(ResourceType.BRICK, 4), turn);

        assertEquals(bankWoodBefore + 4, gameManager.getBank().getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void submitDiscard_partialCompletion_stateRemainsWaitingDiscard() {
        state.submitDiscard(player1, discardMap(ResourceType.WOOD, 4), turn);

        assertInstanceOf(WaitingDiscardState.class, turn.getState());
    }

    @Test
    void buildSettlement_returnsFalse() {
        assertFalse(state.buildSettlement(new Vertex(0, 0), turn));
    }

    @Test
    void buildRoad_returnsFalse() {
        assertFalse(state.buildRoad(new Edge(new Vertex(0, 0), new Vertex(1, 0)), turn));
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
