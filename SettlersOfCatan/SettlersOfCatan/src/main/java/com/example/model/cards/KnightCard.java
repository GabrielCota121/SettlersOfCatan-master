package com.example.model.cards;

import com.example.model.game.CatanGameManager;
import com.example.model.player.Player;
import com.example.model.state.ITurnState;
import com.example.model.state.MoveRobberState;

public class KnightCard implements IDevelopmentCard {
    @Override
    public String getName() { return "Knight"; }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        gameManager.incrementKnightsPlayed(player);
        gameManager.getLogger().log(player.getName() + " jogou um Knight! Mova o Robber!");
        ITurnState previousState = gameManager.getCurrentTurn().getState();
        gameManager.getCurrentTurn().setState(new MoveRobberState(previousState));
        return true;
    }

    @Override
    public void onPurchase(Player player) { player.addNewCard(this); }
}