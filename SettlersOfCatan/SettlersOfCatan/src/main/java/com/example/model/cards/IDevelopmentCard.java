package com.example.model.cards;

import com.example.model.game.CatanGameManager;
import com.example.model.player.Player;

public interface IDevelopmentCard {
    String getName();
    boolean play(CatanGameManager gameManager, Player player);

    void onPurchase(Player player);
}