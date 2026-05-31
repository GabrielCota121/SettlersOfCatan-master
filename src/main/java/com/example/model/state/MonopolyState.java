package com.example.model.state;

// ⬇️ IMPORTS ATUALIZADOS PARA O NOVO PACOTE DO SPRING BOOT ⬇️
import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.cards.IDevelopmentCard;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.player.Player;

import java.util.HashMap;
import java.util.Map;

public class MonopolyState implements ITurnState {
    private final Player player;
    private final ITurnState previousState;

    public MonopolyState(Player player, ITurnState previousState) {
        this.player = player;
        this.previousState = previousState;
    }

    @Override
    public String getName() {
        return "MonopolyState";
    }

    public void chooseResource(ResourceType chosenType, Turn currentTurn) {
        CatanGameManager gameManager = currentTurn.getGameManager();
        currentTurn.setState(previousState);
        int totalStolen = 0;
        for (Player target : gameManager.getPlayers()) {
            if (target.equals(player)) continue;

            int amount = target.getWallet().getResourceAmount(chosenType);

            if (amount > 0) {
                Map<ResourceType, Integer> costMap = new HashMap<>();
                costMap.put(chosenType, amount);
                if (target.getWallet().payCost(costMap)) {
                    player.getWallet().addResource(chosenType, amount);
                    totalStolen += amount;
                }
            }
        }

        if (totalStolen > 0) {
            gameManager.getLogger().log(player.getName() + " roubou um total de " + totalStolen + " " + chosenType.name() + "(s) dos outros jogadores!");
        } else {
            gameManager.getLogger().log("Que azar! Ninguém tinha " + chosenType.name() + " para o " + player.getName() + " roubar.");
        }
    }

    @Override public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }
    @Override public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buyDevelopmentCard(Turn currentTurn) { return false; }
    @Override public boolean rollDice(Turn currentTurn) { return false; }
    @Override public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn) { return false; }
    @Override public boolean canEndTurn() { return false; }
    @Override public boolean canRollDice() { return false; }
    @Override public boolean endTurn(Turn currentTurn) { return false; }
}