package com.example.model.state;

import com.example.model.board.*;
import com.example.model.building.Settlement;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveRobberStateTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private Player robberPlayer;
    private Player victim;
    private CatanGameManager gameManager;
    private MainState previousState;
    private MoveRobberState state;
    private Turn turn;

    @BeforeEach
    void setUp() {
        robberPlayer = new Player(1, "Robber", "red");
        victim = new Player(2, "Victim", "blue");
        gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(robberPlayer, victim), SILENT_LOGGER);
        previousState = new MainState();
        state = new MoveRobberState(previousState);
        turn = new Turn(robberPlayer, gameManager);
        turn.setState(state);
    }

    private Tile getDifferentNonDesertTile(Tile excluded) {
        for (Tile t : gameManager.getBoard().getTiles()) {
            if (t.getResource() != ResourceType.DESERT && !t.equals(excluded)) return t;
        }
        throw new IllegalStateException("No second non-desert tile");
    }

    @Test
    void getName_returnsExpectedString() {
        assertNotNull(state.getName());
        assertFalse(state.getName().isEmpty());
    }

    @Test
    void moveRobber_toDifferentTile_returnsNonNullList() {
        Tile currentTile = gameManager.getRobber().getCurrentTile();
        Tile newTile = getDifferentNonDesertTile(currentTile);

        List<Player> victims = state.moveRobber(newTile, turn);

        assertNotNull(victims);
    }

    @Test
    void moveRobber_toDifferentTile_movestRobber() {
        Tile currentTile = gameManager.getRobber().getCurrentTile();
        Tile newTile = getDifferentNonDesertTile(currentTile);

        state.moveRobber(newTile, turn);

        assertEquals(newTile, gameManager.getRobber().getCurrentTile());
    }

    @Test
    void moveRobber_toSameTile_returnsNull() {
        Tile currentTile = gameManager.getRobber().getCurrentTile();

        List<Player> victims = state.moveRobber(currentTile, turn);

        assertNull(victims);
    }

    @Test
    void moveRobber_toTileWithVictimBuilding_includesVictim() {
        Tile currentTile = gameManager.getRobber().getCurrentTile();
        Tile newTile = getDifferentNonDesertTile(currentTile);
        Vertex tileVertex = newTile.getVertices()[0];
        tileVertex.setBuilding(new Settlement(victim, tileVertex));
        victim.getWallet().addResource(ResourceType.WOOD, 1);

        List<Player> victims = state.moveRobber(newTile, turn);

        assertTrue(victims.contains(victim));
    }

    @Test
    void moveRobber_toTileWithNoBuildings_returnsEmptyList() {
        Tile currentTile = gameManager.getRobber().getCurrentTile();
        Tile newTile = getDifferentNonDesertTile(currentTile);

        List<Player> victims = state.moveRobber(newTile, turn);

        assertTrue(victims.isEmpty());
    }

    @Test
    void moveRobber_victimWithNoCards_notIncluded() {
        Tile currentTile = gameManager.getRobber().getCurrentTile();
        Tile newTile = getDifferentNonDesertTile(currentTile);
        Vertex tileVertex = newTile.getVertices()[0];
        tileVertex.setBuilding(new Settlement(victim, tileVertex));

        List<Player> victims = state.moveRobber(newTile, turn);

        assertFalse(victims.contains(victim));
    }

    @Test
    void executeSteal_withVictimWhoHasCards_transfersResource() {
        victim.getWallet().addResource(ResourceType.ORE, 1);
        int victimCardsBefore = victim.getWallet().getTotalCards();
        int robberCardsBefore = robberPlayer.getWallet().getTotalCards();

        state.executeSteal(victim, turn);

        assertEquals(victimCardsBefore - 1, victim.getWallet().getTotalCards());
        assertEquals(robberCardsBefore + 1, robberPlayer.getWallet().getTotalCards());
    }

    @Test
    void executeSteal_withNullVictim_doesNotCrash() {
        assertDoesNotThrow(() -> state.executeSteal(null, turn));
    }

    @Test
    void executeSteal_restoresPreviousState() {
        state.executeSteal(null, turn);

        assertInstanceOf(MainState.class, turn.getState());
    }

    @Test
    void executeSteal_withVictim_restoresPreviousState() {
        victim.getWallet().addResource(ResourceType.WOOL, 2);

        state.executeSteal(victim, turn);

        assertInstanceOf(MainState.class, turn.getState());
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
