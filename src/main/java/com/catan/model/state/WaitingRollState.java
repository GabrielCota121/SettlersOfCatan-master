package com.catan.model.state;

import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.cards.VictoryPointCard;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.Turn;
import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class WaitingRollState implements ITurnState {

    @Override
    public String getName() { return "Aguardando Rolar os dados!"; }

    @Override
    public boolean rollDice(Turn currentTurn) {
        CatanGameManager manager = currentTurn.getGameManager();
        manager.getDice1().roll();
        manager.getDice2().roll();

        int total = manager.getDice1().getResult() + manager.getDice2().getResult();
        manager.getLogger().log(currentTurn.getCurrentPlayer().getName() + " rolou: " + total);

        if (total == 7) {
            manager.getLogger().log("Rolou 7! Verificando limite de cartas");
            List<Player> playersToDiscard = new ArrayList<>();
            for (Player p : manager.getPlayers()) {
                if (p.getWallet().getTotalCards() >= 8) {
                    playersToDiscard.add(p);
                }
            }

            if (playersToDiscard.isEmpty()) {
                manager.getLogger().log("Ninguém tem mais de 7 cartas!. Clique em um tile para mover o Ladrão!");
                currentTurn.setState(new MoveRobberState(new MainState()));
            } else {
                manager.getLogger().log("Alguns jogadores precisam descartar cartas!");
                currentTurn.setState(new WaitingDiscardState(playersToDiscard));
            }
        } else {
            manager.distributeResources(total);
            currentTurn.setState(new MainState());
        }
        return true;
    }


    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }

    @Override
    public boolean buyDevelopmentCard(Turn currentTurn) { return false; }

    @Override
    public boolean endTurn(Turn currentTurn) {
        return false;
    }

    @Override
    public boolean canEndTurn() { return false; }

    @Override
    public boolean canRollDice() { return true; }

    public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn) {

        if (currentTurn.hasPlayedDevCardThisTurn() && !(card instanceof VictoryPointCard)) {
            currentTurn.getGameManager().getLogger().log("Só pode usar uma Development Card por turno, zé!!!");
            return false;
        }

        boolean success = card.play(currentTurn.getGameManager(), currentTurn.getCurrentPlayer());

        if (success) {

            if (!(card instanceof VictoryPointCard)) {
                currentTurn.markDevCardAsPlayed();
            }
            currentTurn.getCurrentPlayer().removeCard(card);
            checkWinCondition(currentTurn);
        }
        return success;
    }

    private void checkWinCondition(Turn currentTurn) {
        Player activePlayer = currentTurn.getCurrentPlayer();
        if (activePlayer.getVictoryPoints() >= 10) {
            currentTurn.getGameManager().getLogger().log("Fim do jogo! " + activePlayer.getName() + " ganhou com " + activePlayer.getVictoryPoints() + " pontos de vitória! GRATS!");
            currentTurn.setState(new GameOverState(activePlayer));
        }
    }
}