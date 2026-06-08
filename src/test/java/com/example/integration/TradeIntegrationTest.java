package com.example.integration;

import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.network.room.GameSession;
import com.example.network.room.RoomPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeIntegrationTest {

    private static final long SEED = 42L;
    private GameSession game;

    @BeforeEach
    void setUp() {
        List<RoomPlayer> players = List.of(
            new RoomPlayer("b1", "Marcelle", "RED",   true),
            new RoomPlayer("b2", "Lucas",   "BLUE",  true),
            new RoomPlayer("b3", "Gabriel", "GREEN", true),
            new RoomPlayer("b4", "Juliana",  "PURPLE", true)
        );
        game = new GameSession(players, SEED, msg -> {});

        // Completa o setup com bots; pós-setup Marcelle é a jogadora da vez
        for (int i = 0; i < 20 && game.snapshot().isSetupPhase(); i++) {
            game.runBotTurnIfNeeded();
        }
    }

    // Dá recursos diretamente ao jogador via reflection (precondição de teste)
    private void give(String playerName, ResourceType type, int amount) throws Exception {
        Field f = GameSession.class.getDeclaredField("manager");
        f.setAccessible(true);
        CatanGameManager mgr = (CatanGameManager) f.get(game);
        mgr.getPlayers().stream()
            .filter(p -> p.getName().equals(playerName))
            .findFirst().get()
            .getWallet().addResource(type, amount);
    }

    @Test
    void cannotTradeOnOtherPlayerTurn() {
        assertEquals("Marcelle", game.snapshot().getCurrentPlayerName());

        boolean result = game.applyAction("Lucas", "PROPOSE_TRADE",
            "{\"give\":{\"WOOD\":1},\"want\":{\"ORE\":1}}");

        assertFalse(result, "Jogador fora de sua vez não pode propor troca");
    }

    @Test
    void cannotTradeWithSameResource() {
        boolean result = game.applyAction("Marcelle", "PROPOSE_TRADE",
            "{\"give\":{\"WOOD\":1},\"want\":{\"WOOD\":1}}");

        assertFalse(result, "Não pode pedir o mesmo recurso que está oferecendo");
    }

    @Test
    void cannotTradeWhenNoActiveTrade() {
        assertFalse(game.applyAction("Lucas", "TRADE_RESPONSE", "true"),
            "Não pode responder sem troca ativa");
    }

    @Test
    void validateFullTradeFlow() throws Exception {
        give("Marcelle", ResourceType.WOOD, 1);
        give("Lucas",   ResourceType.ORE,  1);

        // Marcelle propõe: dá 1 WOOD, quer 1 ORE
        assertTrue(game.applyAction("Marcelle", "PROPOSE_TRADE",
            "{\"give\":{\"WOOD\":1},\"want\":{\"ORE\":1}}"));
        assertNotNull(game.snapshot().getActiveTrade(), "Troca deve estar ativa");

        // Lucas aceita
        assertTrue(game.applyAction("Lucas", "TRADE_RESPONSE", "true"));

        // Marcelle confirma a troca com Lucas
        assertTrue(game.applyAction("Marcelle", "CONFIRM_TRADE", "Lucas"));

        assertNull(game.snapshot().getActiveTrade(), "Troca deve ser encerrada após confirmação");
    }
}
