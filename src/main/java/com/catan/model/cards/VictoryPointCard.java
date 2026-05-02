package com.catan.model.cards;

import com.catan.model.game.CatanGameManager;
import com.catan.model.player.Player;

public class VictoryPointCard implements IDevelopmentCard {
    @Override
    public String getName() { return "Victory Point"; }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        player.incrementVictoryPoints();
        return true;
    }

    @Override
    public void onPurchase(Player player) {
        player.addPlayableCard(this);
    }
}