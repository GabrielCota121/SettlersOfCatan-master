package com.catan.model.building;

import com.catan.model.player.Player;
import com.catan.model.board.Edge;

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