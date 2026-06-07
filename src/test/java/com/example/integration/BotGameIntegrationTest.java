package com.example.integration;

import com.example.network.protocol.GameStateDTO;
import com.example.network.protocol.PlayerStateDTO;
import com.example.network.room.GameSession;
import com.example.network.room.RoomPlayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotGameIntegrationTest {

    private static final long SEED = 42L;

    private static List<RoomPlayer> fourBots() {
        return List.of(
            new RoomPlayer("b1", "Bot1", "RED",   true),
            new RoomPlayer("b2", "Bot2", "BLUE",  true),
            new RoomPlayer("b3", "Bot3", "GREEN", true),
            new RoomPlayer("b4", "Bot4", "WHITE", true)
        );
    }

    @Test
    void botsCompleteSetup() {
        GameSession game = new GameSession(fourBots(), SEED, msg -> {});

        assertTrue(game.snapshot().isSetupPhase(), "Deve começar em setup");

        // Para assim que o setup terminar para não contar estradas da fase principal
        for (int i = 0; i < 20 && game.snapshot().isSetupPhase(); i++) {
            game.runBotTurnIfNeeded();
        }

        GameStateDTO snap = game.snapshot();
        assertFalse(snap.isSetupPhase(), "Setup deve ter terminado após 8 jogadas de bot");
        assertEquals(8, snap.getBuildings().size(), "4 bots 2 settlements = 8 construções");
        assertEquals(8, snap.getRoads().size(), "4 bots 2 roads = 8 estradas");
    }

    @Test
    void consistentGameAfterBotTurns() {
        GameSession game = new GameSession(fourBots(), SEED, msg -> {});

        for (int i = 0; i < 500; i++) {
            boolean acted = game.runBotTurnIfNeeded()
                         || game.runBotDiscardsIfNeeded()
                         || game.runBotTradeResponsesIfNeeded();

            GameStateDTO snap = game.snapshot();

            assertNotNull(snap.getCurrentPlayerName(), "Iteração " + i + ": currentPlayerName não deve ser null");
            assertNotNull(snap.getStateName(), "Iteração " + i + ": stateName não deve ser null");
            assertEquals(4, snap.getPlayers().size(), "Iteração " + i + ": deve haver 4 jogadores");

            for (PlayerStateDTO p : snap.getPlayers()) {
                assertNotNull(p.getName(), "Iteração " + i + ": nome do jogador não deve ser null");
                assertTrue(p.getVictoryPoints() >= 0, "Iteração " + i + ": victoryPoints não deve ser negativo");
                assertTrue(p.getNumResources() >= 0, "Iteração " + i + ": numResources não deve ser negativo");
            }

            int iteration = i;
            snap.getBank().forEach((resource, amount) ->
                assertTrue(amount >= 0,
                    "Iteração " + iteration + ": banco não deve ter valor negativo para " + resource)
            );

            if (!acted) break;
            if (snap.getWinnerName() != null) break;
        }
    }

    @Test
    void botGameProducesWinner() {
        GameSession game = new GameSession(fourBots(), SEED, msg -> {});

        for (int i = 0; i < 3000; i++) {
            boolean acted = game.runBotTurnIfNeeded()
                    || game.runBotDiscardsIfNeeded()
                    || game.runBotTradeResponsesIfNeeded();
            if (!acted) break;
            if (game.snapshot().getWinnerName() != null) break;
        }

        assertNotNull(game.snapshot().getWinnerName(),
            "Partida com 4 bots deve terminar com um vencedor");
    }
}
