package com.catan.model.board;

public class Robber {
    private Tile currentTile;

    public Robber(Tile initialTile) {
        this.currentTile = initialTile;
    }

    public Tile getCurrentTile() {
        return currentTile;
    }

    public void move(Tile newTile) {
        this.currentTile = newTile;
    }
}