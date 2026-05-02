package com.catan.model.board;

import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import java.util.ArrayList;
import java.util.List;

public class RoadBonus {
    private static final int MIN_ROADS_FOR_BONUS = 5;
    private Player currentHolder = null;
    private int currentLongest = 4;
    private final IGameLogger logger;

    public RoadBonus(IGameLogger logger) {
        this.logger = logger;
    }

    public void updateLongestRoad(Player player, List<Edge> allBoardEdges) {
        int playerMaxRoad = calculateMaxRoadForPlayer(player, allBoardEdges);
        player.setLongestRoad(playerMaxRoad);

        if (playerMaxRoad > currentLongest) {

            if (currentHolder != null && currentHolder.equals(player)) {
                currentLongest = playerMaxRoad;
                return;
            }

            if (currentHolder != null) {
                currentHolder.decrementVictoryPoints();
                currentHolder.decrementVictoryPoints();
                logger.log(player.getName() + " construiu " + playerMaxRoad + " estradas e pegou a Longest Road de " + currentHolder.getName() + "!");
            } else {
                logger.log(player.getName() + " construiu " + playerMaxRoad + " estradas e conquistou a Longest Road!");
            }

            currentHolder = player;
            currentLongest = playerMaxRoad;
            player.incrementVictoryPoints();
            player.incrementVictoryPoints();
        }
    }

    private int calculateMaxRoadForPlayer(Player player, List<Edge> allBoardEdges) {
        List<Edge> playerEdges = getPlayerEdges(player, allBoardEdges);
        int maxLength = 0;

        for (Edge startEdge : playerEdges) {
            maxLength = Math.max(maxLength, dfs(startEdge, startEdge.getV1(), playerEdges, new ArrayList<>(), player));
            maxLength = Math.max(maxLength, dfs(startEdge, startEdge.getV2(), playerEdges, new ArrayList<>(), player));
        }

        return maxLength;
    }

    private int dfs(Edge currentEdge, Vertex expandingVertex, List<Edge> playerEdges, List<Edge> visited, Player player) {
        visited.add(currentEdge);
        int maxDepth = visited.size();

        if (isVertexBlockedByOpponent(expandingVertex, player)) {
            visited.remove(visited.size() - 1);
            return maxDepth;
        }

        for (Edge neighbor : playerEdges) {
            if (!visited.contains(neighbor)) {
                if (neighbor.getV1().equals(expandingVertex)) {
                    int depth = dfs(neighbor, neighbor.getV2(), playerEdges, visited, player);
                    maxDepth = Math.max(maxDepth, depth);
                } else if (neighbor.getV2().equals(expandingVertex)) {
                    int depth = dfs(neighbor, neighbor.getV1(), playerEdges, visited, player);
                    maxDepth = Math.max(maxDepth, depth);
                }
            }
        }

        visited.remove(visited.size() - 1);
        return maxDepth;
    }

    public void reevaluateAllPlayers(List<Player> allPlayers, List<Edge> allBoardEdges) {
        int globalMax = 0;
        List<Player> leaders = new ArrayList<>();

        for (Player p : allPlayers) {
            int pMax = calculateMaxRoadForPlayer(p, allBoardEdges);
            p.setLongestRoad(pMax);

            if (pMax > globalMax) {
                globalMax = pMax;
                leaders.clear();
                leaders.add(p);
            } else if (pMax == globalMax && pMax >= MIN_ROADS_FOR_BONUS) {
                leaders.add(p);
            }
        }

        if (globalMax < MIN_ROADS_FOR_BONUS) {
            clearCurrentHolder("Ninguém atingiu o mínimo de " + MIN_ROADS_FOR_BONUS + " estradas!");

        } else if (leaders.size() > 1) {
            boolean holderKeeps = false;
            if (currentHolder != null) {
                for (Player leader : leaders) {
                    if (leader.equals(currentHolder)) {
                        holderKeeps = true;
                        break;
                    }
                }
            }

            if (holderKeeps) {
                currentLongest = globalMax;
            } else {
                clearCurrentHolder("Ninguém possui a Longest Road no momento!");
            }

        } else if (leaders.size() == 1) {
            Player newHolder = leaders.get(0);

            if (currentHolder == null || !currentHolder.equals(newHolder)) {
                if (currentHolder != null) {
                    currentHolder.decrementVictoryPoints();
                    currentHolder.decrementVictoryPoints();
                    logger.log("A Longest de " + currentHolder.getName() + " foi quebrada! " + newHolder.getName() + " assume a liderança com " + globalMax + " roads!");
                } else {
                    logger.log(newHolder.getName() + " conquistou a Longest Road após um desempate!");
                }
                currentHolder = newHolder;
                newHolder.incrementVictoryPoints();
                newHolder.incrementVictoryPoints();
            }
            currentLongest = globalMax;
        }
    }

    private void clearCurrentHolder(String reason) {
        if (currentHolder != null) {
            currentHolder.decrementVictoryPoints();
            currentHolder.decrementVictoryPoints();
            logger.log("A Longest Road de " + currentHolder.getName() + " foi quebrada! " + reason);
            currentHolder = null;
            currentLongest = 4;
        }
    }

    private List<Edge> getPlayerEdges(Player player, List<Edge> allBoardEdges) {
        List<Edge> playerEdges = new ArrayList<>();
        for (Edge edge : allBoardEdges) {
            if (!edge.isEmpty() && edge.getBuilding().getOwner().equals(player)) {
                playerEdges.add(edge);
            }
        }
        return playerEdges;
    }

    private boolean isVertexBlockedByOpponent(Vertex v, Player p) {
        return !v.isEmpty() && !v.getBuilding().getOwner().equals(p);
    }

    public Player getCurrentHolder() { return currentHolder; }
    public int getCurrentLongest() { return currentLongest; }
}