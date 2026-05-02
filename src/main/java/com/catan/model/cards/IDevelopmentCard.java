package com.catan.model.cards;

import com.catan.model.game.CatanGameManager;
import com.catan.model.player.Player;

public interface IDevelopmentCard {
    String getName();
    boolean play(CatanGameManager gameManager, Player player);

    void onPurchase(Player player);
}