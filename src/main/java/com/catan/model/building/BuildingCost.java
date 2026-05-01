package com.catan.model.building;

import com.catan.model.game.ResourceType;

import java.util.EnumMap;
import java.util.Map;

public enum BuildingCost {

    ROAD(1, 1, 0, 0, 0),
    SETTLEMENT(1, 1, 1, 1, 0),
    CITY(0, 0, 0, 2, 3),
    DEVELOPMENT_CARD(0, 0, 1, 1, 1);

    private final Map<ResourceType, Integer> cost;

    BuildingCost(int wood, int brick, int wool, int wheat, int ore) {
        cost = new EnumMap<>(ResourceType.class);
        cost.put(ResourceType.WOOD, wood);
        cost.put(ResourceType.BRICK, brick);
        cost.put(ResourceType.WOOL, wool);
        cost.put(ResourceType.WHEAT, wheat);
        cost.put(ResourceType.ORE, ore);
    }

    public Map<ResourceType, Integer> getCost() {
        return cost;
    }
}
