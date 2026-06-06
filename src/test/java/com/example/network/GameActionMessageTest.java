package com.example.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameActionMessageTest {

    @Test
    void testConstrutorPadrao() {
        GameActionMessage msg = new GameActionMessage();
        assertNull(msg.getAction());
        assertNull(msg.getPlayerName());
        assertNull(msg.getTargetId());
    }

    @Test
    void testConstrutorComParametros() {
        GameActionMessage msg = new GameActionMessage("ROLL_DICE", "Alice", 42);
        assertEquals("ROLL_DICE", msg.getAction());
        assertEquals("Alice", msg.getPlayerName());
        assertEquals(42, msg.getTargetId());
    }

    @Test
    void testSettersEGetters() {
        GameActionMessage msg = new GameActionMessage();
        msg.setAction("BUILD_ROAD");
        msg.setPlayerName("Bob");
        msg.setTargetId(7);

        assertEquals("BUILD_ROAD", msg.getAction());
        assertEquals("Bob", msg.getPlayerName());
        assertEquals(7, msg.getTargetId());
    }

    @Test
    void testTargetIdNulo() {
        GameActionMessage msg = new GameActionMessage("END_TURN", "Carlos", null);
        assertNull(msg.getTargetId());
    }

    @Test
    void testAcoesValidas() {
        String[] acoes = {"ROLL_DICE", "BUILD_ROAD", "BUILD_SETTLEMENT", "END_TURN"};
        for (String acao : acoes) {
            GameActionMessage msg = new GameActionMessage(acao, "Player", 1);
            assertEquals(acao, msg.getAction());
        }
    }
}
