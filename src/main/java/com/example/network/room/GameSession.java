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
import java.util.Collections;
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
    private final BotLogic botLogic;
    private final Map<String, Vertex> vertexById = new HashMap<>();
    private final Map<String, Edge> edgeById = new HashMap<>();
    private final Map<String, com.example.model.board.Tile> tileById = new HashMap<>();

    // Estado da negociação em andamento (null = nenhuma)
    private com.example.network.protocol.TradeStatusDTO activeTrade = null;

    // Último trade resolvido/cancelado — enviado uma vez só pelo handler
    private com.example.network.protocol.TradeStatusDTO lastResolvedTrade = null;

    // Vítimas possíveis após MOVE_ROBBER, aguardando STEAL_FROM
    private java.util.List<Player> pendingRobberVictims = null;

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
            Player p = new Player(id++, rp.getName(), rp.getColor());
            p.setBot(rp.isBot());
            players.add(p);
        }
        // Mantém a ordem em que os jogadores entraram na sala (sem shuffle)
        // para que servidor e clientes usem exatamente a mesma sequência.

        this.manager = new CatanGameManager(board, players, logger);
        this.botLogic = new BotLogic(manager);

        for (Vertex v : board.getVertices()) vertexById.put(v.getId(), v);
        for (Edge e : board.getEdges()) edgeById.put(e.getId(), e);
        for (com.example.model.board.Tile t : board.getTiles())
            tileById.put(String.valueOf(t.getId()), t);
    }

    public long getSeed() { return seed; }

    public synchronized boolean isCurrentPlayerBot() {
        Player current = manager.getCurrentTurn().getCurrentPlayer();
        return current != null && current.isBot();
    }

    public synchronized String getCurrentPlayerName() {
        return manager.getCurrentTurn().getCurrentPlayer().getName();
    }

    /**
     * Se o jogador da vez for bot e estiver na fase principal/rolagem,
     * executa o turno dele. Retorna true se um bot jogou.
     */
    public synchronized boolean runBotTurnIfNeeded() {
        Player current = manager.getCurrentTurn().getCurrentPlayer();
        if (current == null || !current.isBot()) return false;

        ITurnState state = manager.getCurrentTurn().getState();

        // Fase de setup
        if (state instanceof com.example.model.state.SetupState) {
            botLogic.playSetupTurn();
            return true;
        }

        // Turno normal
        if (state instanceof com.example.model.state.MainState
                || state instanceof com.example.model.state.WaitingRollState) {
            botLogic.playMainTurn();
            return true;
        }
        return false;
    }

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
        // Ações que QUALQUER jogador pode enviar (não só o da vez):
        // - TRADE_RESPONSE: responder a uma proposta de troca
        // - SUBMIT_DISCARD: descartar cartas quando sai 7 (todos com +7 cartas)
        boolean isAllowedForNonCurrent =
            "TRADE_RESPONSE".equals(action) || "SUBMIT_DISCARD".equals(action);
        if (!isCurrentPlayer && !isAllowedForNonCurrent) {
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
                if (targetId == null) return false;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = om.readValue(targetId, Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> give =
                        (Map<String, Integer>) payload.get("give");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> want =
                        (Map<String, Integer>) payload.get("want");
                    if (give == null || want == null) return false;
                    if (give.isEmpty() || want.isEmpty()) {
                        manager.getLogger().log(
                            "Troca inválida: precisa dar e pedir ao menos 1 recurso.");
                        return false;
                    }

                    // REGRA: não pode pedir o mesmo recurso que está dando
                    for (String k : give.keySet()) {
                        if (want.containsKey(k) && give.get(k) > 0 && want.get(k) > 0) {
                            manager.getLogger().log(
                                "Troca inválida: não pode pedir o mesmo recurso "
                                + "que está oferecendo (" + k + ").");
                            return false;
                        }
                    }

                    // Proponente precisa ter o que oferece
                    for (Map.Entry<String, Integer> e : give.entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        if (current.getWallet().getResourceAmount(rt) < e.getValue()) {
                            manager.getLogger().log(
                                "Troca inválida: você não tem " + e.getValue()
                                + " " + e.getKey());
                            return false;
                        }
                    }

                    activeTrade = buildTradeStatus(senderName, give, want);
                    return true;
                } catch (Exception e) {
                    System.out.println("Erro PROPOSE_TRADE: " + e.getMessage());
                    return false;
                }
            }

            case "TRADE_RESPONSE": {
                if (activeTrade == null || !activeTrade.isActive()) return false;
                if (activeTrade.getProposerName().equals(senderName)) return false;
                if (!activeTrade.getResponses().containsKey(senderName)) return false;

                boolean accepts = "true".equalsIgnoreCase(targetId);

                if (accepts) {
                    // Valida que o respondedor TEM os recursos que o proponente quer
                    com.example.model.player.Player responder = manager.getPlayers()
                        .stream().filter(p -> p.getName().equals(senderName))
                        .findFirst().orElse(null);
                    if (responder == null) return false;
                    for (Map.Entry<String, Integer> e :
                            activeTrade.getWant().entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        if (responder.getWallet().getResourceAmount(rt)
                                < e.getValue()) {
                            manager.getLogger().log(senderName
                                + " não tem recursos para aceitar (precisa de "
                                + e.getValue() + " " + e.getKey() + ")");
                            // Marca como DECLINED automaticamente
                            activeTrade.getResponses().put(senderName, "DECLINED");

                            if (activeTrade.getResponses().values().stream().noneMatch("PENDING"::equals)
                                    && activeTrade.getResponses().values().stream().noneMatch("ACCEPTED"::equals)) {
                                manager.getLogger().log("Todos recusaram a troca de "
                                    + activeTrade.getProposerName() + ".");
                                activeTrade.setActive(false);
                                activeTrade.setResolvedWithPlayer(null);
                                lastResolvedTrade = activeTrade;
                                activeTrade = null;
                            }
                            return true; // resposta registrada (como recusa)
                        }
                    }
                }
                activeTrade.getResponses().put(senderName,
                    accepts ? "ACCEPTED" : "DECLINED");
                // Se TODOS os não-proponentes recusaram, encerra a troca
                boolean todosResponderam = activeTrade.getResponses().values().stream()
                    .noneMatch("PENDING"::equals);
                boolean alguemAceitou = activeTrade.getResponses().values().stream()
                    .anyMatch("ACCEPTED"::equals);
                if (todosResponderam && !alguemAceitou) {
                    manager.getLogger().log("Todos recusaram a troca de "
                        + activeTrade.getProposerName() + ".");
                    activeTrade.setActive(false);
                    activeTrade.setResolvedWithPlayer(null);
                    lastResolvedTrade = activeTrade;
                    activeTrade = null;
                }
                return true;
            }

            case "CONFIRM_TRADE": {
                if (activeTrade == null || !activeTrade.isActive()) return false;
                if (!activeTrade.getProposerName().equals(senderName)) return false;
                String partner = targetId;
                if (partner == null) return false;
                if (!"ACCEPTED".equals(activeTrade.getResponses().get(partner)))
                    return false;

                com.example.model.player.Player proposer = manager.getPlayers()
                    .stream().filter(p -> p.getName().equals(senderName))
                    .findFirst().orElse(null);
                com.example.model.player.Player partnerPlayer = manager.getPlayers()
                    .stream().filter(p -> p.getName().equals(partner))
                    .findFirst().orElse(null);
                if (proposer == null || partnerPlayer == null) return false;

                // VALIDAÇÃO PRÉVIA: ambos precisam ter os recursos ANTES de mover
                for (Map.Entry<String, Integer> e : activeTrade.getGive().entrySet()) {
                    com.example.model.game.ResourceType rt =
                        com.example.model.game.ResourceType.valueOf(e.getKey());
                    if (proposer.getWallet().getResourceAmount(rt) < e.getValue()) {
                        manager.getLogger().log("Troca cancelada: " + senderName
                            + " não tem " + e.getValue() + " " + e.getKey());
                        // Recusa a troca e limpa
                        activeTrade.setActive(false);
                        lastResolvedTrade = activeTrade;
                        activeTrade = null;
                        return false;
                    }
                }
                for (Map.Entry<String, Integer> e : activeTrade.getWant().entrySet()) {
                    com.example.model.game.ResourceType rt =
                        com.example.model.game.ResourceType.valueOf(e.getKey());
                    if (partnerPlayer.getWallet().getResourceAmount(rt) < e.getValue()) {
                        manager.getLogger().log("Troca cancelada: " + partner
                            + " não tem " + e.getValue() + " " + e.getKey());
                        activeTrade.setActive(false);
                        lastResolvedTrade = activeTrade;
                        activeTrade = null;
                        return false;
                    }
                }

                // Agora executa com segurança (todos têm os recursos)
                for (Map.Entry<String, Integer> e : activeTrade.getGive().entrySet()) {
                    com.example.model.game.ResourceType rt =
                        com.example.model.game.ResourceType.valueOf(e.getKey());
                    proposer.getWallet().removeResource(rt, e.getValue());
                    partnerPlayer.getWallet().addResource(rt, e.getValue());
                }
                for (Map.Entry<String, Integer> e : activeTrade.getWant().entrySet()) {
                    com.example.model.game.ResourceType rt =
                        com.example.model.game.ResourceType.valueOf(e.getKey());
                    partnerPlayer.getWallet().removeResource(rt, e.getValue());
                    proposer.getWallet().addResource(rt, e.getValue());
                }

                manager.getLogger().log("Troca: " + senderName + " deu "
                    + activeTrade.getGive() + " e recebeu " + activeTrade.getWant()
                    + " de " + partner);

                activeTrade.setActive(false);
                activeTrade.setResolvedWithPlayer(partner);
                lastResolvedTrade = activeTrade;  // envia uma vez pro cliente fechar
                activeTrade = null;               // LIMPA — corrige troca fantasma
                return true;
            }

            case "CANCEL_TRADE": {
                if (activeTrade == null) return false;
                if (!activeTrade.getProposerName().equals(senderName)) return false;
                activeTrade.setActive(false);
                lastResolvedTrade = activeTrade;
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
                    Map<String, Integer> giveMap =
                        (Map<String, Integer>) payload.get("give");
                    // receive pode ser String (single, retrocompat) ou Map (múltiplo)
                    Object receiveObj = payload.get("receive");
                    if (giveMap == null || receiveObj == null) return false;

                    Map<String, Integer> receiveMap = new HashMap<>();
                    if (receiveObj instanceof String) {
                        receiveMap.put((String) receiveObj, 1);
                    } else if (receiveObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> rm = (Map<String, Integer>) receiveObj;
                        receiveMap.putAll(rm);
                    } else {
                        return false;
                    }

                    com.example.model.player.Player player = manager.getPlayers().stream()
                        .filter(p -> p.getName().equals(senderName))
                        .findFirst().orElse(null);
                    if (player == null) return false;

                    // Valida proporção pelas tradeRates do jogador
                    int allowedReceive = 0;
                    for (Map.Entry<String, Integer> e : giveMap.entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        int rate = player.getTradeRates().getOrDefault(rt, 4);
                        if (e.getValue() % rate != 0) return false;
                        allowedReceive += e.getValue() / rate;
                        if (player.getWallet().getResourceAmount(rt) < e.getValue())
                            return false;
                    }
                    int totalReceive = receiveMap.values()
                        .stream().mapToInt(Integer::intValue).sum();
                    if (allowedReceive != totalReceive) return false;

                    // Executa
                    for (Map.Entry<String, Integer> e : giveMap.entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        player.getWallet().removeResource(rt, e.getValue());
                        manager.getBank().getWallet().addResource(rt, e.getValue());
                    }
                    for (Map.Entry<String, Integer> e : receiveMap.entrySet()) {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(e.getKey());
                        player.getWallet().addResource(rt, e.getValue());
                        manager.getBank().getWallet().removeResource(rt, e.getValue());
                    }
                    return true;
                } catch (Exception e) {
                    System.out.println("Erro em BANK_TRADE: " + e.getMessage());
                    return false;
                }
            }

            case "MOVE_ROBBER": {
                if (!(state instanceof com.example.model.state.MoveRobberState robberState))
                    return false;
                com.example.model.board.Tile tile = tileById.get(targetId);
                if (tile == null) return false;
                java.util.List<Player> victims = robberState.moveRobber(tile, turn);
                if (victims == null) return false; // tile inválido (mesmo do atual)

                // Guarda as vítimas possíveis para o STEAL_FROM seguinte
                pendingRobberVictims = victims;

                // Se não há vítimas, executa steal nulo (volta ao estado anterior)
                if (victims.isEmpty()) {
                    robberState.executeSteal(null, turn);
                    pendingRobberVictims = null;
                }
                return true;
            }

            case "STEAL_FROM": {
                if (!(state instanceof com.example.model.state.MoveRobberState robberState))
                    return false;
                Player victim = manager.getPlayers().stream()
                    .filter(p -> p.getName().equals(targetId))
                    .findFirst().orElse(null);
                // Valida que a vítima estava na lista permitida
                if (pendingRobberVictims != null && victim != null
                        && !pendingRobberVictims.contains(victim)) {
                    return false;
                }
                robberState.executeSteal(victim, turn);
                pendingRobberVictims = null;
                return true;
            }

            case "PLAY_KNIGHT": {
                com.example.model.cards.IDevelopmentCard knight =
                    findPlayableCard(current, "Knight");
                if (knight == null) return false;
                return state.playDevelopmentCard(knight, turn);
            }

            case "PLAY_MONOPOLY": {
                if (targetId == null) return false;
                com.example.model.cards.IDevelopmentCard mono =
                    findPlayableCard(current, "Monopoly");
                if (mono == null) return false;
                boolean played = state.playDevelopmentCard(mono, turn);
                if (!played) return false;
                // Após jogar, o estado vira MonopolyState — escolhe o recurso
                com.example.model.state.ITurnState newState = turn.getState();
                if (newState instanceof com.example.model.state.MonopolyState monoState) {
                    try {
                        com.example.model.game.ResourceType rt =
                            com.example.model.game.ResourceType.valueOf(targetId);
                        monoState.chooseResource(rt, turn);
                    } catch (IllegalArgumentException e) { return false; }
                }
                return true;
            }

            case "PLAY_YEAR_OF_PLENTY": {
                if (targetId == null) return false;
                com.example.model.cards.IDevelopmentCard yop =
                    findPlayableCard(current, "Year of Plenty");
                if (yop == null) return false;
                boolean played = state.playDevelopmentCard(yop, turn);
                if (!played) return false;
                com.example.model.state.ITurnState newState = turn.getState();
                if (newState instanceof com.example.model.state.YearOfPlentyState yopState) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, String> payload = om.readValue(targetId, Map.class);
                        com.example.model.game.ResourceType r1 =
                            com.example.model.game.ResourceType.valueOf(payload.get("res1"));
                        com.example.model.game.ResourceType r2 =
                            com.example.model.game.ResourceType.valueOf(payload.get("res2"));
                        yopState.chooseResources(r1, r2, turn);
                    } catch (Exception e) { return false; }
                }
                return true;
            }

            case "PLAY_ROAD_BUILDING": {
                com.example.model.cards.IDevelopmentCard rb =
                    findPlayableCard(current, "Road Building");
                if (rb == null) return false;
                return state.playDevelopmentCard(rb, turn);
            }

            case "PLAY_VICTORY_POINT": {
                com.example.model.player.Player sender2 = manager.getPlayers()
                    .stream().filter(p -> p.getName().equals(senderName))
                    .findFirst().orElse(null);
                if (sender2 == null) return false;
                java.util.Optional<com.example.model.cards.IDevelopmentCard> vpCardOpt =
                    sender2.getPlayableCards().stream()
                        .filter(c -> c instanceof com.example.model.cards.VictoryPointCard)
                        .findFirst();
                if (vpCardOpt.isEmpty()) {
                    vpCardOpt = sender2.getNewCards().stream()
                        .filter(c -> c instanceof com.example.model.cards.VictoryPointCard)
                        .findFirst();
                }
                if (vpCardOpt.isEmpty()) return false;
                com.example.model.cards.IDevelopmentCard vpCard = vpCardOpt.get();
                boolean ok = vpCard.play(manager, sender2);
                if (ok) {
                    sender2.getPlayableCards().remove(vpCard);
                    sender2.getNewCards().remove(vpCard);
                }
                return ok;
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

    public com.example.network.protocol.TradeStatusDTO consumeLastResolvedTrade() {
        com.example.network.protocol.TradeStatusDTO t = lastResolvedTrade;
        lastResolvedTrade = null;
        return t;
    }

    private com.example.model.cards.IDevelopmentCard findPlayableCard(
            Player player, String cardName) {
        for (com.example.model.cards.IDevelopmentCard c : player.getPlayableCards()) {
            if (c.getName().equals(cardName)) return c;
        }
        // Também procura em newCards (algumas implementações deixam lá)
        for (com.example.model.cards.IDevelopmentCard c : player.getNewCards()) {
            if (c.getName().equals(cardName)) return c;
        }
        return null;
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
        if (pendingRobberVictims != null && !pendingRobberVictims.isEmpty()) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Player p : pendingRobberVictims) names.add(p.getName());
            dto.setRobberVictims(names);
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

            Map<String, Integer> rates = new HashMap<>();
            for (Map.Entry<ResourceType, Integer> en : p.getTradeRates().entrySet()) {
                rates.put(en.getKey().name(), en.getValue());
            }
            ps.setTradeRates(rates);

            boolean isViewer = (viewerName == null || viewerName.equals(p.getName()));
            ps.setHiddenResources(!isViewer);

            if (isViewer) {
                // Viewer recebe os recursos reais e os nomes das cartas.
                Map<String, Integer> res = new HashMap<>();
                for (ResourceType type : ResourceType.values()) {
                    if (type != ResourceType.DESERT) res.put(type.name(), p.getWallet().getResourceAmount(type));
                }
                ps.setResources(res);

                List<String> allCards = new ArrayList<>();
                List<String> playable = new ArrayList<>();
                if (p.getPlayableCards() != null) {
                    for (IDevelopmentCard c : p.getPlayableCards()) {
                        allCards.add(c.getName());
                        playable.add(c.getName());
                    }
                }
                if (p.getNewCards() != null) {
                    for (IDevelopmentCard c : p.getNewCards()) {
                        allCards.add(c.getName());
                    }
                }
                ps.setDevCards(allCards);
                ps.setPlayableDevCards(playable);
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
