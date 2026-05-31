package com.example.network.protocol;

/**
 * Uma construção no tabuleiro dentro do {@link GameStateDTO}.
 * Para settlements/cities, {@code locationId} é o id do vértice; para estradas,
 * o id da aresta. {@code type} é SETTLEMENT, CITY ou ROAD.
 */
public class BuildingDTO {

    private String locationId; // Vertex.getId() ou Edge.getId()
    private String type;       // SETTLEMENT | CITY | ROAD
    private String ownerColor;
    private String ownerName;

    public BuildingDTO() {}

    public BuildingDTO(String locationId, String type, String ownerColor, String ownerName) {
        this.locationId = locationId;
        this.type = type;
        this.ownerColor = ownerColor;
        this.ownerName = ownerName;
    }

    public String getLocationId() { return locationId; }
    public void setLocationId(String v) { this.locationId = v; }

    public String getType() { return type; }
    public void setType(String v) { this.type = v; }

    public String getOwnerColor() { return ownerColor; }
    public void setOwnerColor(String v) { this.ownerColor = v; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String v) { this.ownerName = v; }
}
