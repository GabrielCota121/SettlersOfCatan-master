package com.catan.model.cards;

import com.catan.model.board.BoardFactory;
import com.catan.model.game.CatanGameManager;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VictoryPointCardTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private VictoryPointCard card;
    private Player player;
    private CatanGameManager gameManager;

    @BeforeEach
    void setUp() {
        card = new VictoryPointCard();
        player = new Player(1, "Test", "red");
        gameManager = new CatanGameManager(BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
    }

    @Test
    void getNameReturnsVictoryPoint() {
        assertEquals("Victory Point", card.getName());
    }

    @Test
    void onPurchaseAddsToPlayerPlayableCards() {
        card.onPurchase(player);
        assertTrue(player.getPlayableCards().contains(card));
    }

    @Test
    void onPurchaseDoesNotAddToNewCards() {
        card.onPurchase(player);
        assertFalse(player.getNewCards().contains(card));
    }

    @Test
    void playIncrementsPlayerVictoryPoints() {
        int before = player.getVictoryPoints();
        card.play(gameManager, player);
        assertEquals(before + 1, player.getVictoryPoints());
    }

    @Test
    void playReturnsTrue() {
        assertTrue(card.play(gameManager, player));
    }
}
