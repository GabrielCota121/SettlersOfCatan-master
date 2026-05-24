package com.catan.model.state;

import com.catan.model.board.BoardFactory;
import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.Turn;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ITurnStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Turn turn;
    private Vertex vertex;
    private Edge edge;

    @BeforeEach
    void setUp() {
        Player player = new Player(1, "Test", "red");
        CatanGameManager gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
        turn = new Turn(player, gameManager);
        vertex = new Vertex(0, 0);
        edge = new Edge(new Vertex(0, 0), new Vertex(1, 0));
    }

    private ITurnState blockingState() {
        return new ITurnState() {
            @Override public String getName() { return "BlockingState"; }
            @Override public boolean buildSettlement(Vertex v, Turn t) { return false; }
            @Override public boolean buildRoad(Edge e, Turn t) { return false; }
            @Override public boolean buildCity(Vertex v, Turn t) { return false; }
            @Override public boolean buyDevelopmentCard(Turn t) { return false; }
            @Override public boolean rollDice(Turn t) { return false; }
            @Override public boolean canEndTurn() { return false; }
            @Override public boolean canRollDice() { return false; }
            @Override public boolean endTurn(Turn t) { return false; }
            @Override public boolean playDevelopmentCard(IDevelopmentCard card, Turn t) { return false; }
        };
    }

    private ITurnState permissiveState() {
        return new ITurnState() {
            @Override public String getName() { return "PermissiveState"; }
            @Override public boolean buildSettlement(Vertex v, Turn t) { return true; }
            @Override public boolean buildRoad(Edge e, Turn t) { return true; }
            @Override public boolean buildCity(Vertex v, Turn t) { return true; }
            @Override public boolean buyDevelopmentCard(Turn t) { return true; }
            @Override public boolean rollDice(Turn t) { return true; }
            @Override public boolean canEndTurn() { return true; }
            @Override public boolean canRollDice() { return true; }
            @Override public boolean endTurn(Turn t) { return true; }
            @Override public boolean playDevelopmentCard(IDevelopmentCard card, Turn t) { return true; }
        };
    }

    // --- contrato: getName() ---

    @Test
    void getName_neverReturnsNull() {
        assertNotNull(blockingState().getName());
        assertNotNull(permissiveState().getName());
    }

    @Test
    void getName_neverReturnsEmpty() {
        assertFalse(blockingState().getName().isEmpty());
        assertFalse(permissiveState().getName().isEmpty());
    }

    // --- contrato: métodos de ação podem retornar false ---

    @Test
    void buildSettlement_canReturnFalse() {
        assertFalse(blockingState().buildSettlement(vertex, turn));
    }

    @Test
    void buildRoad_canReturnFalse() {
        assertFalse(blockingState().buildRoad(edge, turn));
    }

    @Test
    void buildCity_canReturnFalse() {
        assertFalse(blockingState().buildCity(vertex, turn));
    }

    @Test
    void buyDevelopmentCard_canReturnFalse() {
        assertFalse(blockingState().buyDevelopmentCard(turn));
    }

    @Test
    void rollDice_canReturnFalse() {
        assertFalse(blockingState().rollDice(turn));
    }

    @Test
    void endTurn_canReturnFalse() {
        assertFalse(blockingState().endTurn(turn));
    }

    @Test
    void playDevelopmentCard_canReturnFalse() {
        assertFalse(blockingState().playDevelopmentCard(null, turn));
    }

    @Test
    void canEndTurn_canReturnFalse() {
        assertFalse(blockingState().canEndTurn());
    }

    @Test
    void canRollDice_canReturnFalse() {
        assertFalse(blockingState().canRollDice());
    }

    // --- contrato: métodos de ação podem retornar true ---

    @Test
    void buildSettlement_canReturnTrue() {
        assertTrue(permissiveState().buildSettlement(vertex, turn));
    }

    @Test
    void buildRoad_canReturnTrue() {
        assertTrue(permissiveState().buildRoad(edge, turn));
    }

    @Test
    void buildCity_canReturnTrue() {
        assertTrue(permissiveState().buildCity(vertex, turn));
    }

    @Test
    void buyDevelopmentCard_canReturnTrue() {
        assertTrue(permissiveState().buyDevelopmentCard(turn));
    }

    @Test
    void rollDice_canReturnTrue() {
        assertTrue(permissiveState().rollDice(turn));
    }

    @Test
    void endTurn_canReturnTrue() {
        assertTrue(permissiveState().endTurn(turn));
    }

    @Test
    void playDevelopmentCard_canReturnTrue() {
        assertTrue(permissiveState().playDevelopmentCard(null, turn));
    }

    @Test
    void canEndTurn_canReturnTrue() {
        assertTrue(permissiveState().canEndTurn());
    }

    @Test
    void canRollDice_canReturnTrue() {
        assertTrue(permissiveState().canRollDice());
    }

    // --- polimorfismo: Turn aceita qualquer implementação ---

    @Test
    void turn_acceptsAnyITurnStateImplementation() {
        ITurnState blocking = blockingState();
        ITurnState permissive = permissiveState();

        turn.setState(blocking);
        assertSame(blocking, turn.getState());

        turn.setState(permissive);
        assertSame(permissive, turn.getState());
    }
}
