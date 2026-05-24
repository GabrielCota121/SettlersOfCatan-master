package com.catan.model.cards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DevelopmentDeckTest {

    private DevelopmentDeck deck;

    @BeforeEach
    void setUp() {
        deck = new DevelopmentDeck();
    }

    @Test
    void newDeckHas25Cards() {
        assertEquals(25, deck.getRemainingCardsCount());
    }

    @Test
    void isEmptyReturnsFalseForNewDeck() {
        assertFalse(deck.isEmpty());
    }

    @Test
    void drawCardDecreasesCount() {
        deck.drawCard();
        assertEquals(24, deck.getRemainingCardsCount());
    }

    @Test
    void drawAllCardsEmptiesDeck() {
        for (int i = 0; i < 25; i++) {
            deck.drawCard();
        }
        assertTrue(deck.isEmpty());
    }

    @Test
    void drawFromEmptyDeckReturnsNull() {
        for (int i = 0; i < 25; i++) {
            deck.drawCard();
        }
        assertNull(deck.drawCard());
    }

    @Test
    void shufflePreservesDeckSize() {
        deck.shuffle();
        assertEquals(25, deck.getRemainingCardsCount());
    }

    @Test
    void deckContainsCorrectCardDistribution() {
        List<IDevelopmentCard> drawn = new ArrayList<>();
        while (!deck.isEmpty()) {
            drawn.add(deck.drawCard());
        }

        long knights  = drawn.stream().filter(c -> c instanceof KnightCard).count();
        long vp       = drawn.stream().filter(c -> c instanceof VictoryPointCard).count();
        long road     = drawn.stream().filter(c -> c instanceof RoadBuildingCard).count();
        long yop      = drawn.stream().filter(c -> c instanceof YearOfPlentyCard).count();
        long monopoly = drawn.stream().filter(c -> c instanceof MonopolyCard).count();

        assertEquals(14, knights);
        assertEquals(5,  vp);
        assertEquals(2,  road);
        assertEquals(2,  yop);
        assertEquals(2,  monopoly);
    }
}
