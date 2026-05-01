package com.catan.model.game;

import com.catan.model.board.Board;
import com.catan.model.board.Robber;
import com.catan.model.board.Tile;
import com.catan.model.board.Vertex;
import com.catan.model.building.VertexBuilding;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import com.catan.model.player.ResourceWallet;

import java.util.HashMap;
import java.util.Map;

public class Bank {
    private final ResourceWallet wallet;

    public Bank() {
        this.wallet = new ResourceWallet();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                wallet.addResource(type, 19);
            }
        }
    }

    public ResourceWallet getWallet() {
        return wallet;
    }

    public void receiveResources(Map<ResourceType, Integer> resources) {
        for (Map.Entry<ResourceType, Integer> entry : resources.entrySet()) {
            wallet.addResource(entry.getKey(), entry.getValue());
        }
    }

    public void distributeResources(int roll, Board board, Robber robber, IGameLogger logger) {
        Map<ResourceType, Integer> totalDemand = new HashMap<>();
        Map<Player, Map<ResourceType, Integer>> pendingDistribution = new HashMap<>();

        for (Tile tile : board.getTiles()) {
            if (tile.getNumberToken() == roll && !tile.equals(robber.getCurrentTile())) {
                ResourceType resourceYielded = tile.getResource();

                for (Vertex vertex : tile.getVertices()) {
                    if (!vertex.isEmpty()) {
                        VertexBuilding building = vertex.getBuilding();
                        Player owner = building.getOwner();
                        int yield = building.getResourceYield();

                        totalDemand.put(resourceYielded, totalDemand.getOrDefault(resourceYielded, 0) + yield);

                        pendingDistribution.putIfAbsent(owner, new HashMap<>());
                        pendingDistribution.get(owner).put(
                                resourceYielded,
                                pendingDistribution.get(owner).getOrDefault(resourceYielded, 0) + yield
                        );
                    }
                }
            } else if (tile.getNumberToken() == roll && tile.equals(robber.getCurrentTile())) {
                logger.log("O ladrão bloqueou a produção de " + tile.getResource() + "!");
            }
        }

        for (Map.Entry<Player, Map<ResourceType, Integer>> entry : pendingDistribution.entrySet()) {
            Player player = entry.getKey();
            Map<ResourceType, Integer> playerDue = entry.getValue();

            for (Map.Entry<ResourceType, Integer> resEntry : playerDue.entrySet()) {
                ResourceType type = resEntry.getKey();
                int amountDue = resEntry.getValue();

                if (wallet.getResourceAmount(type) >= totalDemand.get(type)) {
                    wallet.removeResource(type, amountDue);
                    player.getWallet().addResource(type, amountDue);
                    logger.log(player.getName() + " recebeu " + amountDue + " " + type);
                } else {
                    if (totalDemand.get(type) > 0) {
                        logger.log("Faltou cartas no banco! Não tem " + type + " suficiente! Ninguém recebe ))):");
                        totalDemand.put(type, 0);
                    }
                }
            }
        }
    }
}