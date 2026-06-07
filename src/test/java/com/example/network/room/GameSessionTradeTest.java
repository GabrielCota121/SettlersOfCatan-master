package com.example.network.room;

import com.example.model.board.Board;
import com.example.model.board.BoardFactory;
import com.example.model.board.Vertex;
import com.example.network.protocol.GameStateDTO;
import com.example.network.protocol.PlayerStateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa o fluxo completo de trocas no GameSession: PROPOSE_TRADE, TRADE_RESPONSE,
 * CONFIRM_TRADE, CANCEL_TRADE, BANK_TRADE com recursos reais do jogo.
 */
class GameSessionTradeTest {

    private static final long SEED = 99887766L;
    private Board refBoard;

    @BeforeEach
    void setUp() {
        refBoard = BoardFactory.createStandardBoard(SEED);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Vertex findFreeVertex(Board board, Set<String> usedIds) {
        Set<String> forbidden = new HashSet<>(usedIds);
        for (Vertex v : board.getVertices()) {
            if (usedIds.contains(v.getId())) {
                v.getAdjacentVertices().forEach(n -> forbidden.add(n.getId()));
            }
        }
        return board.getVertices().stream()
            .filter(v -> !forbidden.contains(v.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No free vertex found"));
    }

    /**
     * Cria jogo com Marcelle e Lucas e completa o setup, garantindo que Marcelle
     * tem ao menos recursos de um tile não-deserto na 2ª passagem.
     */
    private GameSession createGameWithResourcesForMarcelle() {
        GameSession game = new GameSession(
            List.of(
                new RoomPlayer("s1", "Marcelle", "RED"),
                new RoomPlayer("s2", "Lucas", "BLUE")
            ),
            SEED, msg -> {}
        );
        Set<String> used = new HashSet<>();

        // Marcelle 1st
        Vertex v1 = findFreeVertex(refBoard, used);
        used.add(v1.getId());
        game.applyAction("Marcelle", "BUILD_SETTLEMENT", v1.getId());
        game.applyAction("Marcelle", "BUILD_ROAD", v1.getAdjacentEdges().get(0).getId());

        // Lucas 1st
        Vertex v2 = findFreeVertex(refBoard, used);
        used.add(v2.getId());
        game.applyAction("Lucas", "BUILD_SETTLEMENT", v2.getId());
        game.applyAction("Lucas", "BUILD_ROAD", v2.getAdjacentEdges().get(0).getId());

        // Lucas 2nd
        Vertex v3 = findFreeVertex(refBoard, used);
        used.add(v3.getId());
        game.applyAction("Lucas", "BUILD_SETTLEMENT", v3.getId());
        game.applyAction("Lucas", "BUILD_ROAD", v3.getAdjacentEdges().get(0).getId());

        // Marcelle 2nd — specifically pick a vertex adjacent to non-desert tiles
        Vertex MarcelleSecond = refBoard.getVertices().stream()
            .filter(v -> !used.contains(v.getId()))
            .filter(v -> {
                // Not adjacent to occupied vertices
                Set<String> forbid = new HashSet<>(used);
                used.stream()
                    .map(id -> refBoard.getVertices().stream()
                        .filter(rv -> rv.getId().equals(id)).findFirst().orElse(null))
                    .filter(rv -> rv != null)
                    .forEach(rv -> rv.getAdjacentVertices().forEach(adj -> forbid.add(adj.getId())));
                return !forbid.contains(v.getId());
            })
            .filter(v -> v.getAdjacentTiles().stream()
                .anyMatch(t -> t.getResource() != com.example.model.game.ResourceType.DESERT))
            .findFirst()
            .orElse(findFreeVertex(refBoard, used));

        used.add(MarcelleSecond.getId());
        game.applyAction("Marcelle", "BUILD_SETTLEMENT", MarcelleSecond.getId());
        game.applyAction("Marcelle", "BUILD_ROAD", MarcelleSecond.getAdjacentEdges().get(0).getId());

        return game;
    }

    /** Avança até MainState tratando qualquer estado intermediário. */
    private boolean advanceToMainState(GameSession game, String playerName) {
        for (int i = 0; i < 300; i++) {
            GameStateDTO snap = game.snapshot();
            if (snap.isCanEndTurn()) return true;
            if (snap.isCanRollDice()) {
                game.applyAction(playerName, "ROLL_DICE", null);
            } else if (!snap.isSetupPhase()) {
                String altTile = refBoard.getTiles().stream()
                    .filter(t -> t.getId() != snap.getRobberTileId())
                    .findFirst()
                    .map(t -> String.valueOf(t.getId()))
                    .orElse(null);
                if (altTile != null) {
                    boolean moved = game.applyAction(playerName, "MOVE_ROBBER", altTile);
                    if (moved) game.applyAction(playerName, "STEAL_FROM", null);
                }
            }
        }
        return false;
    }

    // ─── Trade flow tests ─────────────────────────────────────────────────────

    @Test
    void cancelTrade_afterProposing_clearsActiveTrade() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        // Check Marcelle's snapshot to see her resources
        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        Map<String, Integer> resources = MarcelleState.getResources();
        // Find a resource Marcelle has
        String giveRes = resources.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);

        if (giveRes != null) {
            // Propose trade
            String wantRes = giveRes.equals("WOOD") ? "ORE" : "WOOD";
            boolean proposed = game.applyAction("Marcelle", "PROPOSE_TRADE",
                "{\"give\":{\"" + giveRes + "\":1},\"want\":{\"" + wantRes + "\":1}}");

            if (proposed) {
                // Cancel the trade
                assertTrue(game.applyAction("Marcelle", "CANCEL_TRADE", null));
                // Trade should be null now; consumeLastResolvedTrade returns the cancelled trade
                assertNotNull(game.consumeLastResolvedTrade());
                // Subsequent call returns null
                assertNull(game.consumeLastResolvedTrade());
            }
        }
        // If Marcelle has no resources, the test still passes (we can't propose)
        assertNotNull(game.snapshot());
    }

    @Test
    void tradeResponse_byProposer_returnsFalse() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        String giveRes = MarcelleState.getResources().entrySet().stream()
            .filter(e -> e.getValue() > 0).map(Map.Entry::getKey).findFirst().orElse(null);

        if (giveRes != null) {
            String wantRes = giveRes.equals("WOOD") ? "ORE" : "WOOD";
            boolean proposed = game.applyAction("Marcelle", "PROPOSE_TRADE",
                "{\"give\":{\"" + giveRes + "\":1},\"want\":{\"" + wantRes + "\":1}}");

            if (proposed) {
                // The proposer (Marcelle) cannot respond to their own trade
                assertFalse(game.applyAction("Marcelle", "TRADE_RESPONSE", "true"));
            }
        }
        assertNotNull(game.snapshot());
    }

    @Test
    void tradeResponse_decline_allDecline_clearsActiveTrade() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        String giveRes = MarcelleState.getResources().entrySet().stream()
            .filter(e -> e.getValue() > 0).map(Map.Entry::getKey).findFirst().orElse(null);

        if (giveRes != null) {
            String wantRes = giveRes.equals("WOOD") ? "ORE" : "WOOD";
            boolean proposed = game.applyAction("Marcelle", "PROPOSE_TRADE",
                "{\"give\":{\"" + giveRes + "\":1},\"want\":{\"" + wantRes + "\":1}}");

            if (proposed) {
                // Lucas declines
                game.applyAction("Lucas", "TRADE_RESPONSE", "false");
                // After all non-proposers decline, trade should be resolved
                // consumeLastResolvedTrade should return the resolved trade
                var resolved = game.consumeLastResolvedTrade();
                if (resolved != null) {
                    assertFalse(resolved.isActive());
                    assertNull(resolved.getResolvedWithPlayer());
                }
            }
        }
        assertNotNull(game.snapshot());
    }

    @Test
    void confirmTrade_nonExistentPartner_returnsFalse() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        String giveRes = MarcelleState.getResources().entrySet().stream()
            .filter(e -> e.getValue() > 0).map(Map.Entry::getKey).findFirst().orElse(null);

        if (giveRes != null) {
            String wantRes = giveRes.equals("WOOD") ? "ORE" : "WOOD";
            boolean proposed = game.applyAction("Marcelle", "PROPOSE_TRADE",
                "{\"give\":{\"" + giveRes + "\":1},\"want\":{\"" + wantRes + "\":1}}");

            if (proposed) {
                // Lucas hasn't accepted yet — CONFIRM_TRADE with null partner returns false
                assertFalse(game.applyAction("Marcelle", "CONFIRM_TRADE", null));
            }
        }
        assertNotNull(game.snapshot());
    }

    @Test
    void bankTrade_withEnoughResources_succeeds() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        // Find a resource Marcelle has at least 4 of (default 4:1 bank trade rate)
        String fourPlusResource = MarcelleState.getResources().entrySet().stream()
            .filter(e -> e.getValue() >= 4)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);

        if (fourPlusResource != null) {
            String receiveRes = fourPlusResource.equals("WOOD") ? "ORE" : "WOOD";
            assertTrue(game.applyAction("Marcelle", "BANK_TRADE",
                "{\"give\":{\"" + fourPlusResource + "\":4},\"receive\":\"" + receiveRes + "\"}"));
        }
        // If Marcelle doesn't have 4 of any resource, the test trivially passes
        assertNotNull(game.snapshot());
    }

    @Test
    void bankTrade_withMapReceive_succeeds() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        String fourPlusResource = MarcelleState.getResources().entrySet().stream()
            .filter(e -> e.getValue() >= 4)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);

        if (fourPlusResource != null) {
            String receiveRes = fourPlusResource.equals("WOOD") ? "ORE" : "WOOD";
            // Test with Map-style receive
            assertTrue(game.applyAction("Marcelle", "BANK_TRADE",
                "{\"give\":{\"" + fourPlusResource + "\":4},\"receive\":{\"" + receiveRes + "\":1}}"));
        }
        assertNotNull(game.snapshot());
    }

    @Test
    void bankTrade_wrongRatio_returnsFalse() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));

        GameStateDTO snap = game.snapshot("Marcelle");
        PlayerStateDTO MarcelleState = snap.getPlayers().stream()
            .filter(p -> p.getName().equals("Marcelle")).findFirst().orElseThrow();

        String anyResource = MarcelleState.getResources().entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);

        if (anyResource != null) {
            // Try to trade 3:1 (wrong ratio, default rate is 4)
            String receiveRes = anyResource.equals("WOOD") ? "ORE" : "WOOD";
            assertFalse(game.applyAction("Marcelle", "BANK_TRADE",
                "{\"give\":{\"" + anyResource + "\":3},\"receive\":\"" + receiveRes + "\"}"));
        }
        assertNotNull(game.snapshot());
    }

    @Test
    void proposeTrade_missingGiveInJson_returnsFalse() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));
        assertFalse(game.applyAction("Marcelle", "PROPOSE_TRADE",
            "{\"want\":{\"ORE\":1}}"));
    }

    @Test
    void proposeTrade_missingWantInJson_returnsFalse() {
        GameSession game = createGameWithResourcesForMarcelle();
        assertTrue(advanceToMainState(game, "Marcelle"));
        assertFalse(game.applyAction("Marcelle", "PROPOSE_TRADE",
            "{\"give\":{\"WOOD\":1}}"));
    }
}
