package com.example.model.cards;

import com.example.model.board.BoardFactory;
import com.example.model.game.CatanGameManager;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import com.example.model.state.RoadBuildingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoadBuildingCardTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    private RoadBuildingCard card;
    private Player player;
    private CatanGameManager gameManager;

    @BeforeEach
    void setUp() {
        card = new RoadBuildingCard();
        player = new Player(1, "Test", "red");
        gameManager = new CatanGameManager(BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
    }

    @Test
    void getNameReturnsRoadBuilding() {
        assertEquals("Road Building", card.getName());
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
    void playSetsStateToRoadBuildingState() {
        card.play(gameManager, player);
        assertInstanceOf(RoadBuildingState.class, gameManager.getCurrentTurn().getState());
    }

    @Test
    void playReturnsTrue() {
        assertTrue(card.play(gameManager, player));
    }
}
