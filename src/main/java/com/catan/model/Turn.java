package com.catan.model;

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