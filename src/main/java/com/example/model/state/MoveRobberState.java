package com.example.model.state;

import com.example.model.cards.IDevelopmentCard;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.board.Edge;
import com.example.model.board.Robber;
import com.example.model.board.Tile;
import com.example.model.board.Vertex;
import com.example.model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class MoveRobberState implements ITurnState {

    private final ITurnState previousState;

    public MoveRobberState(ITurnState previousState) {
        this.previousState = previousState;
    }

    @Override
    public String getName() {
        return "Aguardando mover o Robber! BORAA!!";
    }

    public List<Player> moveRobber(Tile newTile, Turn currentTurn) {
        CatanGameManager manager = currentTurn.getGameManager();
        Robber robber = manager.getRobber();

        if (newTile.equals(robber.getCurrentTile())) {
            return null;
        }

        robber.move(newTile);
        manager.getLogger().log("Robber movido para o tile de " + newTile.getResource());

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
            currentTurn.getGameManager().getLogger().log("Ninguém para roubar neste terreno. MUITO BOA JOGADA, HEIN!!!!");
        }
        currentTurn.setState(previousState);
    }

    @Override public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn) {return false;}
    @Override public boolean rollDice(Turn currentTurn) { return false; }
    @Override public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }
    @Override public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buyDevelopmentCard(Turn currentTurn) { return false; }
    @Override public boolean endTurn(Turn currentTurn) { return false; }
    @Override public boolean canEndTurn() { return false; }
    @Override public boolean canRollDice() { return false; }
}