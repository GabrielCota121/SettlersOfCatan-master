package com.example.model.state;

// ⬇️ IMPORTS ATUALIZADOS PARA O NOVO PACOTE DO SPRING BOOT ⬇️
import com.example.model.cards.IDevelopmentCard;
import com.example.model.game.Turn;
import com.example.model.board.Edge;
import com.example.model.board.Vertex;

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

