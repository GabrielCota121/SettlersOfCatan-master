package com.catan.model.state;

import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.Turn;
import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;

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
    boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn);
}

