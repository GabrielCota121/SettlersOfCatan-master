package com.example.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameWebSocketClientTest {

    private GameWebSocketClient client;

    @BeforeEach
    void setUp() {
        client = new GameWebSocketClient();
    }

    @Test
    void testIsConnected_semConexao_retornaFalse() {
        assertFalse(client.isConnected());
    }

    @Test
    void testSetPlayerName_eGetPlayerName() {
        client.setPlayerName("Alice");
        assertEquals("Alice", client.getPlayerName());
    }

    @Test
    void testSetCurrentRoomId_eGetCurrentRoomId() {
        client.setCurrentRoomId("room-42");
        assertEquals("room-42", client.getCurrentRoomId());
    }

    @Test
    void testSetOnMessage_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.setOnMessage(msg -> {}));
    }

    @Test
    void testSetOnDisconnected_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.setOnDisconnected(() -> {}));
    }

    @Test
    void testConnect_urlInvalida_retornaFalse() {
        boolean result = client.connect("ws://localhost:9999/invalido");
        assertFalse(result);
    }

    @Test
    void testSend_semConexao_naoLancaExcecao() {
        com.example.network.protocol.NetworkMessage msg =
            com.example.network.protocol.NetworkMessage.of("TEST");
        assertDoesNotThrow(() -> client.send(msg));
    }

    @Test
    void testDisconnect_semConexao_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.disconnect());
    }

    @Test
    void testListRooms_semConexao_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.listRooms());
    }

    @Test
    void testLeaveRoom_semConexao_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.leaveRoom());
    }

    @Test
    void testStartGame_semConexao_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.startGame());
    }

    @Test
    void testStartGameComBots_semConexao_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.startGame(2));
    }

    @Test
    void testSendIntent_semConexao_naoLancaExcecao() {
        assertDoesNotThrow(() -> client.sendIntent("ROLL_DICE", null));
    }
}
