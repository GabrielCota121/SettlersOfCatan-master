package com.example.network;

import com.example.network.protocol.PlayerInfo;
import com.example.network.protocol.RoomInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa a lógica de deserialização JSON usada por LobbyView
 * (via reflexão no MAPPER privado estático) e os modelos RoomInfo/PlayerInfo.
 * LobbyView é JavaFX e não pode ser instanciada sem toolkit — apenas a
 * lógica pura de conversão de dados é testada aqui.
 */
class LobbyViewJsonTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        Field mapperField = LobbyView.class.getDeclaredField("MAPPER");
        mapperField.setAccessible(true);
        mapper = (ObjectMapper) mapperField.get(null);
    }

    // ─── convertRoom ──────────────────────────────────────────────────────────

    @Test
    void convertRoom_null_returnsNull() {
        RoomInfo result = mapper.convertValue(null, RoomInfo.class);
        assertNull(result);
    }

    @Test
    void convertRoom_validMap_returnsRoomInfo() {
        Map<String, Object> raw = Map.of(
            "id", "room-1",
            "name", "Sala do Catan",
            "maxPlayers", 4,
            "status", "WAITING"
        );
        RoomInfo room = mapper.convertValue(raw, RoomInfo.class);
        assertNotNull(room);
        assertEquals("room-1", room.getId());
        assertEquals("Sala do Catan", room.getName());
        assertEquals(4, room.getMaxPlayers());
        assertEquals("WAITING", room.getStatus());
    }

    @Test
    void convertRoom_withPlayers_deserializesPlayerList() {
        List<Map<String, Object>> players = List.of(
            Map.of("name", "Alice", "color", "RED", "ready", true, "host", true),
            Map.of("name", "Bob",   "color", "BLUE", "ready", false, "host", false)
        );
        Map<String, Object> raw = Map.of(
            "id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING",
            "players", players
        );
        RoomInfo room = mapper.convertValue(raw, RoomInfo.class);
        assertEquals(2, room.getPlayers().size());
        assertEquals("Alice", room.getPlayers().get(0).getName());
        assertTrue(room.getPlayers().get(0).isHost());
        assertEquals("Bob", room.getPlayers().get(1).getName());
        assertFalse(room.getPlayers().get(1).isReady());
    }

    @Test
    void convertRoom_unknownProperties_areIgnored() {
        Map<String, Object> raw = Map.of(
            "id", "r1", "name", "X", "maxPlayers", 2, "status", "WAITING",
            "unknownField", "ignored"
        );
        assertDoesNotThrow(() -> mapper.convertValue(raw, RoomInfo.class));
    }

    @Test
    void convertRoom_missingOptionalFields_defaultsApply() {
        Map<String, Object> raw = Map.of("id", "r1", "name", "X", "maxPlayers", 2);
        RoomInfo room = mapper.convertValue(raw, RoomInfo.class);
        assertNull(room.getStatus());
        assertNotNull(room.getPlayers());
        assertTrue(room.getPlayers().isEmpty());
    }

    // ─── convertRoomList ──────────────────────────────────────────────────────

    @Test
    void convertRoomList_null_returnsEmptyList() {
        // Replicate the null-guard in LobbyView.convertRoomList
        Object raw = null;
        List<RoomInfo> result = raw == null ? List.of() :
            mapper.convertValue(raw,
                mapper.getTypeFactory().constructCollectionType(List.class, RoomInfo.class));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertRoomList_emptyList_returnsEmptyList() {
        List<Object> raw = new ArrayList<>();
        List<RoomInfo> result = mapper.convertValue(raw,
            mapper.getTypeFactory().constructCollectionType(List.class, RoomInfo.class));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertRoomList_twoRooms_returnsBothRooms() {
        List<Map<String, Object>> raw = List.of(
            Map.of("id", "r1", "name", "Sala A", "maxPlayers", 2, "status", "WAITING"),
            Map.of("id", "r2", "name", "Sala B", "maxPlayers", 4, "status", "IN_GAME")
        );
        List<RoomInfo> rooms = mapper.convertValue(raw,
            mapper.getTypeFactory().constructCollectionType(List.class, RoomInfo.class));
        assertEquals(2, rooms.size());
        assertEquals("r1", rooms.get(0).getId());
        assertEquals("r2", rooms.get(1).getId());
        assertEquals("IN_GAME", rooms.get(1).getStatus());
    }

    @Test
    void convertRoomList_roomsWithPlayers_deserializesCorrectly() {
        List<Map<String, Object>> raw = List.of(
            Map.of("id", "r1", "name", "X", "maxPlayers", 2, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Alice", "color", "RED", "ready", false, "host", true)
                ))
        );
        List<RoomInfo> rooms = mapper.convertValue(raw,
            mapper.getTypeFactory().constructCollectionType(List.class, RoomInfo.class));
        assertEquals(1, rooms.size());
        assertEquals(1, rooms.get(0).getPlayers().size());
        assertEquals("Alice", rooms.get(0).getPlayers().get(0).getName());
    }

    // ─── RoomInfo model ───────────────────────────────────────────────────────

    @Test
    void roomInfo_defaultConstructor_emptyPlayers() {
        RoomInfo room = new RoomInfo();
        assertNotNull(room.getPlayers());
        assertTrue(room.getPlayers().isEmpty());
    }

    @Test
    void roomInfo_getPlayerCount_reflectsPlayersList() {
        RoomInfo room = new RoomInfo();
        assertEquals(0, room.getPlayerCount());
        room.setPlayers(List.of(new PlayerInfo("Alice", "RED", false, true)));
        assertEquals(1, room.getPlayerCount());
    }

    @Test
    void roomInfo_setPlayers_null_usesEmptyList() {
        RoomInfo room = new RoomInfo();
        room.setPlayers(null);
        assertNotNull(room.getPlayers());
        assertTrue(room.getPlayers().isEmpty());
    }

    @Test
    void roomInfo_settersAndGetters() {
        RoomInfo room = new RoomInfo();
        room.setId("abc");
        room.setName("Minha Sala");
        room.setMaxPlayers(3);
        room.setStatus("WAITING");

        assertEquals("abc", room.getId());
        assertEquals("Minha Sala", room.getName());
        assertEquals(3, room.getMaxPlayers());
        assertEquals("WAITING", room.getStatus());
    }

    // ─── PlayerInfo model ─────────────────────────────────────────────────────

    @Test
    void playerInfo_defaultConstructor() {
        PlayerInfo p = new PlayerInfo();
        assertNull(p.getName());
        assertNull(p.getColor());
        assertFalse(p.isReady());
        assertFalse(p.isHost());
    }

    @Test
    void playerInfo_fullConstructor() {
        PlayerInfo p = new PlayerInfo("Alice", "RED", true, true);
        assertEquals("Alice", p.getName());
        assertEquals("RED", p.getColor());
        assertTrue(p.isReady());
        assertTrue(p.isHost());
    }

    @Test
    void playerInfo_settersAndGetters() {
        PlayerInfo p = new PlayerInfo();
        p.setName("Bob");
        p.setColor("BLUE");
        p.setReady(true);
        p.setHost(false);

        assertEquals("Bob", p.getName());
        assertEquals("BLUE", p.getColor());
        assertTrue(p.isReady());
        assertFalse(p.isHost());
    }

    @Test
    void playerInfo_notReadyByDefault_canBeSetReady() {
        PlayerInfo p = new PlayerInfo("Carol", "GREEN", false, false);
        assertFalse(p.isReady());
        p.setReady(true);
        assertTrue(p.isReady());
    }

    @Test
    void playerInfo_notHostByDefault_canBeSetHost() {
        PlayerInfo p = new PlayerInfo();
        assertFalse(p.isHost());
        p.setHost(true);
        assertTrue(p.isHost());
    }

    // ─── RoomInfo / PlayerInfo round-trip via JSON ────────────────────────────

    @Test
    void roomInfo_roundTripJson_preservesAllFields() throws Exception {
        RoomInfo room = new RoomInfo();
        room.setId("r99");
        room.setName("Partida Final");
        room.setMaxPlayers(4);
        room.setStatus("WAITING");
        room.setPlayers(List.of(new PlayerInfo("Eve", "ORANGE", true, false)));

        String json = mapper.writeValueAsString(room);
        RoomInfo parsed = mapper.readValue(json, RoomInfo.class);

        assertEquals("r99", parsed.getId());
        assertEquals("Partida Final", parsed.getName());
        assertEquals(4, parsed.getMaxPlayers());
        assertEquals("WAITING", parsed.getStatus());
        assertEquals(1, parsed.getPlayers().size());
        assertEquals("Eve", parsed.getPlayers().get(0).getName());
    }

    @Test
    void playerInfo_roundTripJson_preservesAllFields() throws Exception {
        PlayerInfo p = new PlayerInfo("Dave", "PURPLE", true, true);
        String json = mapper.writeValueAsString(p);
        PlayerInfo parsed = mapper.readValue(json, PlayerInfo.class);

        assertEquals("Dave", parsed.getName());
        assertEquals("PURPLE", parsed.getColor());
        assertTrue(parsed.isReady());
        assertTrue(parsed.isHost());
    }
}
