package com.example.network.room;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomManagerTest {
    @Test
    void testCRUD() {
        RoomManager rm = new RoomManager();
        Room r = rm.createRoom("Teste", 4);
        assertNotNull(rm.getRoom(r.getId()));
        assertEquals(1, rm.listRooms().size());
        
        rm.removeIfEmpty(r.getId());
        assertEquals(0, rm.listRooms().size());
    }

    @Test
    void testGetInexistente() {
        RoomManager rm = new RoomManager();
        assertNull(rm.getRoom("id-fantasma"));
    }
}