package com.catan.model.cards;

import com.catan.model.board.BoardFactory;
import com.catan.model.game.CatanGameManager;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import com.catan.model.state.MoveRobberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnightCardTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private KnightCard card;
    private Player player;
    private CatanGameManager gameManager;

    @BeforeEach
    void setUp() {
        card = new KnightCard();
        player = new Player(1, "Test", "red");
        gameManager = new CatanGameManager(BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
    }

    @Test
    void getNameReturnsKnight() {
        assertEquals("Knight", card.getName());
    }

    @Test
    void onPurchaseAddsToPlayerNewCards() {
        card.onPurchase(player);
        assertTrue(player.getNewCards().contains(card));
    }

    @Test
    void onPurchaseDoesNotAddToPlayableCards() {
        card.onPurchase(player);
        assertFalse(player.getPlayableCards().contains(card));
    }

    @Test
    void playIncrementsPlayerKnightCount() {
        int before = player.getNumKnights();
        card.play(gameManager, player);
        assertEquals(before + 1, player.getNumKnights());
    }

    @Test
    void playSetsStateToMoveRobberState() {
        card.play(gameManager, player);
        assertInstanceOf(MoveRobberState.class, gameManager.getCurrentTurn().getState());
    }

    @Test
    void playReturnsTrue() {
        assertTrue(card.play(gameManager, player));
    }
}
