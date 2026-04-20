package com.catan.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final List<Tile> tiles;
    private final Map<String, Vertex> vertexRegistry;
    private final Map<String, Edge> edgeRegistry;

    public Board() {
        this.tiles = new ArrayList<>();
        this.vertexRegistry = new HashMap<>();
        this.edgeRegistry = new HashMap<>();
    }

    protected void createTile(int id, ResourceType resource, int numberToken, double centerX, double centerY, double hexSize) {
        Tile tile = new Tile(id, resource, numberToken);
        Vertex[] hexVertices = new Vertex[6];
        Edge[] hexEdges = new Edge[6];

        for (int i = 0; i < 6; i++) {
            double angle_deg = 60 * i + 30;
            double angle_rad = Math.PI / 180 * angle_deg;
            double vx = centerX + hexSize * Math.cos(angle_rad);
            double vy = centerY + hexSize * Math.sin(angle_rad);

            Vertex tempVertex = new Vertex(vx, vy);
            String vId = tempVertex.getId();

            if (!vertexRegistry.containsKey(vId)) {
                vertexRegistry.put(vId, tempVertex);
            }
            hexVertices[i] = vertexRegistry.get(vId);
            hexVertices[i].addAdjacentTile(tile);
        }

        for (int i = 0; i < 6; i++) {
            Vertex v1 = hexVertices[i];
            Vertex v2 = hexVertices[(i + 1) % 6];

            Edge tempEdge = new Edge(v1, v2);
            String eId = tempEdge.toString();

            if (!edgeRegistry.containsKey(eId)) {
                edgeRegistry.put(eId, tempEdge);

                v1.addAdjacentVertex(v2);
                v2.addAdjacentVertex(v1);
                v1.addAdjacentEdge(tempEdge);
                v2.addAdjacentEdge(tempEdge);
            }
            hexEdges[i] = edgeRegistry.get(eId);
        }

        tile.setVertices(hexVertices);
        tile.setEdges(hexEdges);
        tiles.add(tile);
    }

    public List<Tile> getTiles() { return tiles; }
    public List<Vertex> getVertices() { return new ArrayList<>(vertexRegistry.values()); }
    public List<Edge> getEdges() { return new ArrayList<>(edgeRegistry.values()); }
}