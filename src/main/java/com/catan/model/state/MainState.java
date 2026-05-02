package com.catan.model.state;

import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.cards.VictoryPointCard;
import com.catan.model.game.Bank;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.building.*;
import com.catan.model.player.Player;

import java.util.Map;

public class MainState implements ITurnState {
    @Override
    public String getName() { return "Fase Principal"; }

    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();
        if(activePlayer.getNumSettlements() == 5){
            currentTurn.getGameManager().getLogger().log(activePlayer.getName() + " já tem 5 settlements construidos!!");
            return false;
        }
        Bank bank = currentTurn.getGameManager().getBank();

        if (!vertex.isEmpty() || !vertex.respectsDistanceRule() || !vertex.hasConnectingRoadFor(activePlayer)) return false;

        if (activePlayer.getWallet().payCost(BuildingCost.SETTLEMENT.getCost())) {
            bank.receiveResources(BuildingCost.SETTLEMENT.getCost());
            vertex.setBuilding(new Settlement(activePlayer, vertex));

            if (vertex.hasPort()) {
                currentTurn.getGameManager().applyPortBonus(activePlayer, vertex.getPort());
            }
            activePlayer.incrementSettlements();
            activePlayer.incrementVictoryPoints();

            // CASO haja quebra de estrada, devemos calcular novamente quem detem a maior estrada!
            // Escolhi botar pra sempre fazer isto por corretude! (é rápido, eu juro)
            CatanGameManager gm = currentTurn.getGameManager();
            gm.getRoadBonus().reevaluateAllPlayers(gm.getPlayers(), gm.getBoard().getEdges());
            checkWinCondition(currentTurn);
            return true;
        }
        return false;
    }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) {
        if (vertex.isEmpty()) return false;

        Player activePlayer = currentTurn.getCurrentPlayer();
        if(activePlayer.getNumCities() == 4){
            currentTurn.getGameManager().getLogger().log(activePlayer.getName() + " já tem 4 cities construidas!!");
            return false;
        }
        Bank bank = currentTurn.getGameManager().getBank();

        VertexBuilding currentBuilding = vertex.getBuilding();
        if (!(currentBuilding instanceof Settlement) || !currentBuilding.getOwner().equals(activePlayer)) {
            return false;
        }

        if (activePlayer.getWallet().payCost(BuildingCost.CITY.getCost())) {
            bank.receiveResources(BuildingCost.CITY.getCost());
            vertex.setBuilding(new City(activePlayer, vertex));
            activePlayer.decrementSettlements();
            activePlayer.incrementCities();
            activePlayer.incrementVictoryPoints();
            checkWinCondition(currentTurn);
            return true;
        }
        return false;
    }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) {
        if (!edge.isEmpty()) return false;

        Player activePlayer = currentTurn.getCurrentPlayer();
        if(activePlayer.getNumRoads() == 15){
            currentTurn.getGameManager().getLogger().log(activePlayer.getName() + " já tem 15 roads construidas!!");
            return false;
        }

        Bank bank = currentTurn.getGameManager().getBank();
        boolean connectsToOwnBuilding = edge.hasConnectingSettlementOrCityFor(activePlayer);
        boolean connectsToValidRoad = canConnectViaRoad(edge, activePlayer);

        if (!connectsToOwnBuilding && !connectsToValidRoad) {
            return false;
        }

        if (activePlayer.getWallet().payCost(BuildingCost.ROAD.getCost())) {
            bank.receiveResources(BuildingCost.ROAD.getCost());
            edge.setBuilding(new Road(activePlayer, edge));
            activePlayer.incrementRoads();
            currentTurn.getGameManager().getRoadBonus().updateLongestRoad(activePlayer, currentTurn.getGameManager().getBoard().getEdges());
            checkWinCondition(currentTurn);
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
        CatanGameManager gameManager = currentTurn.getGameManager();
        Player player = currentTurn.getCurrentPlayer();

        Map<ResourceType, Integer> cost = com.catan.model.building.BuildingCost.DEVELOPMENT_CARD.getCost();

        if (player.getWallet().payCost(cost)) {
            IDevelopmentCard card = gameManager.drawDevelopmentCard();

            if (card != null) {
                card.onPurchase(player);
                gameManager.getBank().receiveResources(cost);

                gameManager.getLogger().log(player.getName() + " comprou uma Development Card!");
                return true;
            } else {
                for (Map.Entry<ResourceType, Integer> entry : cost.entrySet()) {
                    if (entry.getValue() > 0) {
                        player.getWallet().addResource(entry.getKey(), entry.getValue());
                    }
                }
                gameManager.getLogger().log("O deck de development cards acabou!");
                return false;
            }
        }
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

    public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn) {

        if (currentTurn.hasPlayedDevCardThisTurn() && !(card instanceof VictoryPointCard)) {
            currentTurn.getGameManager().getLogger().log("Só pode usar uma Development Card por turno, zé!!!");
            return false;
        }

        boolean success = card.play(currentTurn.getGameManager(), currentTurn.getCurrentPlayer());

        if (success) {

            if (!(card instanceof VictoryPointCard)) {
                currentTurn.markDevCardAsPlayed();
            }
            currentTurn.getCurrentPlayer().removeCard(card);
            checkWinCondition(currentTurn);
        }
        return success;
    }

    private void checkWinCondition(Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();
        if (activePlayer.getVictoryPoints() >= 10) {
            currentTurn.getGameManager().getLogger().log("Fim do jogo! " + activePlayer.getName() + " ganhou com " + activePlayer.getVictoryPoints() + " pontos de vitória! GRATS!");
            currentTurn.setState(new GameOverState(activePlayer));
        }
    }
}