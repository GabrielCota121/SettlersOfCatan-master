package com.catan.model;

public interface ITurnState {
    String getName();
    boolean buildSettlement(Vertex vertex, Turn currentTurn);
    boolean buildRoad(Edge edge, Turn currentTurn);
    boolean buildCity(Vertex vertex, Turn currentTurn);
    boolean buyDevelopmentCard(Turn currentTurn);
    boolean rollDice(Turn currentTurn);
    boolean canEndTurn();
    boolean canRollDice();
    boolean endTurn(Turn currentTurn);
}


