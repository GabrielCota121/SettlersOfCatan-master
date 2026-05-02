package com.catan.model.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevelopmentDeck {
    private final List<IDevelopmentCard> cards;

    public DevelopmentDeck() {
        this.cards = new ArrayList<>();
        initializeDeck();
        shuffle();
    }

    private void initializeDeck() {
        for (int i = 0; i < 14; i++) {
            cards.add(new KnightCard());
        }

        for (int i = 0; i < 5; i++) {
            cards.add(new VictoryPointCard());
        }

        for (int i = 0; i < 2; i++) {
            cards.add(new RoadBuildingCard());
            cards.add(new YearOfPlentyCard());
            cards.add(new MonopolyCard());
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public IDevelopmentCard drawCard() {
        if (isEmpty()) {
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int getRemainingCardsCount() {
        return cards.size();
    }
}