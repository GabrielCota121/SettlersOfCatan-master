package com.example.network.protocol;

/**
 * Tipos de mensagem do protocolo WebSocket do Catan.
 *
 * Mantido como constantes String (em vez de enum) para que cliente e servidor
 * possam evoluir de forma independente sem quebrar a desserialização do JSON.
 */
public final class MessageType {

    private MessageType() {}

    // ----- Cliente -> Servidor -----
    public static final String LIST_ROOMS   = "LIST_ROOMS";   // pede a lista de salas
    public static final String CREATE_ROOM  = "CREATE_ROOM";  // data: roomName, maxPlayers, color
    public static final String JOIN_ROOM    = "JOIN_ROOM";    // roomId; data: color
    public static final String LEAVE_ROOM   = "LEAVE_ROOM";   // sai da sala atual
    public static final String SET_READY    = "SET_READY";    // data: ready (boolean)
    public static final String START_GAME   = "START_GAME";   // host inicia a partida
    public static final String GAME_ACTION  = "GAME_ACTION";  // data: action, targetId (jogada in-game)
    public static final String SUBMIT_DISCARD  = "SUBMIT_DISCARD";
    // data: resources (Map<String,Integer> com quantidades a descartar)

    public static final String PROPOSE_TRADE  = "PROPOSE_TRADE";
    // data: offer (Map com "give" e "want", cada um Map<String,Integer>)

    public static final String TRADE_RESPONSE = "TRADE_RESPONSE";
    // data: accept (boolean) — enviado pelos não-proponentes

    public static final String CONFIRM_TRADE  = "CONFIRM_TRADE";
    // data: partnerName (String) — proponente fecha com quem aceitou

    public static final String CANCEL_TRADE   = "CANCEL_TRADE";
    // sem data — proponente cancela a proposta

    public static final String BANK_TRADE    = "BANK_TRADE";
    // data no targetId: JSON {"give":{"WOOD":4},"receive":"ORE"}

    public static final String MOVE_ROBBER   = "MOVE_ROBBER";
    // targetId: tileId (String) do tile onde o ladrão vai

    public static final String STEAL_FROM    = "STEAL_FROM";
    // targetId: nome do jogador a ser roubado (String)

    public static final String PLAY_KNIGHT   = "PLAY_KNIGHT";
    public static final String PLAY_MONOPOLY = "PLAY_MONOPOLY";
    // targetId: recurso escolhido (ex: "WOOD")
    public static final String PLAY_YEAR_OF_PLENTY = "PLAY_YEAR_OF_PLENTY";
    // targetId: JSON {"res1":"WOOD","res2":"ORE"}
    public static final String PLAY_ROAD_BUILDING  = "PLAY_ROAD_BUILDING";
    public static final String PLAY_VICTORY_POINT  = "PLAY_VICTORY_POINT";

    // ----- Servidor -> Cliente -----
    public static final String ROOM_LIST    = "ROOM_LIST";    // data: rooms (List<RoomInfo>)
    public static final String ROOM_JOINED  = "ROOM_JOINED";  // data: room (RoomInfo) -> você entrou
    public static final String ROOM_UPDATE  = "ROOM_UPDATE";  // data: room (RoomInfo) -> algo mudou na sala
    public static final String ROOM_LEFT    = "ROOM_LEFT";    // confirmação de saída
    public static final String GAME_STARTED = "GAME_STARTED"; // data: room (RoomInfo), seed -> partida começou
    public static final String GAME_STATE   = "GAME_STATE";   // data: state (GameStateDTO) -> estado autoritativo
    public static final String GAME_EVENT   = "GAME_EVENT";   // data: message -> linha de log do jogo
    public static final String ERROR        = "ERROR";        // data: message
    public static final String DISCARD_REQUIRED = "DISCARD_REQUIRED";
    // data: players (List<String> com nomes de quem precisa descartar)
    public static final String DISCARD_WAITING  = "DISCARD_WAITING";
    // data: remaining (List<String> com nomes de quem ainda não submeteu)

    public static final String TRADE_UPDATE   = "TRADE_UPDATE";
    // data: trade (TradeStatusDTO) — broadcast do estado atual da negociação
}
