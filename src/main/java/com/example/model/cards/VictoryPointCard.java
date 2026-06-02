package com.example.model.cards;

import com.example.model.game.CatanGameManager;
import com.example.model.player.Player;

public class VictoryPointCard implements IDevelopmentCard {
    @Override
    public String getName() { return "Victory Point"; }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        gameManager.getLogger().log(
            player.getName() + " já tem o ponto desta carta de Vitória.");
        return false; // não há ação de jogar
    }

    @Override
    public void onPurchase(Player player) {
        player.incrementVictoryPoints();
        player.addNewCard(this); // visível, ponto já contado, nunca jogável manualmente
    }
}