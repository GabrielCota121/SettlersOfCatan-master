package com.example.model.building;

import com.example.model.player.Player;
import com.example.model.board.Vertex;

public class Settlement extends VertexBuilding {

    public Settlement(Player owner, Vertex location) {
        super(owner, location);
    }

    @Override
    public int getVictoryPoints() {
        return 1;
    }

    @Override
    public int getResourceYield() {
        return 1;
    }
}