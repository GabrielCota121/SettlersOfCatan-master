package com.example.model.cards;

import com.example.model.board.BoardFactory;
import com.example.model.game.CatanGameManager;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import com.example.model.state.MonopolyState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonopolyCardTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private MonopolyCard card;
    private Player player;
    private CatanGameManager gameManager;

    @BeforeEach
    void setUp() {
        card = new MonopolyCard();
        player = new Player(1, "Test", "red");
        gameManager = new CatanGameManager(BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
    }

    @Test
    void getNameReturnsMonopoly() {
        assertEquals("Monopoly", card.getName());
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
    void playSetsStateToMonopolyState() {
        card.play(gameManager, player);
        assertInstanceOf(MonopolyState.class, gameManager.getCurrentTurn().getState());
    }

    @Test
    void playReturnsTrue() {
        assertTrue(card.play(gameManager, player));
    }
}
