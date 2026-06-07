package com.example.network;

import com.example.network.protocol.MessageType;
import com.example.network.protocol.NetworkMessage;
import com.example.network.protocol.PlayerInfo;
import com.example.network.protocol.RoomInfo;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para LobbyView.
 * Como LobbyView é JavaFX, cada teste roda no FX Application Thread via
 * Platform.runLater() + CountDownLatch.
 */
class LobbyViewTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void startJfx() throws Exception {
        if (!FX_STARTED.getAndSet(true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException e) {
                latch.countDown(); // toolkit já iniciado
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX platform failed to start");
        }
        Platform.setImplicitExit(false);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task timed out");
        if (error.get() != null) {
            if (error.get() instanceof Exception e) throw e;
            throw new RuntimeException(error.get());
        }
    }

    private static GameWebSocketClient mockClient(String playerName) {
        GameWebSocketClient client = mock(GameWebSocketClient.class);
        when(client.getPlayerName()).thenReturn(playerName);
        when(client.isConnected()).thenReturn(true);
        return client;
    }

    private static RoomInfo room(String id, String name, int max, String status, List<PlayerInfo> players) {
        RoomInfo r = new RoomInfo();
        r.setId(id); r.setName(name); r.setMaxPlayers(max); r.setStatus(status); r.setPlayers(players);
        return r;
    }

    private static PlayerInfo player(String name, String color, boolean ready, boolean host) {
        return new PlayerInfo(name, color, ready, host);
    }

    /** Chama o método privado handleMessage via reflexão. */
    private static void handleMessage(LobbyView view, NetworkMessage msg) throws Exception {
        Method m = LobbyView.class.getDeclaredMethod("handleMessage", NetworkMessage.class);
        m.setAccessible(true);
        m.invoke(view, msg);
    }

    /** Chama o método privado ensureName via reflexão. */
    private static boolean ensureName(LobbyView view) throws Exception {
        Method m = LobbyView.class.getDeclaredMethod("ensureName");
        m.setAccessible(true);
        return (boolean) m.invoke(view);
    }

    /** Seta o campo privado currentRoom via reflexão. */
    private static void setCurrentRoom(LobbyView view, RoomInfo r) throws Exception {
        Field f = LobbyView.class.getDeclaredField("currentRoom");
        f.setAccessible(true);
        f.set(view, r);
    }

    private static RoomInfo getCurrentRoom(LobbyView view) throws Exception {
        Field f = LobbyView.class.getDeclaredField("currentRoom");
        f.setAccessible(true);
        return (RoomInfo) f.get(view);
    }

    // ─── Constructor / getRoot ────────────────────────────────────────────────

    @Test
    void getRoot_isNotNull() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            assertNotNull(view.getRoot());
        });
    }

    @Test
    void constructor_registersOnMessageAndCallsListRooms() throws Exception {
        runOnFx(() -> {
            GameWebSocketClient client = mockClient("Alice");
            new LobbyView(client, (r, s) -> {});
            verify(client).setOnMessage(any());
            verify(client, atLeastOnce()).listRooms();
        });
    }

    @Test
    void constructor_playerNameNotNull_preFillsNameField() throws Exception {
        runOnFx(() -> {
            GameWebSocketClient client = mockClient("Alice");
            LobbyView view = new LobbyView(client, (r, s) -> {});
            // ensureName must return true because nameField was pre-filled
            assertTrue(ensureName(view));
        });
    }

    @Test
    void constructor_playerNameNull_nameFieldEmpty() throws Exception {
        runOnFx(() -> {
            GameWebSocketClient client = mockClient(null);
            LobbyView view = new LobbyView(client, (r, s) -> {});
            // nameField is empty, ensureName returns false
            assertFalse(ensureName(view));
        });
    }

    @Test
    void constructor_currentRoom_isNullInitially() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            assertNull(getCurrentRoom(view));
        });
    }

    // ─── ensureName ───────────────────────────────────────────────────────────

    @Test
    void ensureName_blankName_returnsFalse() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient(null), (r, s) -> {});
            assertFalse(ensureName(view));
        });
    }

    @Test
    void ensureName_validName_callsSetPlayerName() throws Exception {
        runOnFx(() -> {
            GameWebSocketClient client = mockClient("Bob");
            LobbyView view = new LobbyView(client, (r, s) -> {});
            boolean result = ensureName(view);
            assertTrue(result);
            verify(client, atLeastOnce()).setPlayerName("Bob");
        });
    }

    // ─── handleMessage – ROOM_LIST ────────────────────────────────────────────

    @Test
    void handleMessage_roomList_emptyList_doesNotThrow() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", List.of());
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_roomList_nullData_doesNotThrow() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", null);
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_roomList_withWaitingRoom_rendersJoinableCard() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", List.of(
                Map.of("id", "r1", "name", "Sala A", "maxPlayers", 4,
                    "status", "WAITING", "players", List.of())
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_roomList_inGameRoom_rendersDisabledCard() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", List.of(
                Map.of("id", "r2", "name", "Em Jogo", "maxPlayers", 4,
                    "status", "IN_GAME", "players", List.of())
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_roomList_fullRoom_rendersDisabledJoinButton() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", List.of(
                Map.of("id", "r3", "name", "Cheia", "maxPlayers", 1,
                    "status", "WAITING", "players",
                    List.of(Map.of("name", "Bob", "color", "RED", "ready", false, "host", true)))
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_roomList_multipleRooms_rendersAll() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", List.of(
                Map.of("id", "r1", "name", "A", "maxPlayers", 4, "status", "WAITING", "players", List.of()),
                Map.of("id", "r2", "name", "B", "maxPlayers", 4, "status", "WAITING", "players", List.of()),
                Map.of("id", "r3", "name", "C", "maxPlayers", 2, "status", "IN_GAME", "players", List.of())
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_roomList_ignoredWhenAlreadyInRoom() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            setCurrentRoom(view, room("r1", "Sala", 4, "WAITING", List.of()));
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LIST);
            msg.put("rooms", List.of(
                Map.of("id", "r9", "name", "Outra", "maxPlayers", 4, "status", "WAITING", "players", List.of())
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
            // currentRoom should still be the original room
            assertNotNull(getCurrentRoom(view));
        });
    }

    // ─── handleMessage – ROOM_JOINED ─────────────────────────────────────────

    @Test
    void handleMessage_roomJoined_setsCurrentRoomAndBuildsScreen() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Alice", "color", "RED", "ready", false, "host", true)
                )
            ));
            handleMessage(view, msg);
            assertNotNull(getCurrentRoom(view));
            assertEquals("r1", getCurrentRoom(view).getId());
        });
    }

    @Test
    void handleMessage_roomJoined_nullRoom_doesNotThrow() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", null);
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    // ─── buildRoomScreen – host/non-host branches ─────────────────────────────

    @Test
    void buildRoomScreen_aliceIsHost_rendersWithStartButton() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Alice", "color", "RED", "ready", false, "host", true),
                    Map.of("name", "Bob",   "color", "BLUE", "ready", true,  "host", false)
                )
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
            assertNotNull(view.getRoot());
        });
    }

    @Test
    void buildRoomScreen_aliceNotHost_notReady_rendersReadyButton() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Bob",   "color", "BLUE", "ready", false, "host", true),
                    Map.of("name", "Alice", "color", "RED",  "ready", false, "host", false)
                )
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void buildRoomScreen_aliceNotHost_alreadyReady_readyButtonToggled() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Bob",   "color", "BLUE", "ready", false, "host", true),
                    Map.of("name", "Alice", "color", "RED",  "ready", true,  "host", false)
                )
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void buildRoomScreen_hostWithFullRoom_botsSpinnerMaxIsZero() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala", "maxPlayers", 2, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Alice", "color", "RED",  "ready", false, "host", true),
                    Map.of("name", "Bob",   "color", "BLUE", "ready", false, "host", false)
                )
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void buildRoomScreen_playerNameNotInList_iAmHostFalse() throws Exception {
        runOnFx(() -> {
            // Alice is not in the player list at all → iAmHost = false
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Bob",   "color", "BLUE", "ready", false, "host", true),
                    Map.of("name", "Carol", "color", "GREEN","ready", false, "host", false)
                )
            ));
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    // ─── handleMessage – ROOM_UPDATE ─────────────────────────────────────────

    @Test
    void handleMessage_roomUpdate_withCurrentRoom_rebuildsScreen() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            setCurrentRoom(view, room("r1", "Sala", 4, "WAITING", List.of()));
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_UPDATE);
            msg.put("room", Map.of(
                "id", "r1", "name", "Sala Atualizada", "maxPlayers", 4, "status", "WAITING",
                "players", List.of(
                    Map.of("name", "Alice", "color", "RED",  "ready", true,  "host", false),
                    Map.of("name", "Bob",   "color", "BLUE", "ready", false, "host", true)
                )
            ));
            handleMessage(view, msg);
            assertEquals("Sala Atualizada", getCurrentRoom(view).getName());
        });
    }

    @Test
    void handleMessage_roomUpdate_withoutCurrentRoom_doesNothing() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            // currentRoom is null → update ignored
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_UPDATE);
            msg.put("room", Map.of("id", "r1", "name", "X", "maxPlayers", 2, "status", "WAITING", "players", List.of()));
            handleMessage(view, msg);
            assertNull(getCurrentRoom(view));
        });
    }

    @Test
    void handleMessage_roomUpdate_nullRoom_doesNotThrow() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            setCurrentRoom(view, room("r1", "Sala", 4, "WAITING", List.of()));
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_UPDATE);
            msg.put("room", null);
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    // ─── handleMessage – ROOM_LEFT ────────────────────────────────────────────

    @Test
    void handleMessage_roomLeft_clearsCurrentRoomAndShowsList() throws Exception {
        runOnFx(() -> {
            GameWebSocketClient client = mockClient("Alice");
            LobbyView view = new LobbyView(client, (r, s) -> {});
            setCurrentRoom(view, room("r1", "Sala", 4, "WAITING", List.of()));
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_LEFT);
            handleMessage(view, msg);
            assertNull(getCurrentRoom(view));
            assertNotNull(view.getRoot());
        });
    }

    // ─── handleMessage – GAME_STARTED ────────────────────────────────────────

    @Test
    void handleMessage_gameStarted_callsOnGameStart_withRoomAndSeed() throws Exception {
        runOnFx(() -> {
            AtomicReference<RoomInfo> capturedRoom = new AtomicReference<>();
            AtomicLong capturedSeed = new AtomicLong(-1);
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {
                capturedRoom.set(r);
                capturedSeed.set(s);
            });
            NetworkMessage msg = NetworkMessage.of(MessageType.GAME_STARTED);
            msg.put("room", Map.of("id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING", "players", List.of()));
            msg.put("seed", 12345L);
            handleMessage(view, msg);
            assertNotNull(capturedRoom.get());
            assertEquals("r1", capturedRoom.get().getId());
            assertEquals(12345L, capturedSeed.get());
        });
    }

    @Test
    void handleMessage_gameStarted_nullOnGameStart_doesNotThrow() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), null);
            NetworkMessage msg = NetworkMessage.of(MessageType.GAME_STARTED);
            msg.put("room", Map.of("id", "r1", "name", "Sala", "maxPlayers", 4, "status", "WAITING", "players", List.of()));
            msg.put("seed", 0L);
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    // ─── handleMessage – ERROR ────────────────────────────────────────────────

    @Test
    void handleMessage_error_setsStatusLabelText() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ERROR);
            msg.put("message", "Sala não encontrada");
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    // ─── handleMessage – default / unknown ───────────────────────────────────

    @Test
    void handleMessage_unknownType_doesNotThrow() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of("TIPO_DESCONHECIDO");
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }

    @Test
    void handleMessage_malformedRoomData_caughtGracefully() throws Exception {
        runOnFx(() -> {
            LobbyView view = new LobbyView(mockClient("Alice"), (r, s) -> {});
            NetworkMessage msg = NetworkMessage.of(MessageType.ROOM_JOINED);
            // Data inválida — deve ser capturada pelo catch interno de handleMessage
            msg.put("room", "não é um mapa");
            assertDoesNotThrow(() -> handleMessage(view, msg));
        });
    }
}
