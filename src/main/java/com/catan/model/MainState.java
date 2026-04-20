package com.catan.model;

public class MainState implements ITurnState {
    @Override
    public String getName() { return "Fase Principal"; }

    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();

        if (!vertex.isEmpty() || !vertex.respectsDistanceRule() || !vertex.hasConnectingRoadFor(activePlayer) ) return false;

        if (activePlayer.getWallet().payCost(BuildingCost.SETTLEMENT.getCost())) {
            vertex.setBuilding(new Settlement(activePlayer, vertex));
            return true;
        }
        return false;
    }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();

        if (vertex.isEmpty()) return false;

        VertexBuilding currentBuilding = vertex.getBuilding();
        if (!(currentBuilding instanceof Settlement) || !currentBuilding.getOwner().equals(activePlayer)) {
            return false;
        }

        if (activePlayer.getWallet().payCost(BuildingCost.CITY.getCost())) {
            vertex.setBuilding(new City(activePlayer, vertex));
            return true;
        }
        return false;
    }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();

        if (!edge.isEmpty()) return false;

        boolean connectsToBuilding = edge.hasConnectingSettlementOrCityFor(activePlayer);
        boolean connectsToRoad = edge.hasConnectingRoadFor(activePlayer);

        if (!connectsToBuilding && !connectsToRoad) {
            return false;
        }
        if (activePlayer.getWallet().payCost(BuildingCost.ROAD.getCost())) {
            edge.setBuilding(new Road(activePlayer, edge));
            return true;
        }
        return false;
    }


    @Override
    public boolean buyDevelopmentCard(Turn currentTurn) {
        // TODO: fazer só depois de implementar as cartas
        return false;
    }

    @Override
    public boolean rollDice(Turn currentTurn) {
        return false;
    }

    @Override
    public boolean endTurn(Turn currentTurn) {
        currentTurn.getGameManager().proceedTurn();
        return true;
    }

    @Override
    public boolean canEndTurn() { return true; }

    @Override
    public boolean canRollDice() { return false; }
}
