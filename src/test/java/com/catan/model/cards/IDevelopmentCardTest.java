package com.catan.model.cards;

import com.catan.model.board.BoardFactory;
import com.catan.model.game.CatanGameManager;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IDevelopmentCardTest {

    private static final IGameLogger SILENT_LOGGER = new IGameLogger() {
        public void log(String message) {}
        public void error(String message) {}
    };

    static Stream<IDevelopmentCard> allCards() {
        return Stream.of(
                new KnightCard(),
                new MonopolyCard(),
                new RoadBuildingCard(),
                new VictoryPointCard(),
                new YearOfPlentyCard()
        );
    }

    @ParameterizedTest
    @MethodSource("allCards")
    void getNameIsNotBlank(IDevelopmentCard card) {
        assertNotNull(card.getName());
        assertFalse(card.getName().isBlank());
    }

    @ParameterizedTest
    @MethodSource("allCards")
    void onPurchaseAddsCardToPlayer(IDevelopmentCard card) {
        Player player = new Player(1, "Test", "red");
        card.onPurchase(player);
        boolean inEither = player.getNewCards().contains(card)
                        || player.getPlayableCards().contains(card);
        assertTrue(inEither);
    }

    @ParameterizedTest
    @MethodSource("allCards")
    void playReturnsTrue(IDevelopmentCard card) {
        Player player = new Player(1, "Test", "red");
        CatanGameManager gameManager = new CatanGameManager(
                BoardFactory.createStandardBoard(), List.of(player), SILENT_LOGGER);
        assertTrue(card.play(gameManager, player));
    }
}
