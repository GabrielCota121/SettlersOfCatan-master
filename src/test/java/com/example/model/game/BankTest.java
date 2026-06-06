package com.example.model.game;

import com.example.model.board.Board;
import com.example.model.board.BoardFactory;
import com.example.model.board.Robber;
import com.example.model.board.Tile;
import com.example.model.board.Vertex;
import com.example.model.building.Settlement;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BankTest {

    private static final IGameLogger SILENT = new IGameLogger() {
        public void log(String m) {}
        public void error(String m) {}
    };

    private Bank bank;
    private Board board;
    private Robber robber;
    private StatisticsManager stats;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        board = BoardFactory.createStandardBoard();
        // SET posição inicial do Robber (DESERT)
        Tile desertTile = board.getTiles().stream()
            .filter(t -> t.getResource() == ResourceType.DESERT)
            .findFirst()
            .orElseThrow();
        robber = new Robber(desertTile);
        stats = new StatisticsManager();
    }

    // ─── Estado inicial ────────────────────────────────────────────────────────

    @Test
    void initialBank_has19OfEachNonDesertResource() {
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                assertEquals(19, bank.getWallet().getResourceAmount(type),
                    "Bank should start with 19 " + type);
            }
        }
    }

    @Test
    void initialTotalResources_is95() {
        assertEquals(95, bank.getWallet().getTotalCards());
    }

    // ─── Receber recursos ─────────────────────────────────────────────────────

    @Test
    void receiveResources_addsToBankWallet() {
        int before = bank.getWallet().getResourceAmount(ResourceType.WOOD);
        bank.receiveResources(Map.of(ResourceType.WOOD, 4));
        assertEquals(before + 4, bank.getWallet().getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void receiveResources_multipleTypes() {
        bank.receiveResources(Map.of(
            ResourceType.ORE, 2,
            ResourceType.WHEAT, 3
        ));
        assertEquals(21, bank.getWallet().getResourceAmount(ResourceType.ORE));
        assertEquals(22, bank.getWallet().getResourceAmount(ResourceType.WHEAT));
    }

    // ─── Distribuir recursos ──────────────────────────────────────────────────

    @Test
    void distributeResources_playerWithSettlementOnMatchingTile_receivesResource() {
        Player player = new Player(1, "Marcelle", "RED");

        Tile targetTile = board.getTiles().stream()
            .filter(t -> t.getResource() != ResourceType.DESERT && t.getNumberToken() > 0)
            .findFirst().orElseThrow();

        Vertex vertex = targetTile.getVertices()[0];
        vertex.setBuilding(new Settlement(player, vertex));

        int roll = targetTile.getNumberToken();
        int bankBefore = bank.getWallet().getResourceAmount(targetTile.getResource());

        bank.distributeResources(roll, board, robber, SILENT, stats);

        assertEquals(bankBefore - 1, bank.getWallet().getResourceAmount(targetTile.getResource()));
        assertEquals(1, player.getWallet().getResourceAmount(targetTile.getResource()));
    }

    @Test
    void distributeResources_recordsStatsForPlayer() {
        Player player = new Player(1, "Marcelle", "RED");
        Tile targetTile = board.getTiles().stream()
            .filter(t -> t.getResource() != ResourceType.DESERT && t.getNumberToken() > 0)
            .findFirst().orElseThrow();

        Vertex vertex = targetTile.getVertices()[0];
        vertex.setBuilding(new Settlement(player, vertex));

        bank.distributeResources(targetTile.getNumberToken(), board, robber, SILENT, stats);

        assertTrue(stats.getTotalResourcesGainedBy(player) > 0);
    }

    @Test
    void distributeResources_noMatchingTiles_playerReceivesNothing() {
        Player player = new Player(1, "Marcelle", "RED");

        Tile tile = board.getTiles().stream()
            .filter(t -> t.getResource() != ResourceType.DESERT && t.getNumberToken() > 0)
            .findFirst().orElseThrow();
        tile.getVertices()[0].setBuilding(new Settlement(player, tile.getVertices()[0]));

        // Roll a number that doesn't match any tile
        // Rolling 2 when the tile's token is different
        int mismatchRoll = (tile.getNumberToken() == 2) ? 3 : 2;
        int playerBefore = player.getWallet().getTotalCards();

        bank.distributeResources(mismatchRoll, board, robber, SILENT, stats);

        assertEquals(playerBefore, player.getWallet().getTotalCards());
    }

    @Test
    void distributeResources_robberBlocksTile_noResourceDistributed() {
        Player player = new Player(1, "Marcelle", "RED");

        Tile targetTile = board.getTiles().stream()
            .filter(t -> t.getResource() != ResourceType.DESERT && t.getNumberToken() > 0)
            .findFirst().orElseThrow();

        Vertex vertex = targetTile.getVertices()[0];
        vertex.setBuilding(new Settlement(player, vertex));

        robber = new Robber(targetTile);

        int playerBefore = player.getWallet().getTotalCards();
        bank.distributeResources(targetTile.getNumberToken(), board, robber, SILENT, stats);

        assertEquals(playerBefore, player.getWallet().getTotalCards());
    }

    @Test
    void distributeResources_emptyTileVertex_playerReceivesNothing() {
        bank.distributeResources(6, board, robber, SILENT, stats);
        assertEquals(95, bank.getWallet().getTotalCards());
    }

    @Test
    void distributeResources_nullStats_doesNotThrow() {
        Player player = new Player(1, "Marcelle", "RED");
        Tile targetTile = board.getTiles().stream()
            .filter(t -> t.getResource() != ResourceType.DESERT && t.getNumberToken() > 0)
            .findFirst().orElseThrow();
        targetTile.getVertices()[0].setBuilding(
            new Settlement(player, targetTile.getVertices()[0]));

        assertDoesNotThrow(() ->
            bank.distributeResources(targetTile.getNumberToken(), board, robber, SILENT, null));
    }
}
