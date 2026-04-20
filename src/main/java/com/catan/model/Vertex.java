package com.catan.model;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
    private final String id;
    private final double x;
    private final double y;
    private VertexBuilding building;
    private final List<Vertex> adjacentVertices;
    private final List<Edge> adjacentEdges;
    private final List<Tile> adjacentTiles;
    private Port port;

    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
        this.id = String.format("%.1f,%.1f", x, y);
        this.building = null;
        this.port = null;

        this.adjacentVertices = new ArrayList<>();
        this.adjacentEdges = new ArrayList<>();
        this.adjacentTiles = new ArrayList<>();
    }

    public void addAdjacentVertex(Vertex v) {
        if (!adjacentVertices.contains(v)) adjacentVertices.add(v);
    }
    public void addAdjacentEdge(Edge e) {
        if (!adjacentEdges.contains(e)) adjacentEdges.add(e);
    }

    public void addAdjacentTile(Tile tile) {
        if (!this.adjacentTiles.contains(tile)) {
            this.adjacentTiles.add(tile);
        }
    }

    public List<Tile> getAdjacentTiles() {
        return adjacentTiles;
    }

    public boolean respectsDistanceRule() {
        for (Vertex neighbor : adjacentVertices) {
            if (!neighbor.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasConnectingRoadFor(Player player) {
        for (Edge edge : adjacentEdges) {
            if (!edge.isEmpty() && edge.getBuilding().getOwner().equals(player)) {
                return true;
            }
        }
        return false;
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public VertexBuilding getBuilding() { return building; }
    public void setBuilding(VertexBuilding building) { this.building = building; }
    public boolean isEmpty() { return building == null; }
    public List<Vertex> getAdjacentVertices() { return adjacentVertices; }
    public List<Edge> getAdjacentEdges() { return adjacentEdges; }
    public Port getPort() { return port; }
    public void setPort(Port port) { this.port = port; }
    public boolean hasPort() { return port != null; }

    @Override
    public String toString() { return "V(" + id + ")"; }
}