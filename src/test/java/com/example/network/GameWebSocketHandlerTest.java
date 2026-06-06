package com.example.network;

import com.example.network.protocol.MessageType;
import com.example.network.protocol.NetworkMessage;
import com.example.network.room.RoomManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameWebSocketHandlerTest {

    // ---- Fake Session ----
    static class FakeSession implements WebSocketSession {
        private final String id;
        private final Map<String, Object> attributes = new HashMap<>();
        private final List<String> sent = new ArrayList<>();
        private boolean open = true;

        FakeSession(String id) { this.id = id; }

        /** Última mensagem recebida */
        String last() { return sent.isEmpty() ? null : sent.get(sent.size() - 1); }

        /** Todas as mensagens recebidas */
        List<String> all() { return sent; }

        /**
         * Retorna a primeira mensagem que contém o tipo informado.
         * Útil porque broadcastRoomList() sobrescreve last() com ROOM_LIST.
         */
        String firstWith(String type) {
            return sent.stream().filter(m -> m.contains(type)).findFirst().orElse(null);
        }

        boolean anyContains(String text) {
            return sent.stream().anyMatch(m -> m.contains(text));
        }

        @Override public String getId() { return id; }
        @Override public URI getUri() { return null; }
        @Override public HttpHeaders getHandshakeHeaders() { return new HttpHeaders(); }
        @Override public Map<String, Object> getAttributes() { return attributes; }
        @Override public Principal getPrincipal() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return null; }
        @Override public InetSocketAddress getRemoteAddress() { return null; }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int l) {}
        @Override public int getTextMessageSizeLimit() { return 0; }
        @Override public void setBinaryMessageSizeLimit(int l) {}
        @Override public int getBinaryMessageSizeLimit() { return 0; }
        @Override public List<WebSocketExtension> getExtensions() { return List.of(); }
        @Override public void sendMessage(WebSocketMessage<?> message) throws IOException {
            sent.add(String.valueOf(message.getPayload()));
        }
        @Override public boolean isOpen() { return open; }
        @Override public void close() throws IOException { open = false; }
        @Override public void close(CloseStatus status) throws IOException { open = false; }
    }

    // ---- Setup ----
    private GameWebSocketHandler handler;
    private RoomManager roomManager;
    private ObjectMapper mapper;
    private FakeSession s1;
    private FakeSession s2;

    @BeforeEach
    void setup() throws Exception {
        roomManager = new RoomManager();
        handler = new GameWebSocketHandler(roomManager);
        mapper = new ObjectMapper();
        s1 = new FakeSession("s1");
        s2 = new FakeSession("s2");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);
    }

    private void send(FakeSession session, NetworkMessage msg) throws Exception {
        handler.handleTextMessage(session, new TextMessage(mapper.writeValueAsString(msg)));
    }

    // ---- Testes de conexão ----

    @Test
    void testConectar_naoLancaExcecao() {
        FakeSession s3 = new FakeSession("s3");
        assertDoesNotThrow(() -> handler.afterConnectionEstablished(s3));
    }

    @Test
    void testDesconectar_semSala_naoLancaExcecao() {
        assertDoesNotThrow(() -> handler.afterConnectionClosed(s1, CloseStatus.NORMAL));
    }

    // ---- Testes de validação de mensagem ----

    @Test
    void testJsonInvalido_retornaErro() throws Exception {
        handler.handleTextMessage(s1, new TextMessage("{ bad json }"));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    @Test
    void testSemType_retornaErro() throws Exception {
        handler.handleTextMessage(s1, new TextMessage("{\"data\":{}}"));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    @Test
    void testTipoDesconhecido_retornaErro() throws Exception {
        send(s1, NetworkMessage.of("TIPO_INVALIDO"));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    // ---- LIST_ROOMS ----

    @Test
    void testListRooms_retornaRoomList() throws Exception {
        send(s1, NetworkMessage.of(MessageType.LIST_ROOMS));
        assertTrue(s1.anyContains(MessageType.ROOM_LIST));
    }

    // ---- CREATE_ROOM ----

    @Test
    void testCreateRoom_semNome_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .put("roomName", "Sala").put("maxPlayers", 4));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    @Test
    void testCreateRoom_nomeVazio_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("   ").put("roomName", "Sala").put("maxPlayers", 4));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    @Test
    void testCreateRoom_valido_retornaRoomJoined() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala da Alice").put("maxPlayers", 4));
        // broadcastRoomList() envia ROOM_LIST depois, por isso usamos anyContains
        assertTrue(s1.anyContains(MessageType.ROOM_JOINED));
        assertNotNull(s1.getAttributes().get("roomId"));
        assertEquals("Alice", s1.getAttributes().get("playerName"));
    }

    @Test
    void testCreateRoom_comCorEspecifica_retornaRoomJoined() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4).put("color", "BLUE"));
        assertTrue(s1.anyContains(MessageType.ROOM_JOINED));
    }

    // ---- JOIN_ROOM ----

    @Test
    void testJoinRoom_salaInexistente_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.JOIN_ROOM)
                .sender("Bob").room("sala-fantasma"));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    @Test
    void testJoinRoom_valido_retornaRoomJoined() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM)
                .sender("Bob").room(roomId));
        assertTrue(s2.anyContains(MessageType.ROOM_JOINED));
        assertEquals(roomId, s2.getAttributes().get("roomId"));
    }

    @Test
    void testJoinRoom_corOcupada_escolheOutra() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4).put("color", "RED"));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM)
                .sender("Bob").room(roomId).put("color", "RED"));
        assertTrue(s2.anyContains(MessageType.ROOM_JOINED));
    }

    @Test
    void testJoinRoom_semNome_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM).room(roomId));
        assertTrue(s2.anyContains(MessageType.ERROR));
    }

    // ---- LEAVE_ROOM ----

    @Test
    void testLeaveRoom_retornaRoomLeft() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        s1.all().clear(); // limpa mensagens anteriores para facilitar verificação

        send(s1, NetworkMessage.of(MessageType.LEAVE_ROOM));
        assertTrue(s1.anyContains(MessageType.ROOM_LEFT));
        assertNull(s1.getAttributes().get("roomId"));
    }

    @Test
    void testLeaveRoom_ultimoJogador_removeSala() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s1, NetworkMessage.of(MessageType.LEAVE_ROOM));
        assertNull(roomManager.getRoom(roomId));
    }

    @Test
    void testLeaveRoom_comOutroJogador_salaPermaneceEAtualiza() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM).sender("Bob").room(roomId));
        s2.all().clear(); // limpa mensagens anteriores

        send(s2, NetworkMessage.of(MessageType.LEAVE_ROOM));
        assertTrue(s2.anyContains(MessageType.ROOM_LEFT));
        assertNotNull(roomManager.getRoom(roomId));
    }

    @Test
    void testLeaveRoom_semSala_naoLancaExcecao() {
        assertDoesNotThrow(() -> send(s1, NetworkMessage.of(MessageType.LEAVE_ROOM)));
    }

    // ---- SET_READY ----

    @Test
    void testSetReady_semSala_naoLancaExcecao() {
        assertDoesNotThrow(() -> send(s1, NetworkMessage.of(MessageType.SET_READY).put("ready", true)));
    }

    @Test
    void testSetReady_comSala_atualizaEstado() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        assertDoesNotThrow(() -> send(s1, NetworkMessage.of(MessageType.SET_READY).put("ready", true)));
    }

    // ---- START_GAME ----

    @Test
    void testStartGame_semSala_naoLancaExcecao() {
        assertDoesNotThrow(() -> send(s1, NetworkMessage.of(MessageType.START_GAME)));
    }

    @Test
    void testStartGame_naoEHost_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM).sender("Bob").room(roomId));
        s2.all().clear();

        send(s2, NetworkMessage.of(MessageType.START_GAME));
        assertTrue(s2.anyContains(MessageType.ERROR));
    }

    @Test
    void testStartGame_jogadoresNaoProntos_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM).sender("Bob").room(roomId));
        s1.all().clear();

        send(s1, NetworkMessage.of(MessageType.START_GAME));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    // ---- GAME_ACTION ----

    @Test
    void testGameAction_semPartida_retornaErro() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        s1.all().clear();

        send(s1, NetworkMessage.of(MessageType.GAME_ACTION).put("action", "ROLL_DICE"));
        assertTrue(s1.anyContains(MessageType.ERROR));
    }

    @Test
    void testGameAction_semSala_naoLancaExcecao() {
        assertDoesNotThrow(() -> send(s1, NetworkMessage.of(MessageType.GAME_ACTION).put("action", "ROLL_DICE")));
    }

    // ---- Desconexão com sala ----

    @Test
    void testDesconectar_comSala_removeSala() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        handler.afterConnectionClosed(s1, CloseStatus.NORMAL);
        assertNull(roomManager.getRoom(roomId));
    }

    @Test
    void testDesconectar_comDoisJogadores_salaPermaneceComUm() throws Exception {
        send(s1, NetworkMessage.of(MessageType.CREATE_ROOM)
                .sender("Alice").put("roomName", "Sala").put("maxPlayers", 4));
        String roomId = s1.getAttributes().get("roomId").toString();

        send(s2, NetworkMessage.of(MessageType.JOIN_ROOM).sender("Bob").room(roomId));

        handler.afterConnectionClosed(s2, CloseStatus.NORMAL);
        assertNotNull(roomManager.getRoom(roomId));
    }
}
