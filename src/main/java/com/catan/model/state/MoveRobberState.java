package com.catan.model.state;

import com.catan.model.game.CatanGameManager;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.board.Edge;
import com.catan.model.board.Robber;
import com.catan.model.board.Tile;
import com.catan.model.board.Vertex;
import com.catan.model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class MoveRobberState implements ITurnState {

    @Override
    public String getName() {
        return "Aguardando mover o Ladrão! BORAA!!";
    }

    public List<Player> moveRobber(Tile newTile, Turn currentTurn) {
        CatanGameManager manager = currentTurn.getGameManager();
        Robber robber = manager.getRobber();

        if (newTile.equals(robber.getCurrentTile())) {
            manager.getLogger().log("O ladrão deve ser movido para um tile diferente do atual, gënio!");
            return null;
        }

        robber.move(newTile);
        manager.getLogger().log("Ladrão movido para o tile de " + newTile.getResource());

        List<Player> victims = new ArrayList<>();
        Player currentPlayer = currentTurn.getCurrentPlayer();

        for (Vertex v : newTile.getVertices()) {
            if (!v.isEmpty()) {
                Player owner = v.getBuilding().getOwner();
                if (!owner.equals(currentPlayer) && !victims.contains(owner) && owner.getWallet().getTotalCards() > 0) {
                    victims.add(owner);
                }
            }
        }

        return victims;
    }

    public void executeSteal(Player victim, Turn currentTurn) {
        if (victim != null) {
            ResourceType stolenResource = victim.getWallet().removeRandomResource();
            if (stolenResource != null) {
                currentTurn.getCurrentPlayer().getWallet().addResource(stolenResource, 1);
                currentTurn.getGameManager().getLogger().log(
                        currentTurn.getCurrentPlayer().getName() + " roubou uma carta de " + victim.getName() + "!"
                );
            }
        } else {
            currentTurn.getGameManager().getLogger().log("Ninguém para roubar neste terreno. Que azar!");
        }
        currentTurn.setState(new MainState());
    }

    @Override public boolean rollDice(Turn currentTurn) { return false; }
    @Override public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }
    @Override public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buyDevelopmentCard(Turn currentTurn) { return false; }
    @Override public boolean endTurn(Turn currentTurn) { return false; }
    @Override public boolean canEndTurn() { return false; }
    @Override public boolean canRollDice() { return false; }
}