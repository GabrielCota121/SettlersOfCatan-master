package com.example.network.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkMessageTest {

    @Test
    void ofFactorySetsType() {
        NetworkMessage msg = NetworkMessage.of("TEST_TYPE");
        assertEquals("TEST_TYPE", msg.getType());
    }

    @Test
    void defaultConstructorHasNullFieldsAndEmptyData() {
        NetworkMessage msg = new NetworkMessage();
        assertNull(msg.getType());
        assertNull(msg.getRoomId());
        assertNull(msg.getSenderName());
        assertNotNull(msg.getData());
        assertTrue(msg.getData().isEmpty());
    }

    @Test
    void constructorWithTypeSetsType() {
        NetworkMessage msg = new NetworkMessage("GAME_STATE");
        assertEquals("GAME_STATE", msg.getType());
    }

    @Test
    void builderChainSetsAllFields() {
        NetworkMessage msg = NetworkMessage.of("ROOM_JOINED")
                .room("r-42")
                .sender("Marcelle")
                .put("slot", 3);

        assertEquals("ROOM_JOINED", msg.getType());
        assertEquals("r-42", msg.getRoomId());
        assertEquals("Marcelle", msg.getSenderName());
        assertEquals(3, msg.getData().get("slot"));
    }

    @Test
    void putReturnsTheSameInstanceForChaining() {
        NetworkMessage msg = NetworkMessage.of("T");
        assertSame(msg, msg.put("k", "v"));
    }

    @Test
    void roomReturnsTheSameInstanceForChaining() {
        NetworkMessage msg = NetworkMessage.of("T");
        assertSame(msg, msg.room("r1"));
    }

    @Test
    void senderReturnsTheSameInstanceForChaining() {
        NetworkMessage msg = NetworkMessage.of("T");
        assertSame(msg, msg.sender("Marcelle"));
    }

    @Test
    void getStringReturnsNullForMissingKey() {
        assertNull(NetworkMessage.of("T").getString("missing"));
    }

    @Test
    void getStringReturnsValue() {
        NetworkMessage msg = NetworkMessage.of("T").put("k", "hello");
        assertEquals("hello", msg.getString("k"));
    }

    @Test
    void getStringConvertsNonString() {
        NetworkMessage msg = NetworkMessage.of("T").put("n", 42);
        assertEquals("42", msg.getString("n"));
    }

    @Test
    void getIntReturnsParsedInteger() {
        assertEquals(7, NetworkMessage.of("T").put("n", 7).getInt("n", 0));
    }

    @Test
    void getIntParsesStringNumber() {
        assertEquals(13, NetworkMessage.of("T").put("s", "13").getInt("s", 0));
    }

    @Test
    void getIntReturnsDefaultForMissingKey() {
        assertEquals(-1, NetworkMessage.of("T").getInt("missing", -1));
    }

    @Test
    void getIntReturnsDefaultForUnparseable() {
        assertEquals(0, NetworkMessage.of("T").put("x", "nao-numero").getInt("x", 0));
    }

    @Test
    void getLongReturnsParsedLong() {
        assertEquals(9876543210L, NetworkMessage.of("T").put("big", 9876543210L).getLong("big", 0L));
    }

    @Test
    void getLongParsesStringNumber() {
        assertEquals(12345678901L, NetworkMessage.of("T").put("s", "12345678901").getLong("s", 0L));
    }

    @Test
    void getLongReturnsDefaultForMissingKey() {
        assertEquals(99L, NetworkMessage.of("T").getLong("missing", 99L));
    }

    @Test
    void getBooleanReturnsBooleanValue() {
        assertTrue(NetworkMessage.of("T").put("flag", true).getBoolean("flag", false));
        assertFalse(NetworkMessage.of("T").put("flag", false).getBoolean("flag", true));
    }

    @Test
    void getBooleanParsesStringTrue() {
        assertTrue(NetworkMessage.of("T").put("s", "true").getBoolean("s", false));
    }

    @Test
    void getBooleanParsesStringFalse() {
        assertFalse(NetworkMessage.of("T").put("s", "false").getBoolean("s", true));
    }

    @Test
    void getBooleanReturnsDefaultForMissingKey() {
        assertTrue(NetworkMessage.of("T").getBoolean("missing", true));
        assertFalse(NetworkMessage.of("T").getBoolean("missing", false));
    }

    @Test
    void setDataNullFallsBackToEmptyMap() {
        NetworkMessage msg = new NetworkMessage("T");
        msg.setData(null);
        assertNotNull(msg.getData());
        assertTrue(msg.getData().isEmpty());
    }

    @Test
    void setDataReplacesPayload() {
        NetworkMessage msg = NetworkMessage.of("T").put("old", 1);
        msg.setData(new java.util.HashMap<>());
        assertNull(msg.getData().get("old"));
    }
}
