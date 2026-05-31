package com.example.network.room;

import com.example.network.protocol.RoomInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registro central de salas do servidor. Thread-safe: o mapa é concorrente e
 * as mutações de cada sala são sincronizadas dentro da própria {@link Room}.
 */
@Component
public class RoomManager {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(1);

    public Room createRoom(String name, int maxPlayers) {
        String id = "room-" + idSeq.getAndIncrement();
        String safeName = (name == null || name.isBlank()) ? ("Sala " + id) : name.trim();
        Room room = new Room(id, safeName, maxPlayers);
        rooms.put(id, room);
        return room;
    }

    public Room getRoom(String roomId) {
        return roomId == null ? null : rooms.get(roomId);
    }

    /** Remove a sala se estiver vazia. */
    public void removeIfEmpty(String roomId) {
        Room room = rooms.get(roomId);
        if (room != null && room.isEmpty()) {
            rooms.remove(roomId);
        }
    }

    /** Lista todas as salas como DTOs para o lobby. */
    public List<RoomInfo> listRooms() {
        List<RoomInfo> list = new ArrayList<>();
        for (Room room : rooms.values()) {
            list.add(room.toInfo());
        }
        return list;
    }
}
