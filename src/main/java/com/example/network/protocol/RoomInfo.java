package com.example.network.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Visão serializável de uma sala, enviada ao cliente (ROOM_LIST, ROOM_JOINED,
 * ROOM_UPDATE, GAME_STARTED). É um "DTO" sem lógica nem sessões do servidor.
 */
public class RoomInfo {

    private String id;
    private String name;
    private int maxPlayers;
    private String status;            // WAITING / IN_GAME / FINISHED
    private List<PlayerInfo> players = new ArrayList<>();

    public RoomInfo() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<PlayerInfo> getPlayers() { return players; }
    public void setPlayers(List<PlayerInfo> players) {
        this.players = players == null ? new ArrayList<>() : players;
    }

    @JsonIgnore
    public int getPlayerCount() { return players.size(); }
}
