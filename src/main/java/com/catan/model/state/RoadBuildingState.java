package com.catan.model.state;

import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.building.Road;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.Turn;
import com.catan.model.player.Player;

public class RoadBuildingState implements ITurnState {
    private final Player player;
    private final ITurnState previousState;
    private int roadsBuilt;

    public RoadBuildingState(Player player, ITurnState previousState) {
        this.player = player;
        this.previousState = previousState;
        this.roadsBuilt = 0;
    }

    @Override
    public String getName() {
        return "RoadBuildingState";
    }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) {
        if (!edge.isEmpty()) return false;

        if (player.getNumRoads() == 15) {
            currentTurn.getGameManager().getLogger().log(player.getName() + " já tem 15 roads construídas!!");
            currentTurn.setState(previousState);
            return false;
        }

        boolean connectsToOwnBuilding = edge.hasConnectingSettlementOrCityFor(player);
        boolean connectsToValidRoad = canConnectViaRoad(edge, player);

        if (!connectsToOwnBuilding && !connectsToValidRoad) {return false;}

        edge.setBuilding(new Road(player, edge));
        player.incrementRoads();
        roadsBuilt++;

        if (roadsBuilt >= 2 || player.getNumRoads() == 15) {
            currentTurn.getGameManager().getLogger().log("Road Building concluída!");
            currentTurn.setState(previousState);
        }

        return true;
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

    @Override public boolean buildSettlement(Vertex vertex, Turn currentTurn){return false;}
    @Override public boolean buildCity(Vertex vertex, Turn currentTurn){return false;}
    @Override public boolean buyDevelopmentCard(Turn currentTurn) {return false;}
    @Override public boolean rollDice(Turn currentTurn){return false;}
    @Override public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn){return false;}
    @Override public boolean canEndTurn(){return false;}
    @Override public boolean canRollDice() {return false;}
    @Override public boolean endTurn(Turn currentTurn){return false;}
}