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
    private final Map<String, com.example.model.board.Tile> tileById = new HashMap<>();

    // Estado da negociação em andamento (null = nenhuma)
    private com.example.network.protocol.TradeStatusDTO activeTrade = null;

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
        for (com.example.model.board.Tile t : board.getTiles())
            tileById.put(String.valueOf(t.getId()), t);
    }

    public long getSeed() { return seed; }

    /**
     * Monta um TradeStatusDTO inicial a partir de uma proposta de troca.
     * Todos os não-proponentes começam como PENDING.
     */
    private com.example.network.protocol.TradeStatusDTO buildTradeStatus(String proposerName,
            Map<String, Integer> give, Map<String, Integer> want) {
        com.example.network.protocol.TradeStatusDTO dto =
            new com.example.network.protocol.TradeStatusDTO();
        dto.setProposerName(proposerName);
        dto.setGive(give);
        dto.setWant(want);
        dto.setActive(true);

        Map<String, String> responses = new java.util.LinkedHashMap<>();
        for (com.example.model.player.Player p : manager.getPlayers()) {
            if (!p.getName().equals(proposerName)) {
                responses.put(p.getName(), "PENDING");
            }
        }
        dto.setResponses(responses);
        return dto;
    }

    /**
     * Aplica uma ação enviada por {@code senderName}. Só o jogador da vez pode
     * agir (anti-trapaça básico). Retorna true se a ação foi aceita.
     */
    public synchronized boolean applyAction(String senderName, String action, String targetId) {
        if (senderName == null || action == null) return false;

        Turn turn = manager.getCurrentTurn();
        ITurnState state = turn.getState();
        Player current = turn.getCurrentPlayer();

        boolean isCurrentPlayer = current.getName().equals(senderName);
        boolean isTradeResponse = "TRADE_RESPONSE".equals(action);
        if (!isCurrentPlayer && !isTradeResponse) {
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
            case "PROPOSE_TRADE": {
                // targetId carrega JSON: {"give":{"WOOD":1},"want":{"ORE":2}}
                if (targetId == null) return false;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = om.readValue(targetId, Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> give = (Map<String, Integer>) payload.get("give");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> want = (Map<String, Integer>) payload.get("want");
                    if (give == null || want == null) return false;
                    activeTrade = buildTradeStatus(senderName, give, want);
                    return true;
                } catch (Exception e) {
                    System.out.println("Erro ao deserializar PROPOSE_TRADE: " + e.getMessage());
                    return false;
                }
            }

            case "TRADE_RESPONSE": {
                // Qualquer jogador (exceto o proponente) pode responder
                if (activeTrade == null || !activeTrade.isActive()) return false;
                if (activeTrade.getProposerName().equals(senderName)) return false;
                if (!activeTrade.getResponses().containsKey(senderName)) return false;
                // targetId = "true" ou "false"
                boolean accepts = "true".equalsIgnoreCase(targetId);
                activeTrade.getResponses().put(senderName, accepts ? "ACCEPTED" : "DECLINED");
                return true;
            }

            case "CONFIRM_TRADE": {
                // Só o proponente confirma; targetId = nome do parceiro escolhido
                if (activeTrade == null || !activeTrade.isActive()) return false;
                if (!activeTrade.getProposerName().equals(senderName)) return false;
                String partner = targetId;
                if (partner == null) return false;
                if (!"ACCEPTED".equals(activeTrade.getResponses().get(partner))) return false;

                // Executa a troca no modelo
                com.example.model.player.Player proposer = manager.getPlayers().stream()
                    .filter(p -> p.getName().equals(senderName)).findFirst().orElse(null);
                com.example.model.player.Player partnerPlayer = manager.getPlayers().stream()
                    .filter(p -> p.getName().equals(partner)).findFirst().orElse(null);
                if (proposer == null || partnerPlayer == null) return false;

                // Transfere recursos: proponente dá, recebe
                for (Map.Entry<String, Integer> e : activeTrade.getGive().entrySet()) {
                    try {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        proposer.getWallet().removeResource(rt, e.getValue());
                        partnerPlayer.getWallet().addResource(rt, e.getValue());
                    } catch (IllegalArgumentException ignored) {}
                }
                for (Map.Entry<String, Integer> e : activeTrade.getWant().entrySet()) {
                    try {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        partnerPlayer.getWallet().removeResource(rt, e.getValue());
                        proposer.getWallet().addResource(rt, e.getValue());
                    } catch (IllegalArgumentException ignored) {}
                }

                // Marca todos os pendentes e não-escolhidos como DECLINED
                for (String name : activeTrade.getResponses().keySet()) {
                    if (!name.equals(partner)) {
                        activeTrade.getResponses().put(name, "DECLINED");
                    }
                }
                activeTrade.setActive(false);
                activeTrade.setResolvedWithPlayer(partner);
                return true;
            }

            case "CANCEL_TRADE": {
                if (activeTrade == null) return false;
                if (!activeTrade.getProposerName().equals(senderName)) return false;
                activeTrade.setActive(false);
                activeTrade = null;
                return true;
            }

            case "BANK_TRADE": {
                if (targetId == null) return false;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = om.readValue(targetId, Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> giveMap = (Map<String, Integer>) payload.get("give");
                    String receiveStr = (String) payload.get("receive");
                    if (giveMap == null || receiveStr == null) return false;

                    com.example.model.game.ResourceType receive =
                        com.example.model.game.ResourceType.valueOf(receiveStr);

                    // Valida e executa: remove os recursos dados e adiciona o recebido
                    com.example.model.player.Player player = manager.getPlayers().stream()
                        .filter(p -> p.getName().equals(senderName)).findFirst().orElse(null);
                    if (player == null) return false;

                    for (Map.Entry<String, Integer> e : giveMap.entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        if (player.getWallet().getResourceAmount(rt) < e.getValue()) return false;
                    }
                    for (Map.Entry<String, Integer> e : giveMap.entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        player.getWallet().removeResource(rt, e.getValue());
                        manager.getBank().getWallet().addResource(rt, e.getValue());
                    }
                    player.getWallet().addResource(receive, 1);
                    manager.getBank().getWallet().removeResource(receive, 1);
                    return true;
                } catch (Exception e) {
                    System.out.println("Erro em BANK_TRADE: " + e.getMessage());
                    return false;
                }
            }

            case "MOVE_ROBBER": {
                // TODO: método moveRobber não existe em ITurnState — implementar futuramente
                return false;
            }

            case "STEAL_FROM": {
                // TODO: método stealFrom não existe em ITurnState — implementar futuramente
                return false;
            }

            case "PLAY_KNIGHT": {
                // TODO: método playKnight não existe em ITurnState — implementar futuramente
                return false;
            }

            case "PLAY_MONOPOLY": {
                // TODO: método playMonopoly não existe em ITurnState — implementar futuramente
                return false;
            }

            case "PLAY_YEAR_OF_PLENTY": {
                // TODO: método playYearOfPlenty não existe em ITurnState — implementar futuramente
                return false;
            }

            case "PLAY_ROAD_BUILDING": {
                // TODO: método playRoadBuilding não existe em ITurnState — implementar futuramente
                return false;
            }

            case "SUBMIT_DISCARD": {
                com.example.model.state.ITurnState currentState = manager.getCurrentTurn().getState();
                if (!(currentState instanceof com.example.model.state.WaitingDiscardState discardState)) {
                    return false;
                }
                // targetId carrega o JSON dos recursos a descartar, ex: {"WOOD":2,"ORE":1}
                if (targetId == null) return false;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> rawMap = om.readValue(targetId, Map.class);
                    Map<com.example.model.game.ResourceType, Integer> selection = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : rawMap.entrySet()) {
                        try {
                            selection.put(
                                com.example.model.game.ResourceType.valueOf(entry.getKey()),
                                entry.getValue()
                            );
                        } catch (IllegalArgumentException ignored) {}
                    }
                    // Localiza o Player pelo nome do sender
                    com.example.model.player.Player sender = manager.getPlayers().stream()
                        .filter(p -> p.getName().equals(senderName))
                        .findFirst().orElse(null);
                    if (sender == null) return false;
                    return discardState.submitDiscard(sender, selection, manager.getCurrentTurn());
                } catch (Exception e) {
                    System.out.println("Erro ao deserializar descarte: " + e.getMessage());
                    return false;
                }
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
        dto.setActiveTrade(activeTrade);

        // Preenche dados de descarte quando o estado for WaitingDiscardState
        if (state instanceof com.example.model.state.WaitingDiscardState discardState) {
            List<String> pending = discardState.getPendingPlayers()
                .stream()
                .map(com.example.model.player.Player::getName)
                .collect(java.util.stream.Collectors.toList());
            dto.setDiscardPendingPlayers(pending);

            Map<String, Integer> amounts = new HashMap<>();
            for (com.example.model.player.Player p : discardState.getPendingPlayers()) {
                int total = 0;
                for (com.example.model.game.ResourceType t : com.example.model.game.ResourceType.values()) {
                    if (t != com.example.model.game.ResourceType.DESERT)
                        total += p.getWallet().getResourceAmount(t);
                }
                amounts.put(p.getName(), total / 2); // quanto deve descartar
            }
            dto.setDiscardAmounts(amounts);
        }

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
