package com.catan.model.board;

import com.catan.model.game.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RobberTest {

    private Tile desertTile;
    private Tile woodTile;
    private Robber robber;

    @BeforeEach
    void setUp() {
        desertTile = new Tile(0, ResourceType.DESERT, 0);
        woodTile = new Tile(1, ResourceType.WOOD, 6);
        robber = new Robber(desertTile);
    }

    @Test
    void startsOnInitialTile() {
        assertEquals(desertTile, robber.getCurrentTile());
    }

    @Test
    void moveChangesCurrentTile() {
        robber.move(woodTile);

        assertEquals(woodTile, robber.getCurrentTile());
    }

    @Test
    void moveToSameTileKeepsSameTile() {
        robber.move(desertTile);

        assertEquals(desertTile, robber.getCurrentTile());
    }

    @Test
    void moveCanBeCalledMultipleTimes() {
        Tile mountainTile = new Tile(2, ResourceType.ORE, 8);

        robber.move(woodTile);
        robber.move(mountainTile);

        assertEquals(mountainTile, robber.getCurrentTile());
    }
}
