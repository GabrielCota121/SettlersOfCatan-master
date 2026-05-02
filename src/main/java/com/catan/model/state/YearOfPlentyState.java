package com.catan.model.state;

import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.Bank;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.player.Player;

public class YearOfPlentyState implements ITurnState {
    private final Player player;
    private final ITurnState previousState;

    public YearOfPlentyState(Player player, ITurnState previousState) {
        this.player = player;
        this.previousState = previousState;
    }

    @Override
    public String getName() {
        return "YearOfPlentyState";
    }

    public void chooseResources(ResourceType res1, ResourceType res2, Turn currentTurn) {
        CatanGameManager gameManager = currentTurn.getGameManager();
        Bank bank = gameManager.getBank();

        if (res1 == res2) {
            int available = bank.getWallet().getResourceAmount(res1);
            if (available >= 2) {
                transferResource(bank, player, res1, 2);
                gameManager.getLogger().log(player.getName() + " pegou 2 " + res1.name() + "(s) do banco!");
            } else if (available == 1) {
                transferResource(bank, player, res1, 1);
                gameManager.getLogger().log("O banco só tinha 1 " + res1.name() + " disponível. " + player.getName() + " pegou o que tinha!");
            } else {
                gameManager.getLogger().log("O banco não tem mais " + res1.name() + "! " + player.getName() + " desperdiçou a escolha.");
            }
        } else {
            handleSingleResource(res1, bank, player, gameManager);
            handleSingleResource(res2, bank, player, gameManager);
        }
        currentTurn.setState(previousState);
    }

    private void transferResource(Bank bank, Player player, ResourceType type, int amount) {
        bank.getWallet().removeResource(type, amount);
        player.getWallet().addResource(type, amount);
    }

    private void handleSingleResource(ResourceType type, Bank bank, Player player, CatanGameManager gameManager) {
        if (bank.getWallet().getResourceAmount(type) > 0) {
            transferResource(bank, player, type, 1);
            gameManager.getLogger().log(player.getName() + " pegou 1 " + type.name() + " do banco.");
        } else {
            gameManager.getLogger().log("O banco não tem " + type.name() + " disponível!");
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