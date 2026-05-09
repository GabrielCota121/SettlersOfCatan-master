package com.catan.model.board;

import com.catan.model.game.ResourceType;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final List<Tile> tiles;
    private final List<Vertex> vertices;
    private final List<Edge> edges;

    public Board() {
        this.tiles = new ArrayList<>();
        this.vertices = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    private Vertex getOrCreateVertex(double x, double y) {
        for (Vertex v : vertices) {
            if (Math.hypot(v.getX() - x, v.getY() - y) < 5.0) {
                return v;
            }
        }
        Vertex newVertex = new Vertex(x, y);
        vertices.add(newVertex);
        return newVertex;
    }

    private Edge getOrCreateEdge(Vertex v1, Vertex v2) {
        for (Edge e : edges) {
            if ((e.getV1().equals(v1) && e.getV2().equals(v2)) ||
                    (e.getV1().equals(v2) && e.getV2().equals(v1))) {
                return e;
            }
        }
        Edge newEdge = new Edge(v1, v2);
        edges.add(newEdge);
        return newEdge;
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

            hexVertices[i] = getOrCreateVertex(vx, vy);
            hexVertices[i].addAdjacentTile(tile);
        }

        for (int i = 0; i < 6; i++) {
            Vertex v1 = hexVertices[i];
            Vertex v2 = hexVertices[(i + 1) % 6];

            Edge edge = getOrCreateEdge(v1, v2);
            hexEdges[i] = edge;

            v1.addAdjacentVertex(v2);
            v2.addAdjacentVertex(v1);
            v1.addAdjacentEdge(edge);
            v2.addAdjacentEdge(edge);
        }

        tile.setVertices(hexVertices);
        tile.setEdges(hexEdges);
        tiles.add(tile);
    }

    public List<Tile> getTiles() { return tiles; }
    public List<Vertex> getVertices() { return vertices; }
    public List<Edge> getEdges() { return edges; }
}