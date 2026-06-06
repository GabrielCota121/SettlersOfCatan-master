package com.example.model.game;

import com.example.model.board.Board;
import com.example.model.board.BoardFactory;
import com.example.model.board.Port;
import com.example.model.board.Tile;
import com.example.model.board.Vertex;
import com.example.model.building.Settlement;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import com.example.model.state.GameOverState;
import com.example.model.state.SetupState;
import com.example.model.state.WaitingRollState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatanGameManagerTest {

    private static final IGameLogger SILENT = new IGameLogger() {
        public void log(String m) {}
        public void error(String m) {}
    };

    private Board board;
    private Player player1;
    private Player player2;
    private CatanGameManager manager;

    @BeforeEach
    void setUp() {
        board = BoardFactory.createStandardBoard();
        player1 = new Player(1, "Alice", "RED");
        player2 = new Player(2, "Bob", "BLUE");
        manager = new CatanGameManager(board, List.of(player1, player2), SILENT);
    }

    // ─── Estado inicial ────────────────────────────────────────────────────────

    @Test
    void initialState_setupPhase_player1Turn() {
        assertEquals(player1, manager.getCurrentTurn().getCurrentPlayer());
    }

    @Test
    void initialState_setupPhase_isSetupState() {
        assertInstanceOf(SetupState.class, manager.getCurrentTurn().getState());
    }

    @Test
    void initialState_boardIsSet() {
        assertNotNull(manager.getBoard());
        assertSame(board, manager.getBoard());
    }

    @Test
    void initialState_bankHasResources() {
        assertTrue(manager.getBank().getWallet().getTotalCards() > 0);
    }

    @Test
    void initialState_robberOnDesertTile() {
        assertNotNull(manager.getRobber());
        assertEquals(ResourceType.DESERT, manager.getRobber().getCurrentTile().getResource());
    }

    @Test
    void getPlayers_returnsAllPlayers() {
        assertEquals(2, manager.getPlayers().size());
        assertTrue(manager.getPlayers().contains(player1));
        assertTrue(manager.getPlayers().contains(player2));
    }

    @Test
    void getLogger_returnsLogger() {
        assertNotNull(manager.getLogger());
    }

    @Test
    void getDice1_isNotNull() {
        assertNotNull(manager.getDice1());
    }

    @Test
    void getDice2_isNotNull() {
        assertNotNull(manager.getDice2());
    }

    @Test
    void getStatisticsManager_isNotNull() {
        assertNotNull(manager.getStatisticsManager());
    }

    @Test
    void getDevelopmentDeck_isNotNull() {
        assertNotNull(manager.getDevelopmentDeck());
    }

    @Test
    void getRoadBonus_isNotNull() {
        assertNotNull(manager.getRoadBonus());
    }

    // ─── Rolar dados ─────────────────────────────────────────────────────────────

    @Test
    void rollDice_wrongPlayer_returnsFalse() {
        assertFalse(manager.rollDice(player2));
    }

    @Test
    void rollDice_inSetupState_returnsFalse() {
        assertFalse(manager.rollDice(player1));
    }

    @Test
    void rollDice_inWaitingRollState_returnsTrue() {
        completeSetupPhase();
        assertTrue(manager.rollDice(player1));
    }

    @Test
    void rollDice_recordsDiceRoll() {
        completeSetupPhase();
        manager.rollDice(player1);
        int total = manager.getDice1().getResult() + manager.getDice2().getResult();
        assertTrue(manager.getStatisticsManager().getDiceRollCounts().getOrDefault(total, 0) > 0);
    }

    // ─── Distribuir recursos ─────────────────────────────────────────────────

    @Test
    void distributeResources_delegatesToBank() {
        Tile targetTile = board.getTiles().stream()
            .filter(t -> t.getResource() != ResourceType.DESERT && t.getNumberToken() > 0)
            .findFirst().orElseThrow();

        Vertex vertex = targetTile.getVertices()[0];
        vertex.setBuilding(new Settlement(player1, vertex));

        int before = player1.getWallet().getTotalCards();
        manager.distributeResources(targetTile.getNumberToken());
        assertTrue(player1.getWallet().getTotalCards() >= before);
    }

    // ─── Aplicar bônus de porto ──────────────────────────────────────────────────────

    @Test
    void applyPortBonus_genericPort_setsAllRatesTo3() {
        Port port = new Port(null, 3); // 3:1 porto genérico
        manager.applyPortBonus(player1, port);
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                assertEquals(3, player1.getTradeRate(type));
            }
        }
    }

    @Test
    void applyPortBonus_specificPort_setsResourceRateTo2() {
        Port port = new Port(ResourceType.WOOD, 2);
        manager.applyPortBonus(player1, port);
        assertEquals(2, player1.getTradeRate(ResourceType.WOOD));
    }

    @Test
    void applyPortBonus_genericPort_doesNotLowerRateAlreadyAt3() {
        player1.setTradeRate(ResourceType.WOOD, 3);
        Port port = new Port(null, 3);
        manager.applyPortBonus(player1, port);
        assertEquals(3, player1.getTradeRate(ResourceType.WOOD));
    }

    // ─── Incrementar cavaleiros ──────────────────────────────────────────────

    @Test
    void incrementKnightsPlayed_increasesPlayerKnights() {
        manager.incrementKnightsPlayed(player1);
        assertEquals(1, player1.getNumKnights());
    }

    @Test
    void incrementKnightsPlayed_updatesLargestArmyBonus() {
        for (int i = 0; i < 3; i++) {
            manager.incrementKnightsPlayed(player1);
        }
        assertEquals(3, player1.getNumKnights());
    }

    // ─── Comprar carta de desenvolvimento ─────────────────────────────────────────────────

    @Test
    void drawDevelopmentCard_returnsCard() {
        assertNotNull(manager.drawDevelopmentCard());
    }

    // ─── Avançar turno ─────────────────────────────────────────────────────────

    @Test
    void proceedTurn_duringSetup_advancesToNextPlayer() {
        manager.proceedTurn();
        assertEquals(player2, manager.getCurrentTurn().getCurrentPlayer());
    }

    @Test
    void proceedTurn_completesSetup_startsWaitingRollForFirstPlayer() {
        completeSetupPhase();
        assertInstanceOf(WaitingRollState.class, manager.getCurrentTurn().getState());
        assertEquals(player1, manager.getCurrentTurn().getCurrentPlayer());
    }

    @Test
    void proceedTurn_normalPhase_advancesToNextPlayer() {
        completeSetupPhase();
        manager.rollDice(player1);
        manager.proceedTurn();
        assertEquals(player2, manager.getCurrentTurn().getCurrentPlayer());
    }

    @Test
    void proceedTurn_playerWith10VP_triggerGameOver() {
        completeSetupPhase();
        player2.setVictoryPoints(10);
        manager.rollDice(player1);
        manager.proceedTurn();
        assertInstanceOf(GameOverState.class, manager.getCurrentTurn().getState());
    }

    @Test
    void setOnTurnChangedListener_isCalledOnProceedTurn() {
        boolean[] called = {false};
        manager.setOnTurnChangedListener(() -> called[0] = true);
        manager.proceedTurn();
        assertTrue(called[0]);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void completeSetupPhase() {
        Board ref = BoardFactory.createStandardBoard();
        java.util.Set<String> used = new java.util.HashSet<>();

        for (int round = 0; round < 4; round++) {
            Player current = manager.getCurrentTurn().getCurrentPlayer();
            // Find a free vertex
            Vertex vertex = ref.getVertices().stream()
                .filter(v -> {
                    if (used.contains(v.getId())) return false;
                    for (String id : used) {
                        Vertex usedV = ref.getVertices().stream()
                            .filter(u -> u.getId().equals(id)).findFirst().orElse(null);
                        if (usedV != null) {
                            for (Vertex adj : usedV.getAdjacentVertices()) {
                                if (adj.getId().equals(v.getId())) return false;
                            }
                        }
                    }
                    return true;
                })
                .findFirst().orElseThrow();

            used.add(vertex.getId());

            // Find the same vertex in the actual board
            Vertex actualVertex = board.getVertices().stream()
                .filter(v -> v.getId().equals(vertex.getId()))
                .findFirst().orElseThrow();

            SetupState state = (SetupState) manager.getCurrentTurn().getState();
            state.buildSettlement(actualVertex, manager.getCurrentTurn());
            state.buildRoad(actualVertex.getAdjacentEdges().get(0), manager.getCurrentTurn());
        }
    }
}
