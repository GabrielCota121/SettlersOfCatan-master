package com.catan.model.cards;

import com.catan.model.game.CatanGameManager;
import com.catan.model.player.Player;

public class VictoryPointCard implements IDevelopmentCard {
    @Override
    public String getName() { return "Victory Point"; }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        player.incrementVictoryPoints();
        gameManager.getLogger().log(player.getName() + " usou uma carta de Victory Point e recebe 1 ponto!");
        return true;
    }

    @Override
    public void onPurchase(Player player) {
        player.addPlayableCard(this);
    }
}