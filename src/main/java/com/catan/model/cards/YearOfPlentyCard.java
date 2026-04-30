package com.catan.model.cards;

import com.catan.model.game.CatanGameManager;
import com.catan.model.player.Player;
import com.catan.model.state.ITurnState;
import com.catan.model.state.YearOfPlentyState;

public class YearOfPlentyCard implements IDevelopmentCard {
    @Override
    public String getName() {
        return "Year of Plenty";
    }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        gameManager.getLogger().log(player.getName() + " usou Year of Plenty! Escolha 2 recursos quaisquer!");
        ITurnState previousState = gameManager.getCurrentTurn().getState();
        gameManager.getCurrentTurn().setState(new YearOfPlentyState(player,previousState));
        return true;
    }

    @Override
    public void onPurchase(Player player) {
        player.addNewCard(this);
    }
}