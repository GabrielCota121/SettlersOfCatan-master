package com.example.network.room;

import com.example.model.board.Board;
import com.example.model.board.BoardFactory;
import com.example.model.board.Edge;
import com.example.model.board.Tile;
import com.example.model.board.Vertex;
import com.example.network.protocol.BuildingDTO;
import com.example.network.protocol.GameStateDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa o motor autoritativo do servidor: determinismo do tabuleiro (mesma seed
 * => mesmo layout) e aplicação de ações refletida no snapshot, incluindo o
 * controle de "só o jogador da vez age".
 */
class GameSessionTest {

    private static final long SEED = 987654321L;

    @Test
    void boardIsDeterministicForSameSeed() {
        assertEquals(signature(BoardFactory.createStandardBoard(SEED)),
                     signature(BoardFactory.createStandardBoard(SEED)),
                     "Tabuleiros gerados com a mesma seed devem ser idênticos");
    }

    @Test
    void setupPlacementAndTurnAdvanceAndAntiCheat() {
        // Tabuleiro de referência (mesma seed) para descobrir ids estáveis.
        Board ref = BoardFactory.createStandardBoard(SEED);
        Vertex v0 = ref.getVertices().get(0);
        Edge e0 = v0.getAdjacentEdges().get(0);

        List<RoomPlayer> players = List.of(
                new RoomPlayer("s1", "Alice", "RED"),
                new RoomPlayer("s2", "Bob", "BLUE"));
        GameSession game = new GameSession(players, SEED, msg -> { /* ignora log */ });

        // Começa no setup, vez da Alice.
        GameStateDTO snap = game.snapshot();
        assertEquals("Alice", snap.getCurrentPlayerName());
        assertTrue(snap.isSetupPhase());
        assertFalse(snap.isCanRollDice());

        // Anti-trapaça: Bob não pode agir na vez da Alice.
        assertFalse(game.applyAction("Bob", "BUILD_SETTLEMENT", v0.getId()));

        // Alice coloca o settlement inicial.
        assertTrue(game.applyAction("Alice", "BUILD_SETTLEMENT", v0.getId()));
        snap = game.snapshot();
        assertEquals(1, snap.getBuildings().size());
        BuildingDTO b = snap.getBuildings().get(0);
        assertEquals("SETTLEMENT", b.getType());
        assertEquals("Alice", b.getOwnerName());
        assertEquals(v0.getId(), b.getLocationId());

        // Alice coloca a estrada inicial -> turno avança para Bob.
        assertTrue(game.applyAction("Alice", "BUILD_ROAD", e0.getId()));
        snap = game.snapshot();
        assertEquals(1, snap.getRoads().size());
        assertEquals("Bob", snap.getCurrentPlayerName(), "Após a estrada do setup, deve ser a vez do Bob");
    }

    @Test
    void rejectsUnknownActionAndMissingTarget() {
        GameSession game = new GameSession(
                List.of(new RoomPlayer("s1", "Alice", "RED"), new RoomPlayer("s2", "Bob", "BLUE")),
                SEED, msg -> {});
        assertFalse(game.applyAction("Alice", "NOPE", null));
        assertFalse(game.applyAction("Alice", "BUILD_SETTLEMENT", "id-que-nao-existe"));
    }

    /** Assinatura canônica do layout: recurso+token por tile (ordenado por id). */
    private static String signature(Board board) {
        StringBuilder sb = new StringBuilder();
        board.getTiles().stream()
                .sorted((a, c) -> Integer.compare(a.getId(), c.getId()))
                .forEach((Tile t) -> sb.append(t.getId()).append(':')
                        .append(t.getResource()).append(':')
                        .append(t.getNumberToken()).append('|'));
        return sb.toString();
    }
}
