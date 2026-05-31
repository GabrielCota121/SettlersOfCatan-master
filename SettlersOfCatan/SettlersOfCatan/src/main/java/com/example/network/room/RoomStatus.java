package com.example.network.room;

/** Ciclo de vida de uma sala. */
public enum RoomStatus {
    WAITING,   // aguardando jogadores no lobby
    IN_GAME,   // partida em andamento
    FINISHED   // partida encerrada
}
