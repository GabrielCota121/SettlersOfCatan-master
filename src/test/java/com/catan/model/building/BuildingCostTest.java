package com.catan.model.building;

import com.catan.model.game.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildingCostTest {

    @Test
    void verifyRoadCost() {
        Map<ResourceType, Integer> cost = BuildingCost.ROAD.getCost();

        assertEquals(1, cost.get(ResourceType.WOOD));
        assertEquals(1, cost.get(ResourceType.BRICK));
        assertEquals(0, cost.get(ResourceType.WOOL));
        assertEquals(0, cost.get(ResourceType.WHEAT));
        assertEquals(0, cost.get(ResourceType.ORE));
    }

    @Test
    void verifySettlementCost() {
        Map<ResourceType, Integer> cost = BuildingCost.SETTLEMENT.getCost();

        assertEquals(1, cost.get(ResourceType.WOOD));
        assertEquals(1, cost.get(ResourceType.BRICK));
        assertEquals(1, cost.get(ResourceType.WOOL));
        assertEquals(1, cost.get(ResourceType.WHEAT));
        assertEquals(0, cost.get(ResourceType.ORE));
    }

    @Test
    void verifyCityCost() {
        Map<ResourceType, Integer> cost = BuildingCost.CITY.getCost();

        assertEquals(0, cost.get(ResourceType.WOOD));
        assertEquals(0, cost.get(ResourceType.BRICK));
        assertEquals(0, cost.get(ResourceType.WOOL));
        assertEquals(2, cost.get(ResourceType.WHEAT));
        assertEquals(3, cost.get(ResourceType.ORE));
    }

    @Test
    void verifyDevelopmentCardCost() {
        Map<ResourceType, Integer> cost = BuildingCost.DEVELOPMENT_CARD.getCost();

        assertEquals(0, cost.get(ResourceType.WOOD));
        assertEquals(0, cost.get(ResourceType.BRICK));
        assertEquals(1, cost.get(ResourceType.WOOL));
        assertEquals(1, cost.get(ResourceType.WHEAT));
        assertEquals(1, cost.get(ResourceType.ORE));
    }
}
