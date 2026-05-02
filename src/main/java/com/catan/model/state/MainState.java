package com.catan.model.state;

import com.catan.model.game.Bank;
import com.catan.model.game.Turn;
import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.building.*;
import com.catan.model.player.Player;

public class MainState implements ITurnState {
    @Override
    public String getName() { return "Fase Principal"; }

    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();
        Bank bank = currentTurn.getGameManager().getBank(); // Puxa o banco

        if (!vertex.isEmpty() || !vertex.respectsDistanceRule() || !vertex.hasConnectingRoadFor(activePlayer)) return false;

        if (activePlayer.getWallet().payCost(BuildingCost.SETTLEMENT.getCost())) {
            bank.receiveResources(BuildingCost.SETTLEMENT.getCost()); // Devolve pro banco!
            vertex.setBuilding(new Settlement(activePlayer, vertex));

            if (vertex.hasPort()) {
                currentTurn.getGameManager().applyPortBonus(activePlayer, vertex.getPort());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();
        Bank bank = currentTurn.getGameManager().getBank();

        if (vertex.isEmpty()) return false;

        VertexBuilding currentBuilding = vertex.getBuilding();
        if (!(currentBuilding instanceof Settlement) || !currentBuilding.getOwner().equals(activePlayer)) {
            return false;
        }

        if (activePlayer.getWallet().payCost(BuildingCost.CITY.getCost())) {
            bank.receiveResources(BuildingCost.CITY.getCost()); // Devolve pro banco!
            vertex.setBuilding(new City(activePlayer, vertex));
            return true;
        }
        return false;
    }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();
        Bank bank = currentTurn.getGameManager().getBank();

        if (!edge.isEmpty()) return false;

        boolean connectsToOwnBuilding = edge.hasConnectingSettlementOrCityFor(activePlayer);
        boolean connectsToValidRoad = canConnectViaRoad(edge, activePlayer);

        if (!connectsToOwnBuilding && !connectsToValidRoad) {
            return false;
        }

        if (activePlayer.getWallet().payCost(BuildingCost.ROAD.getCost())) {
            bank.receiveResources(BuildingCost.ROAD.getCost()); // Devolve pro banco!
            edge.setBuilding(new Road(activePlayer, edge));
            return true;
        }
        return false;
    }


    private boolean canConnectViaRoad(Edge edge, Player player) {
        for (Vertex v : new Vertex[]{edge.getV1(), edge.getV2()}) {
            if (v.isEmpty() || v.getBuilding().getOwner().equals(player)) {
                for (Edge adjacentEdge : v.getAdjacentEdges()) {
                    if (adjacentEdge != edge && !adjacentEdge.isEmpty() &&
                            adjacentEdge.getBuilding().getOwner().equals(player)) {
                        return true;
                    }
                }
            }
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
