package com.catan.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BoardFactory {

    public static Board createStandardBoard() {
        Board board = new Board();

        List<ResourceType> resources = new ArrayList<>(Arrays.asList(
                ResourceType.WOOD, ResourceType.WOOD, ResourceType.WOOD, ResourceType.WOOD,
                ResourceType.WOOL, ResourceType.WOOL, ResourceType.WOOL, ResourceType.WOOL,
                ResourceType.WHEAT, ResourceType.WHEAT, ResourceType.WHEAT, ResourceType.WHEAT,
                ResourceType.BRICK, ResourceType.BRICK, ResourceType.BRICK,
                ResourceType.ORE, ResourceType.ORE, ResourceType.ORE,
                ResourceType.DESERT
        ));
        Collections.shuffle(resources);

        List<Integer> tokens = new ArrayList<>(Arrays.asList(
                2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12
        ));
        boolean valid = false;
        while (!valid) {
            Collections.shuffle(tokens);
            valid = checkProbabilityBalance(resources, tokens);
        }

        int[] hexesPerRow = {3, 4, 5, 4, 3};

        double hexSize = 400.0;
        double width = Math.sqrt(3) * hexSize;
        double height = 2 * hexSize;
        double verticalDist = height * 0.75;

        double centerX = 2000;
        double startY = 800;

        int tileId = 0;

        for (int row = 0; row < 5; row++) {
            int numHexes = hexesPerRow[row];
            double rowStartX = centerX - ((numHexes - 1) * width / 2.0);
            for (int col = 0; col < numHexes; col++) {
                double x = rowStartX + (col * width);
                double y = startY + (row * verticalDist);
                ResourceType res = resources.remove(0);
                int token = 0;
                if (res != ResourceType.DESERT) {
                    token = tokens.remove(0);
                }
                board.createTile(tileId++, res, token, x, y, hexSize);
            }
        }

        // Distribui os portos antes de finalizar a criação do tabuleiro!
        assignRandomPorts(board);

        return board;
    }

    private static void assignRandomPorts(Board board) {
        // 1. Cria a lista estrita dos 9 portos padrão (4 Genéricos + 5 Específicos)
        List<Port> portsToAssign = new ArrayList<>(Arrays.asList(
                new Port(null, 3), new Port(null, 3), new Port(null, 3), new Port(null, 3),
                new Port(ResourceType.WOOD, 2),
                new Port(ResourceType.BRICK, 2),
                new Port(ResourceType.WOOL, 2),
                new Port(ResourceType.WHEAT, 2),
                new Port(ResourceType.ORE, 2)
        ));
        Collections.shuffle(portsToAssign);

        // 2. Pega todas as arestas que ficam na Costa
        List<Edge> coastalEdges = new ArrayList<>();
        for (Edge e : board.getEdges()) {
            if (e.getV1().getAdjacentTiles().size() <= 2 && e.getV2().getAdjacentTiles().size() <= 2) {
                coastalEdges.add(e);
            }
        }
        Collections.shuffle(coastalEdges);

        // 3. Distribui exatamente os 9 portos com regras de espaçamento
        int portsPlaced = 0;
        for (Edge edge : coastalEdges) {
            if (portsPlaced >= 9) break; // Garante que só vai colocar 9!

            if (!edge.getV1().hasPort() && !edge.getV2().hasPort()) {

                boolean neighborHasPort = false;
                for (Vertex neighbor : edge.getV1().getAdjacentVertices()) {
                    if (neighbor.hasPort()) neighborHasPort = true;
                }
                for (Vertex neighbor : edge.getV2().getAdjacentVertices()) {
                    if (neighbor.hasPort()) neighborHasPort = true;
                }

                if (!neighborHasPort) {
                    Port port = portsToAssign.get(portsPlaced);
                    edge.getV1().setPort(port);
                    edge.getV2().setPort(port);
                    portsPlaced++;
                }
            }
        }

        // 4. Fallback de Segurança:
        // Se as regras de espaçamento ali em cima forem muito duras e não derem conta de posicionar todos os 9,
        // esse bloco de emergência garante que o resto da lista seja colocado no mar em arestas vazias.
        if (portsPlaced < 9) {
            for (Edge edge : coastalEdges) {
                if (portsPlaced >= 9) break;
                if (!edge.getV1().hasPort() && !edge.getV2().hasPort()) {
                    Port port = portsToAssign.get(portsPlaced);
                    edge.getV1().setPort(port);
                    edge.getV2().setPort(port);
                    portsPlaced++;
                }
            }
        }
    }

    private static boolean checkProbabilityBalance(List<ResourceType> resources, List<Integer> tokens) {
        int[][] neighbors = {
                {1, 3, 4}, {0, 2, 4, 5}, {1, 5, 6},
                {0, 4, 7, 8}, {0, 1, 3, 5, 8, 9}, {1, 2, 4, 6, 9, 10}, {2, 5, 10, 11},
                {3, 8, 12}, {3, 4, 7, 9, 12, 13}, {4, 5, 8, 10, 13, 14}, {5, 6, 9, 11, 14, 15}, {6, 10, 15},
                {7, 8, 13, 16}, {8, 9, 12, 14, 16, 17}, {9, 10, 13, 15, 17, 18}, {10, 11, 14, 18},
                {12, 13, 17}, {13, 14, 16, 18}, {14, 15, 17}
        };

        List<Integer> currentMap = new ArrayList<>();
        int tokenIdx = 0;
        for (ResourceType res : resources) {
            if (res == ResourceType.DESERT) {
                currentMap.add(0);
            } else {
                currentMap.add(tokens.get(tokenIdx++));
            }
        }

        for (int i = 0; i < 19; i++) {
            int num = currentMap.get(i);
            if (num == 0) continue;
            for (int neighborIdx : neighbors[i]) {
                int neighborNum = currentMap.get(neighborIdx);
                if ((num == 6 || num == 8) && (neighborNum == 6 || neighborNum == 8)) {
                    return false;
                }
                if (num == neighborNum) {
                    return false;
                }
            }
        }
        return true;
    }
}