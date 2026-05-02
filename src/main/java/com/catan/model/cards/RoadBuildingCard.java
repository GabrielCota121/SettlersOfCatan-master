package com.catan.model.cards;

import com.catan.model.game.CatanGameManager;
import com.catan.model.player.Player;
import com.catan.model.state.ITurnState;
import com.catan.model.state.RoadBuildingState;

public class RoadBuildingCard implements IDevelopmentCard {
    @Override
    public String getName() { return "Road Building"; }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        gameManager.getLogger().log(player.getName() + " usou Road Building! Const'oi 2 Roads de graça!");
        ITurnState previousState = gameManager.getCurrentTurn().getState();
        gameManager.getCurrentTurn().setState(new RoadBuildingState(player,previousState));
        return true;
    }

    @Override
    public void onPurchase(Player player) { player.addNewCard(this); }
}