package com.example.network.room;

import com.example.network.protocol.GameStateDTO;
import com.example.network.protocol.PlayerStateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa o comportamento personalizado do snapshot: o viewer recebe seus próprios
 * recursos em claro, enquanto os dos demais jogadores ficam ocultos.
 */
class GameSessionSnapshotTest {

    private static final long SEED = 112233445566L;
    private GameSession game;

    @BeforeEach
    void setUp() {
        List<RoomPlayer> players = List.of(
                new RoomPlayer("s1", "Marcelle", "RED"),
                new RoomPlayer("s2", "Lucas", "BLUE"),
                new RoomPlayer("s3", "Gabriel", "GREEN"));
        game = new GameSession(players, SEED, msg -> {});
    }

    // ─── visibilidade de recursos ─────────────────────────────────────────

    @Test
    void publicSnapshotExposesAllPlayers() {
        GameStateDTO snap = game.snapshot();
        for (PlayerStateDTO p : snap.getPlayers()) {
            assertFalse(p.isHiddenResources(),
                    p.getName() + " deve estar visível no snapshot público");
            assertNotNull(p.getResources(),
                    p.getName() + " deve ter mapa de recursos no snapshot público");
        }
    }

    @Test
    void personalizedSnapshotHidesOtherPlayers() {
        GameStateDTO snap = game.snapshot("Marcelle");

        PlayerStateDTO Marcelle = findPlayer(snap, "Marcelle");
        PlayerStateDTO Lucas   = findPlayer(snap, "Lucas");
        PlayerStateDTO Gabriel = findPlayer(snap, "Gabriel");

        assertFalse(Marcelle.isHiddenResources(), "Marcelle deve ver seus próprios recursos");
        assertNotNull(Marcelle.getResources(),    "Marcelle deve ter mapa de recursos");

        assertTrue(Lucas.isHiddenResources(),  "Lucas deve estar oculto para Marcelle");
        assertTrue(Gabriel.isHiddenResources(),"Gabriel deve estar oculto para Marcelle");
    }

    @Test
    void everyPlayerSeesOnlyThemselves() {
        for (String viewer : List.of("Marcelle", "Lucas", "Gabriel")) {
            GameStateDTO snap = game.snapshot(viewer);
            for (PlayerStateDTO p : snap.getPlayers()) {
                boolean shouldBeVisible = p.getName().equals(viewer);
                assertEquals(shouldBeVisible, !p.isHiddenResources(),
                        viewer + " viewpoint: visibilidade incorreta para " + p.getName());
            }
        }
    }

    // ─── campos do snapshot ───────────────────────────────────────────────

    @Test
    void snapshotContainsAllPlayersInOrder() {
        GameStateDTO snap = game.snapshot();
        List<String> names = snap.getPlayers().stream()
                .map(PlayerStateDTO::getName)
                .toList();
        assertEquals(List.of("Marcelle", "Lucas", "Gabriel"), names);
    }

    @Test
    void snapshotStartsInSetupPhase() {
        GameStateDTO snap = game.snapshot();
        assertTrue(snap.isSetupPhase());
        assertFalse(snap.isCanRollDice());
        assertFalse(snap.isCanEndTurn());
        assertEquals("Marcelle", snap.getCurrentPlayerName());
    }

    @Test
    void bankResourcesArePresentInSnapshot() {
        GameStateDTO snap = game.snapshot();
        Map<String, Integer> bank = snap.getBank();
        assertNotNull(bank);
        assertFalse(bank.isEmpty(), "Banco deve conter recursos");
        // Catan começa com 19 de cada recurso (5 tipos)
        bank.forEach((res, qty) ->
                assertTrue(qty > 0, "Banco deve ter " + res + " disponível no início"));
    }

    @Test
    void noBuildingsOrRoadsAtGameStart() {
        GameStateDTO snap = game.snapshot();
        assertTrue(snap.getBuildings().isEmpty(), "Não deve haver construções no início");
        assertTrue(snap.getRoads().isEmpty(), "Não deve haver estradas no início");
    }

    @Test
    void winnerIsNullAtGameStart() {
        assertNull(game.snapshot().getWinnerName());
    }

    @Test
    void noDiscardPhaseAtStart() {
        GameStateDTO snap = game.snapshot();
        assertTrue(snap.getDiscardPendingPlayers() == null
                || snap.getDiscardPendingPlayers().isEmpty());
    }

    // ─── helper ───────────────────────────────────────────────────────────

    private PlayerStateDTO findPlayer(GameStateDTO snap, String name) {
        return snap.getPlayers().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Jogador não encontrado: " + name));
    }
}
