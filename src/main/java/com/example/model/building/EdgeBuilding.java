package com.example.model.building;

import com.example.model.player.Player;
import com.example.model.board.Edge;

public abstract class EdgeBuilding {
    protected Player owner;
    protected Edge location;

    public EdgeBuilding(Player owner, Edge location) {
        this.owner = owner;
        this.location = location;
    }

    public Player getOwner() {
        return owner;
    }

    public Edge getLocation() {
        return location;
    }
}