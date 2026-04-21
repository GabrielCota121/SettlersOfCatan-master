package com.catan.model.board;

import com.catan.model.game.ResourceType;

public class Tile {
    private final int id;
    private final ResourceType resource;
    private final int numberToken;

    private Vertex[] vertices;
    private Edge[] edges;

    public Tile(int id, ResourceType resource, int numberToken) {
        this.id = id;
        this.resource = resource;
        this.numberToken = numberToken;
    }

    public void setVertices(Vertex[] vertices) {
        if (vertices.length != 6) {
            throw new IllegalArgumentException("Um hexágono deve ter 6 vértices, né");
        }
        this.vertices = vertices;
    }

    public void setEdges(Edge[] edges) {
        if (edges.length != 6) {
            throw new IllegalArgumentException("Um hexágono deve ter 6 arestas, né");
        }
        this.edges = edges;
    }
    public int getId() { return id; }
    public ResourceType getResource() { return resource; }
    public int getNumberToken() { return numberToken; }
    public Vertex[] getVertices() { return vertices; }
    public Edge[] getEdges() { return edges; }
}