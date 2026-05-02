package com.catan.model.state;

import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.Turn;
import com.catan.model.player.Player;

public class GameOverState implements ITurnState {
    private final Player winner;

    public GameOverState(Player winner) {
        this.winner = winner;
    }

    @Override
    public String getName() { return "Fim de Jogo" + winner.getName() + " venceu!";}

    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }

    @Override
    public boolean buyDevelopmentCard(Turn currentTurn) { return false; }

    @Override
    public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn) { return false; }

    @Override
    public boolean rollDice(Turn currentTurn) { return false; }

    @Override
    public boolean endTurn(Turn currentTurn) { return false; }

    @Override
    public boolean canEndTurn() { return false; }

    @Override
    public boolean canRollDice() { return false; }
}