package com.catan.model.building;

import com.catan.model.player.Player;
import com.catan.model.board.Edge;

public class Road extends EdgeBuilding {

    public Road(Player owner, Edge location) {
        super(owner, location);
    }
}