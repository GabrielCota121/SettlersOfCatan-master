package com.example.model.player;

import com.example.model.building.BuildingCost;
import com.example.model.cards.KnightCard;
import com.example.model.cards.VictoryPointCard;
import com.example.model.game.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(1, "Marcelle", "RED");
    }

    @Test
    void constructor_setsId() {
        assertEquals(1, player.getId());
    }

    @Test
    void constructor_setsName() {
        assertEquals("Marcelle", player.getName());
    }

    @Test
    void constructor_setsColor() {
        assertEquals("RED", player.getColor());
    }

    @Test
    void initialVictoryPoints_zero() {
        assertEquals(0, player.getVictoryPoints());
    }

    @Test
    void initialSettlements_zero() {
        assertEquals(0, player.getNumSettlements());
    }

    @Test
    void initialCities_zero() {
        assertEquals(0, player.getNumCities());
    }

    @Test
    void initialRoads_zero() {
        assertEquals(0, player.getNumRoads());
    }

    @Test
    void initialKnights_zero() {
        assertEquals(0, player.getNumKnights());
    }

    @Test
    void initialLongestRoad_zero() {
        assertEquals(0, player.getLongestRoad());
    }

    @Test
    void initialCards_empty() {
        assertTrue(player.getPlayableCards().isEmpty());
        assertTrue(player.getNewCards().isEmpty());
    }

    @Test
    void initialBot_false() {
        assertFalse(player.isBot());
    }

    @Test
    void initialDevCardCount_zero() {
        assertEquals(0, player.getDevCardCount());
    }

    @Test
    void initialTradeRate_fourForAllResources() {
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                assertEquals(4, player.getTradeRate(type));
            }
        }
    }

    @Test
    void setBot_true() {
        player.setBot(true);
        assertTrue(player.isBot());
    }

    @Test
    void setBot_false() {
        player.setBot(true);
        player.setBot(false);
        assertFalse(player.isBot());
    }

    @Test
    void incrementVictoryPoints_increases() {
        player.incrementVictoryPoints();
        assertEquals(1, player.getVictoryPoints());
    }

    @Test
    void incrementVictoryPoints_twice() {
        player.incrementVictoryPoints();
        player.incrementVictoryPoints();
        assertEquals(2, player.getVictoryPoints());
    }

    @Test
    void decrementVictoryPoints_decreases() {
        player.incrementVictoryPoints();
        player.incrementVictoryPoints();
        player.decrementVictoryPoints();
        assertEquals(1, player.getVictoryPoints());
    }

    @Test
    void setVictoryPoints_setsArbitraryValue() {
        player.setVictoryPoints(7);
        assertEquals(7, player.getVictoryPoints());
    }

    @Test
    void incrementSettlements() {
        player.incrementSettlements();
        assertEquals(1, player.getNumSettlements());
    }

    @Test
    void decrementSettlements() {
        player.incrementSettlements();
        player.decrementSettlements();
        assertEquals(0, player.getNumSettlements());
    }

    @Test
    void setNumSettlements() {
        player.setNumSettlements(3);
        assertEquals(3, player.getNumSettlements());
    }

    @Test
    void incrementCities() {
        player.incrementCities();
        assertEquals(1, player.getNumCities());
    }

    @Test
    void setNumCities() {
        player.setNumCities(4);
        assertEquals(4, player.getNumCities());
    }

    @Test
    void incrementRoads() {
        player.incrementRoads();
        assertEquals(1, player.getNumRoads());
    }

    @Test
    void setNumRoads() {
        player.setNumRoads(10);
        assertEquals(10, player.getNumRoads());
    }

    @Test
    void incrementKnightsPlayed() {
        player.incrementKnightsPlayed();
        assertEquals(1, player.getNumKnights());
    }

    @Test
    void setNumKnights() {
        player.setNumKnights(5);
        assertEquals(5, player.getNumKnights());
    }

    @Test
    void setAndGetLongestRoad() {
        player.setLongestRoad(8);
        assertEquals(8, player.getLongestRoad());
    }

    @Test
    void setTradeRate_lowersRate() {
        player.setTradeRate(ResourceType.WOOD, 3);
        assertEquals(3, player.getTradeRate(ResourceType.WOOD));
    }

    @Test
    void setTradeRate_doesNotRaiseRate() {
        player.setTradeRate(ResourceType.WOOD, 3);
        player.setTradeRate(ResourceType.WOOD, 4);
        assertEquals(3, player.getTradeRate(ResourceType.WOOD));
    }

    @Test
    void setTradeRate_to2_works() {
        player.setTradeRate(ResourceType.ORE, 2);
        assertEquals(2, player.getTradeRate(ResourceType.ORE));
    }

    @Test
    void setTradeRate_otherResourceUnaffected() {
        player.setTradeRate(ResourceType.WOOD, 2);
        assertEquals(4, player.getTradeRate(ResourceType.BRICK)); // unchanged
    }

    @Test
    void getTradeRates_returnsFullMap() {
        assertFalse(player.getTradeRates().isEmpty());
    }

    @Test
    void addPlayableCard_addsToList() {
        KnightCard card = new KnightCard();
        player.addPlayableCard(card);
        assertEquals(1, player.getPlayableCards().size());
        assertTrue(player.getPlayableCards().contains(card));
    }

    @Test
    void addNewCard_addsToNewList() {
        VictoryPointCard card = new VictoryPointCard();
        player.addNewCard(card);
        assertEquals(1, player.getNewCards().size());
        assertTrue(player.getNewCards().contains(card));
    }

    @Test
    void makeNewCardsPlayable_movesCards() {
        VictoryPointCard card = new VictoryPointCard();
        player.addNewCard(card);
        player.makeNewCardsPlayable();
        assertTrue(player.getNewCards().isEmpty());
        assertTrue(player.getPlayableCards().contains(card));
    }

    @Test
    void makeNewCardsPlayable_emptyList_noEffect() {
        player.makeNewCardsPlayable();
        assertTrue(player.getNewCards().isEmpty());
        assertTrue(player.getPlayableCards().isEmpty());
    }

    @Test
    void removeCard_removesFromPlayable() {
        KnightCard card = new KnightCard();
        player.addPlayableCard(card);
        player.removeCard(card);
        assertTrue(player.getPlayableCards().isEmpty());
    }

    @Test
    void getDevCardCount_playableAndNew_sumsTotal() {
        player.addPlayableCard(new KnightCard());
        player.addNewCard(new VictoryPointCard());
        assertEquals(2, player.getDevCardCount());
    }

    @Test
    void getDevCardCount_onlyPlayable() {
        player.addPlayableCard(new KnightCard());
        assertEquals(1, player.getDevCardCount());
    }

    @Test
    void getDevCardCount_onlyNew() {
        player.addNewCard(new VictoryPointCard());
        assertEquals(1, player.getDevCardCount());
    }

    @Test
    void setHiddenDevCardCount_overridesNormalCount() {
        player.addPlayableCard(new KnightCard());
        player.setHiddenDevCardCount(5);
        assertEquals(5, player.getDevCardCount());
    }

    @Test
    void setHiddenDevCardCount_negative_usesActualCount() {
        player.addPlayableCard(new KnightCard());
        player.setHiddenDevCardCount(-1);
        assertEquals(1, player.getDevCardCount());
    }

    @Test
    void canAfford_settlement_withResources_returnsTrue() {
        player.getWallet().addResource(ResourceType.WOOD, 1);
        player.getWallet().addResource(ResourceType.BRICK, 1);
        player.getWallet().addResource(ResourceType.WOOL, 1);
        player.getWallet().addResource(ResourceType.WHEAT, 1);
        assertTrue(player.canAfford(BuildingCost.SETTLEMENT));
    }

    @Test
    void canAfford_settlement_withoutResources_returnsFalse() {
        assertFalse(player.canAfford(BuildingCost.SETTLEMENT));
    }

    @Test
    void canAfford_city_withResources_returnsTrue() {
        player.getWallet().addResource(ResourceType.WHEAT, 2);
        player.getWallet().addResource(ResourceType.ORE, 3);
        assertTrue(player.canAfford(BuildingCost.CITY));
    }

    @Test
    void canAfford_road_withResources_returnsTrue() {
        player.getWallet().addResource(ResourceType.WOOD, 1);
        player.getWallet().addResource(ResourceType.BRICK, 1);
        assertTrue(player.canAfford(BuildingCost.ROAD));
    }

    @Test
    void canAfford_devCard_withResources_returnsTrue() {
        player.getWallet().addResource(ResourceType.ORE, 1);
        player.getWallet().addResource(ResourceType.WHEAT, 1);
        player.getWallet().addResource(ResourceType.WOOL, 1);
        assertTrue(player.canAfford(BuildingCost.DEVELOPMENT_CARD));
    }

    @Test
    void equals_sameId_returnsTrue() {
        Player other = new Player(1, "Different Name", "BLUE");
        assertEquals(player, other);
    }

    @Test
    void equals_differentId_returnsFalse() {
        Player other = new Player(2, "Marcelle", "RED");
        assertNotEquals(player, other);
    }

    @Test
    void equals_self_returnsTrue() {
        assertEquals(player, player);
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(null, player);
    }

    @Test
    void hashCode_sameId_sameHash() {
        Player other = new Player(1, "Other", "BLUE");
        assertEquals(player.hashCode(), other.hashCode());
    }

    @Test
    void hashCode_differentId_differentHash() {
        Player other = new Player(2, "Marcelle", "RED");
        assertNotEquals(player.hashCode(), other.hashCode());
    }
}
