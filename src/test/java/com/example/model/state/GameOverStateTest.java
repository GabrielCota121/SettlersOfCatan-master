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

class GameOverStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player winner;
    private GameOverState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        winner = new Player(1, "Winner", "red");
        state = new GameOverState(winner);
        CatanGameManager gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(winner), SILENT_LOGGER);
        turn = new Turn(winner, gameManager);
        turn.setState(state);
    }

    @Test
    void getWinner_returnsCorrectPlayer() {
        assertSame(winner, state.getWinner());
    }

    @Test
    void getName_containsWinnerName() {
        assertTrue(state.getName().contains("Winner"));
    }

    @Test
    void getName_notNull() {
        assertNotNull(state.getName());
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
    void buildRoad_returnsFalse() {
        assertFalse(state.buildRoad(new Edge(new Vertex(0, 0), new Vertex(1, 0)), turn));
    }

    @Test
    void buyDevelopmentCard_returnsFalse() {
        assertFalse(state.buyDevelopmentCard(turn));
    }

    @Test
    void playDevelopmentCard_returnsFalse() {
        assertFalse(state.playDevelopmentCard(null, turn));
    }

    @Test
    void rollDice_returnsFalse() {
        assertFalse(state.rollDice(turn));
    }

    @Test
    void endTurn_returnsFalse() {
        assertFalse(state.endTurn(turn));
    }

    @Test
    void canEndTurn_returnsFalse() {
        assertFalse(state.canEndTurn());
    }

    @Test
    void canRollDice_returnsFalse() {
        assertFalse(state.canRollDice());
    }
}
