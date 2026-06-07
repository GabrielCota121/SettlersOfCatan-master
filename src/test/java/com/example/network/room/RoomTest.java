package com.example.network.room;

import com.example.network.protocol.PlayerInfo;
import com.example.network.protocol.RoomInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("r1", "Sala Teste", 3);
    }

    // ─── capacidade e clamp ────────────────────────────────────────────────

    @Test
    void maxPlayersIsClampedToMinimum() {
        Room small = new Room("r2", "Mini", 1);
        assertEquals(2, small.getMaxPlayers());
    }

    @Test
    void maxPlayersIsClampedToMaximum() {
        Room big = new Room("r3", "Grande", 10);
        assertEquals(4, big.getMaxPlayers());
    }

    @Test
    void isEmptyWhenNoPlayers() {
        assertTrue(room.isEmpty());
        assertFalse(room.isFull());
    }

    // ─── addPlayer ────────────────────────────────────────────────────────

    @Test
    void addPlayerUpToCapacity() {
        assertTrue(room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED")));
        assertTrue(room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE")));
        assertTrue(room.addPlayer(new RoomPlayer("s3", "Gabriel", "GREEN")));
        assertTrue(room.isFull());
    }

    @Test
    void addPlayerFailsWhenFull() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE"));
        room.addPlayer(new RoomPlayer("s3", "Gabriel", "GREEN"));
        assertFalse(room.addPlayer(new RoomPlayer("s4", "Dave", "ORANGE")));
    }

    @Test
    void rejectsDuplicateNameCaseInsensitive() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        assertFalse(room.addPlayer(new RoomPlayer("s2", "Marcelle", "BLUE")));
        assertFalse(room.addPlayer(new RoomPlayer("s3", "Marcelle", "GREEN")));
    }

    @Test
    void rejectsPlayerWhenNotWaiting() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.setStatus(RoomStatus.IN_GAME);
        assertFalse(room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE")));
    }

    // ─── removePlayer ─────────────────────────────────────────────────────

    @Test
    void removePlayerReturnsRemovedPlayer() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        RoomPlayer removed = room.removePlayer("s1");
        assertNotNull(removed);
        assertEquals("Marcelle", removed.getName());
        assertTrue(room.isEmpty());
    }

    @Test
    void removeNonexistentPlayerReturnsNull() {
        assertNull(room.removePlayer("ghost"));
    }

    // ─── host ─────────────────────────────────────────────────────────────

    @Test
    void firstPlayerIsHost() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE"));
        assertTrue(room.isHost("s1"));
        assertFalse(room.isHost("s2"));
    }

    @Test
    void hostShiftsAfterFirstPlayerLeaves() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE"));
        room.removePlayer("s1");
        assertEquals("s2", room.getHostSessionId());
        assertTrue(room.isHost("s2"));
    }

    @Test
    void getHostSessionIdNullWhenEmpty() {
        assertNull(room.getHostSessionId());
    }

    // ─── allReady ─────────────────────────────────────────────────────────

    @Test
    void allReadyFailsWithOnlyOnePlayer() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        assertFalse(room.allReady());
    }

    @Test
    void allReadyFalseWhenNonHostNotReady() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE"));
        assertFalse(room.allReady());
    }

    @Test
    void allReadyTrueWhenAllNonHostsReady() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        RoomPlayer Lucas = new RoomPlayer("s2", "Lucas", "BLUE");
        Lucas.setReady(true);
        room.addPlayer(Lucas);
        assertTrue(room.allReady());
    }

    // ─── allHumansReady ───────────────────────────────────────────────────

    @Test
    void allHumansReadyTrueWhenOnlyHostAndBots() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addBots(2);
        assertTrue(room.allHumansReady());
    }

    @Test
    void allHumansReadyFalseWhenHumanNonHostNotReady() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE")); // not ready
        assertFalse(room.allHumansReady());
    }

    // ─── addBots / fillWithBots ───────────────────────────────────────────

    @Test
    void addBotsCreatesBotsWithUniqueColors() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addBots(2);

        List<RoomPlayer> players = room.snapshotPlayers();
        assertEquals(3, players.size());

        long bots = players.stream().filter(RoomPlayer::isBot).count();
        assertEquals(2, bots);

        long distinctColors = players.stream().map(RoomPlayer::getColor).distinct().count();
        assertEquals(3, distinctColors);
    }

    @Test
    void addBotsDoesNotExceedMaxPlayers() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.addBots(10); // pede mais do que a capacidade
        assertTrue(room.isFull());
        assertEquals(room.getMaxPlayers(), room.snapshotPlayers().size());
    }

    @Test
    void botsAreMarkedReadyAutomatically() {
        room.addBots(2);
        room.snapshotPlayers().stream()
                .filter(RoomPlayer::isBot)
                .forEach(b -> assertTrue(b.isReady()));
    }

    @Test
    void fillWithBotsReachesMaxPlayers() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        room.fillWithBots();
        assertTrue(room.isFull());
        assertEquals(room.getMaxPlayers(), room.snapshotPlayers().size());
    }

    // ─── snapshotPlayers ─────────────────────────────────────────────────

    @Test
    void snapshotPlayersIsIndependentCopy() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        List<RoomPlayer> snapshot = room.snapshotPlayers();
        room.addPlayer(new RoomPlayer("s2", "Lucas", "BLUE"));
        assertEquals(1, snapshot.size(), "snapshot não deve refletir mudanças posteriores");
    }

    // ─── toInfo ───────────────────────────────────────────────────────────

    @Test
    void toInfoConvertsToDTOCorrectly() {
        room.addPlayer(new RoomPlayer("s1", "Marcelle", "RED"));
        RoomPlayer Lucas = new RoomPlayer("s2", "Lucas", "BLUE");
        Lucas.setReady(true);
        room.addPlayer(Lucas);

        RoomInfo info = room.toInfo();

        assertEquals("r1", info.getId());
        assertEquals("Sala Teste", info.getName());
        assertEquals(3, info.getMaxPlayers());
        assertEquals("WAITING", info.getStatus());
        assertEquals(2, info.getPlayers().size());

        PlayerInfo MarcelleInfo = info.getPlayers().get(0);
        assertTrue(MarcelleInfo.isHost());
        assertEquals("Marcelle", MarcelleInfo.getName());

        PlayerInfo LucasInfo = info.getPlayers().get(1);
        assertFalse(LucasInfo.isHost());
        assertTrue(LucasInfo.isReady());
    }

    @Test
    void toInfoReflectsStatusChange() {
        room.setStatus(RoomStatus.IN_GAME);
        assertEquals("IN_GAME", room.toInfo().getStatus());
    }
}
