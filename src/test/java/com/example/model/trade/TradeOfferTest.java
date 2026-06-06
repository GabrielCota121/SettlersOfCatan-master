package com.example.model.trade;

import com.example.model.game.ResourceType;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TradeOfferTest {

    private Player proposer;
    private Player otherPlayer;

    @BeforeEach
    void setUp() {
        proposer = new Player(1, "Marcelle", "red");
        otherPlayer = new Player(2, "Lucas", "blue");
    }

    @Test
    void addPlayerToList() {
        TradeOffer offer = new TradeOffer(proposer, Map.of(ResourceType.WOOD, 1), Map.of(ResourceType.ORE, 1));

        offer.addAcceptance(otherPlayer);

        assertTrue(offer.getAcceptedBy().contains(otherPlayer));
    }

    @Test
    void notAddDuplicatePlayerToList() {
        TradeOffer offer = new TradeOffer(proposer, Map.of(ResourceType.WOOD, 1), Map.of(ResourceType.ORE, 1));

        offer.addAcceptance(otherPlayer);
        offer.addAcceptance(otherPlayer);

        assertEquals(1, offer.getAcceptedBy().size());
    }

    @Test
    void addDifferentPlayersToList() {
        Player thirdPlayer = new Player(3, "Gabriel", "green");
        TradeOffer offer = new TradeOffer(proposer, Map.of(ResourceType.WOOD, 1), Map.of(ResourceType.ORE, 1));

        offer.addAcceptance(otherPlayer);
        offer.addAcceptance(thirdPlayer);

        assertEquals(2, offer.getAcceptedBy().size());
    }

    @Test
    void exceptionWhenOfferAndRequestSameResource() {
        Map<ResourceType, Integer> offered = Map.of(ResourceType.WOOD, 2);
        Map<ResourceType, Integer> requested = Map.of(ResourceType.WOOD, 1);

        assertThrows(IllegalArgumentException.class, () -> new TradeOffer(proposer, offered, requested));
    }

    @Test
    void canPlayerAffordReturnsTrueWhenPlayerHasResources() {
        TradeOffer offer = new TradeOffer(proposer, Map.of(ResourceType.WOOD, 2), Map.of(ResourceType.ORE, 1));

        otherPlayer.getWallet().addResource(ResourceType.ORE, 3);

        assertTrue(offer.canPlayerAfford(otherPlayer));
    }

    @Test
    void canPlayerAffordReturnsFalseWhenPlayerLacksResources() {
        TradeOffer offer = new TradeOffer(proposer, Map.of(ResourceType.WOOD, 2), Map.of(ResourceType.ORE, 3));

        otherPlayer.getWallet().addResource(ResourceType.ORE, 1);

        assertFalse(offer.canPlayerAfford(otherPlayer));
    }

}
