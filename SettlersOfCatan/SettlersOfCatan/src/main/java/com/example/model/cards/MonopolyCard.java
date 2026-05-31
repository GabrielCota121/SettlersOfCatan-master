package com.example.model.cards;

import com.example.model.game.CatanGameManager;
import com.example.model.player.Player;
import com.example.model.state.ITurnState;
import com.example.model.state.MonopolyState;

public class MonopolyCard implements IDevelopmentCard {
    @Override
    public String getName() { return "Monopoly"; }

    @Override
    public boolean play(CatanGameManager gameManager, Player player) {
        gameManager.getLogger().log(player.getName() + " usou Monopólio, safado! Escolha o recurso para roubar!");
        ITurnState previousState = gameManager.getCurrentTurn().getState();
        gameManager.getCurrentTurn().setState(new MonopolyState(player,previousState));
        return true;
    }

    @Override
    public void onPurchase(Player player) { player.addNewCard(this); }
}