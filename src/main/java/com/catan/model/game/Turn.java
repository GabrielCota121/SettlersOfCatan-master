package com.catan.model.game;

import com.catan.model.player.Player;
import com.catan.model.state.ITurnState;

public class Turn {
    private Player currentPlayer;
    private ITurnState currentState;
    private final CatanGameManager gameManager;

    public Turn(Player player, CatanGameManager gameManager) {
        this.currentPlayer = player;
        this.gameManager = gameManager;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public ITurnState getState() {return currentState;}

    public void setState(ITurnState state) {
        this.currentState = state;
    }

    public CatanGameManager getGameManager() { return gameManager; }
}