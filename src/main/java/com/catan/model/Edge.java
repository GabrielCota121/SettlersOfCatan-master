package com.catan.model;

public class Edge {
    private final String id;
    private final Vertex v1;
    private final Vertex v2;
    private EdgeBuilding building;

    public Edge(Vertex v1, Vertex v2) {
        this.v1 = v1;
        this.v2 = v2;
        this.building = null;
        if (v1.getId().compareTo(v2.getId()) < 0) {
            this.id = "E[" + v1.getId() + "|" + v2.getId() + "]";
        } else {
            this.id = "E[" + v2.getId() + "|" + v1.getId() + "]";
        }
    }

    // mt mais limpo assim
    public boolean hasConnectingRoadFor(Player player) {
        return v1.hasConnectingRoadFor(player) ||  v2.hasConnectingRoadFor(player);
    }

    // mt mais limpo AINDA assim
    public boolean hasConnectingSettlementOrCityFor(Player player) {
        boolean v1HasBuilding = !v1.isEmpty() && v1.getBuilding().getOwner().equals(player);
        boolean v2HasBuilding = !v2.isEmpty() && v2.getBuilding().getOwner().equals(player);
        return v1HasBuilding || v2HasBuilding;
    }

    public Vertex getV1() { return v1; }
    public Vertex getV2() { return v2; }
    public EdgeBuilding getBuilding() { return building; }
    public void setBuilding(EdgeBuilding building) { this.building = building; }
    public boolean isEmpty() { return building == null; }

    @Override
    public String toString() {
        return id;
    }
}

