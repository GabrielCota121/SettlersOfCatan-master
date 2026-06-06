package com.example.model.state;

import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
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

class WaitingRollStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player player;
    private Player otherPlayer;
    private CatanGameManager gameManager;
    private WaitingRollState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Marcelle", "red");
        otherPlayer = new Player(2, "Lucas", "blue");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player, otherPlayer), SILENT_LOGGER);
        state = new WaitingRollState();
        turn = new Turn(player, gameManager);
        turn.setState(state);
    }

    @Test
    void getName_returnsNonEmpty() {
        assertFalse(state.getName().isEmpty());
    }

    @Test
    void canRollDice_returnsTrue() {
        assertTrue(state.canRollDice());
    }

    @Test
    void canEndTurn_returnsFalse() {
        assertFalse(state.canEndTurn());
    }

    @Test
    void rollDice_returnsTrue() {
        assertTrue(state.rollDice(turn));
    }

    @Test
    void rollDice_transitionsAwayFromWaitingRollState() {
        state.rollDice(turn);

        assertFalse(turn.getState() instanceof WaitingRollState);
    }

    @Test
    void rollDice_normalResult_transitionsToMainState() {
        boolean sawMainState = false;
        for (int i = 0; i < 100; i++) {
            WaitingRollState freshState = new WaitingRollState();
            Turn freshTurn = new Turn(player, gameManager);
            freshTurn.setState(freshState);
            freshState.rollDice(freshTurn);
            if (freshTurn.getState() instanceof MainState) {
                sawMainState = true;
                break;
            }
        }
        assertTrue(sawMainState, "Expected to see MainState after a non-7 roll at least once in 100 attempts");
    }

    @Test
    void rollDice_result7_noExcessCards_transitionsToMoveRobberState() {
        boolean sawMoveRobber = false;
        for (int i = 0; i < 200; i++) {
            WaitingRollState freshState = new WaitingRollState();
            Turn freshTurn = new Turn(player, gameManager);
            freshTurn.setState(freshState);
            freshState.rollDice(freshTurn);
            if (freshTurn.getState() instanceof MoveRobberState) {
                sawMoveRobber = true;
                break;
            }
        }
        assertTrue(sawMoveRobber, "Expected to see MoveRobberState after rolling 7 at least once in 200 attempts");
    }

    @Test
    void rollDice_result7_withExcessCards_transitionsToWaitingDiscardState() {
        player.getWallet().addResource(ResourceType.WOOD, 8);

        boolean sawWaitingDiscard = false;
        for (int i = 0; i < 200; i++) {
            WaitingRollState freshState = new WaitingRollState();
            Turn freshTurn = new Turn(player, gameManager);
            freshTurn.setState(freshState);
            freshState.rollDice(freshTurn);
            if (freshTurn.getState() instanceof WaitingDiscardState) {
                sawWaitingDiscard = true;
                break;
            }
        }
        assertTrue(sawWaitingDiscard, "Expected to see WaitingDiscardState after rolling 7 with excess cards");
    }

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
    void playDevelopmentCard_knightCard_returnsTrue() {
        KnightCard card = new KnightCard();
        player.addPlayableCard(card);

        assertTrue(state.playDevelopmentCard(card, turn));
    }

    @Test
    void playDevelopmentCard_secondNonVPCard_returnsFalse() {
        KnightCard card1 = new KnightCard();
        KnightCard card2 = new KnightCard();
        player.addPlayableCard(card1);
        player.addPlayableCard(card2);

        state.playDevelopmentCard(card1, turn);
        turn.markDevCardAsPlayed();

        turn.setState(state);
        assertFalse(state.playDevelopmentCard(card2, turn));
    }

    @Test
    void playDevelopmentCard_triggersWin_setsGameOverState() {
        for (int i = 0; i < 9; i++) player.incrementVictoryPoints();
        VictoryPointCard card = new VictoryPointCard();
        player.addPlayableCard(card);

        state.playDevelopmentCard(card, turn);

        assertInstanceOf(GameOverState.class, turn.getState());
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
    void endTurn_returnsFalse() {
        assertFalse(state.endTurn(turn));
    }
}
