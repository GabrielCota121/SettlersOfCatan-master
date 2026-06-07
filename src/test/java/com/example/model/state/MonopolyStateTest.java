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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonopolyStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player proposer;
    private Player victim1;
    private Player victim2;
    private CatanGameManager gameManager;
    private MainState previousState;
    private MonopolyState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        proposer = new Player(1, "Proposer", "red");
        victim1 = new Player(2, "Victim1", "blue");
        victim2 = new Player(3, "Victim2", "green");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(proposer, victim1, victim2), SILENT_LOGGER);
        previousState = new MainState();
        state = new MonopolyState(proposer, previousState);
        turn = new Turn(proposer, gameManager);
        turn.setState(state);
    }

    @Test
    void getName_returnsMonopolyState() {
        assertEquals("MonopolyState", state.getName());
    }

    @Test
    void chooseResource_stealsAllFromBothVictims() {
        victim1.getWallet().addResource(ResourceType.WOOD, 3);
        victim2.getWallet().addResource(ResourceType.WOOD, 2);

        state.chooseResource(ResourceType.WOOD, turn);

        assertEquals(5, proposer.getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void chooseResource_victimsLoseAllOfChosenType() {
        victim1.getWallet().addResource(ResourceType.ORE, 4);
        victim2.getWallet().addResource(ResourceType.ORE, 1);

        state.chooseResource(ResourceType.ORE, turn);

        assertEquals(0, victim1.getWallet().getResourceAmount(ResourceType.ORE));
        assertEquals(0, victim2.getWallet().getResourceAmount(ResourceType.ORE));
    }

    @Test
    void chooseResource_nothingToSteal_proposerGainsNothing() {
        state.chooseResource(ResourceType.WHEAT, turn);

        assertEquals(0, proposer.getWallet().getResourceAmount(ResourceType.WHEAT));
    }

    @Test
    void chooseResource_doesNotStealFromProposerSelf() {
        proposer.getWallet().addResource(ResourceType.WOOL, 3);
        victim1.getWallet().addResource(ResourceType.WOOL, 2);

        state.chooseResource(ResourceType.WOOL, turn);

        assertEquals(3 + 2, proposer.getWallet().getResourceAmount(ResourceType.WOOL));
    }

    @Test
    void chooseResource_restoresPreviousState() {
        state.chooseResource(ResourceType.BRICK, turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void chooseResource_oneVictimHasCards_otherDoesNot() {
        victim1.getWallet().addResource(ResourceType.WOOD, 2);

        state.chooseResource(ResourceType.WOOD, turn);

        assertEquals(2, proposer.getWallet().getResourceAmount(ResourceType.WOOD));
        assertEquals(0, victim2.getWallet().getResourceAmount(ResourceType.WOOD));
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
