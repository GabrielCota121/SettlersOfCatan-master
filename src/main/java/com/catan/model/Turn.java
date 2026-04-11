package com.catan.model;

public class Turn {
    private Player currentPlayer;
    // TODO implementar TurnState e as classes que implementam
    private TurnState currentState;

    public Turn(Player startingPlayer) {
        this.currentPlayer = startingPlayer;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public TurnState getState() {return currentState;}

    // Futuramente quero que fique mais ou menos assin:

    // public void changeState(TurnState newState) {..}
    // public void handleAction(..) {..}
}