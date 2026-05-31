package com.example.network.room;

import com.example.model.board.*;
import com.example.model.building.City;
import com.example.model.cards.IDevelopmentCard;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.player.Player;
import com.example.model.state.ITurnState;
import com.example.network.protocol.BuildingDTO;
import com.example.network.protocol.GameStateDTO;
import com.example.network.protocol.PlayerStateDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Partida autoritativa de uma sala. Encapsula o {@link CatanGameManager} que
 * roda <b>somente no servidor</b>: os clientes enviam intenções (ações) e este
 * objeto valida pelas regras, aplica e produz o snapshot ({@link GameStateDTO})
 * que é devolvido a todos.
 *
 * <p>Como só o servidor rola dados e compra cartas, não há divergência de RNG
 * entre clientes. O tabuleiro é determinístico via {@link #getSeed() seed}.
 *
 * <p>Ações suportadas nesta fase: ROLL_DICE, END_TURN, BUY_DEV_CARD,
 * BUILD_SETTLEMENT, BUILD_ROAD, BUILD_CITY (cobrem setup + fase principal).
 * Ladrão, descarte, trocas e jogar cartas serão adicionados na próxima etapa.
 */
public class GameSession {

    private final long seed;
    private final CatanGameManager manager;
    private final Map<String, Vertex> vertexById = new HashMap<>();
    private final Map<String, Edge> edgeById = new HashMap<>();

    public GameSession(List<RoomPlayer> roomPlayers, long seed, Consumer<String> logSink) {
        this.seed = seed;

        IGameLogger logger = new IGameLogger() {
            @Override public void log(String message) { logSink.accept(message); }
            @Override public void error(String message) { logSink.accept("[ERRO] " + message); }
        };

        Board board = BoardFactory.createStandardBoard(seed);
        List<Player> players = new ArrayList<>();
        int id = 1;
        for (RoomPlayer rp : roomPlayers) {
            players.add(new Player(id++, rp.getName(), rp.getColor()));
        }

        this.manager = new CatanGameManager(board, players, logger);

        for (Vertex v : board.getVertices()) vertexById.put(v.getId(), v);
        for (Edge e : board.getEdges()) edgeById.put(e.getId(), e);
    }

    public long getSeed() { return seed; }

    /**
     * Aplica uma ação enviada por {@code senderName}. Só o jogador da vez pode
     * agir (anti-trapaça básico). Retorna true se a ação foi aceita.
     */
    public synchronized boolean applyAction(String senderName, String action, String targetId) {
        if (senderName == null || action == null) return false;

        Turn turn = manager.getCurrentTurn();
        ITurnState state = turn.getState();
        Player current = turn.getCurrentPlayer();

        // Anti-trapaça: ignora ações de quem não é o jogador da vez.
        if (!current.getName().equals(senderName)) {
            return false;
        }

        switch (action) {
            case "ROLL_DICE":
                return manager.rollDice(current);
            case "END_TURN":
                return state.endTurn(turn);
            case "BUY_DEV_CARD":
                return state.buyDevelopmentCard(turn);
            case "BUILD_SETTLEMENT": {
                Vertex v = vertexById.get(targetId);
                return v != null && state.buildSettlement(v, turn);
            }
            case "BUILD_CITY": {
                Vertex v = vertexById.get(targetId);
                return v != null && state.buildCity(v, turn);
            }
            case "BUILD_ROAD": {
                Edge e = edgeById.get(targetId);
                return e != null && state.buildRoad(e, turn);
            }
            default:
                return false;
        }
    }

    /**
     * Snapshot público (sem destinatário específico) — todos os recursos visíveis.
     * Usado somente internamente e em testes; em produção use {@link #snapshot(String)}.
     */
    public synchronized GameStateDTO snapshot() {
        return snapshot(null);
    }

    /**
     * Snapshot personalizado: os recursos e cartas de desenvolvimento do {@code viewerName}
     * são enviados em claro; os dos demais jogadores chegam ocultos (só total).
     * Passa {@code null} para expor tudo (testes / logs do servidor).
     */
    public synchronized GameStateDTO snapshot(String viewerName) {
        GameStateDTO dto = new GameStateDTO();
        Turn turn = manager.getCurrentTurn();
        ITurnState state = turn.getState();

        dto.setCurrentPlayerName(turn.getCurrentPlayer().getName());
        dto.setStateName(state.getName());
        dto.setSetupPhase("Setup".equals(state.getName()));
        dto.setCanRollDice(state.canRollDice());
        dto.setCanEndTurn(state.canEndTurn());
        dto.setDice1(manager.getDice1().getResult());
        dto.setDice2(manager.getDice2().getResult());

        Robber robber = manager.getRobber();
        if (robber != null && robber.getCurrentTile() != null) {
            dto.setRobberTileId(robber.getCurrentTile().getId());
        }

        int totalSettlements = 0;
        List<PlayerStateDTO> playerDtos = new ArrayList<>();
        for (Player p : manager.getPlayers()) {
            totalSettlements += p.getNumSettlements();
            PlayerStateDTO ps = new PlayerStateDTO();
            ps.setName(p.getName());
            ps.setColor(p.getColor());
            ps.setVictoryPoints(p.getVictoryPoints());
            ps.setNumKnights(p.getNumKnights());
            ps.setLongestRoad(p.getLongestRoad());
            ps.setNumSettlements(p.getNumSettlements());
            ps.setNumCities(p.getNumCities());
            ps.setNumRoads(p.getNumRoads());

            // Contagens sempre enviadas (usadas na sidebar de todos os clientes).
            int totalRes = 0;
            for (ResourceType t : ResourceType.values()) {
                if (t != ResourceType.DESERT) totalRes += p.getWallet().getResourceAmount(t);
            }
            ps.setNumResources(totalRes);

            int totalCards = 0;
            if (p.getPlayableCards() != null) totalCards += p.getPlayableCards().size();
            if (p.getNewCards()      != null) totalCards += p.getNewCards().size();
            ps.setNumDevCardsTotal(totalCards);

            boolean isViewer = (viewerName == null || viewerName.equals(p.getName()));
            ps.setHiddenResources(!isViewer);

            if (isViewer) {
                // Viewer recebe os recursos reais e os nomes das cartas.
                Map<String, Integer> res = new HashMap<>();
                for (ResourceType type : ResourceType.values()) {
                    if (type != ResourceType.DESERT) res.put(type.name(), p.getWallet().getResourceAmount(type));
                }
                ps.setResources(res);

                List<String> cards = new ArrayList<>();
                if (p.getPlayableCards() != null) for (IDevelopmentCard c : p.getPlayableCards()) cards.add(c.getName());
                if (p.getNewCards()      != null) for (IDevelopmentCard c : p.getNewCards())      cards.add(c.getName());
                ps.setDevCards(cards);
            }
            // Para não-viewers: resources e devCards ficam vazios — o cliente não recebe detalhes.

            if (p.getVictoryPoints() >= 10) dto.setWinnerName(p.getName());
            playerDtos.add(ps);
        }
        dto.setPlayers(playerDtos);
        // Segunda passada do setup: todos já colocaram o primeiro settlement.
        dto.setSetupSecondPass(dto.isSetupPhase() && totalSettlements >= manager.getPlayers().size());

        Board board = manager.getBoard();
        List<BuildingDTO> buildings = new ArrayList<>();
        for (Vertex v : board.getVertices()) {
            if (!v.isEmpty()) {
                Player owner = v.getBuilding().getOwner();
                String type = (v.getBuilding() instanceof City) ? "CITY" : "SETTLEMENT";
                buildings.add(new BuildingDTO(v.getId(), type, owner.getColor(), owner.getName()));
            }
        }
        dto.setBuildings(buildings);

        List<BuildingDTO> roads = new ArrayList<>();
        for (Edge e : board.getEdges()) {
            if (!e.isEmpty()) {
                Player owner = e.getBuilding().getOwner();
                roads.add(new BuildingDTO(e.getId(), "ROAD", owner.getColor(), owner.getName()));
            }
        }
        dto.setRoads(roads);

        Map<String, Integer> bank = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                bank.put(type.name(), manager.getBank().getWallet().getResourceAmount(type));
            }
        }
        dto.setBank(bank);

        return dto;
    }
}
