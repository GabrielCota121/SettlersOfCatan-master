package com.catan.model.game;

import com.catan.model.logging.IGameLogger;
import com.catan.model.board.*;
import com.catan.model.building.VertexBuilding;
import com.catan.model.player.Player;
import com.catan.model.state.SetupState;
import com.catan.model.state.WaitingRollState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CatanGameManager {

    private final Board board;
    private final List<Player> players;
    private final Dice dice1;
    private final Dice dice2;
    private final Bank bank;
    private final IGameLogger logger;
    private Turn currentTurn;
    private List<Player> setupOrder;
    private int setupIndex = 0;
    private boolean isSetupPhase = true;
    private Runnable onTurnChanged;
    private Robber robber;

    public CatanGameManager(Board board, List<Player> players, IGameLogger logger) {
        this.board = board;
        this.players = players;
        this.logger = logger;
        this.dice1 = new Dice();
        this.dice2 = new Dice();
        this.bank = new Bank();

        for (Tile tile : board.getTiles()) {
            if (tile.getResource() == ResourceType.DESERT) {
                this.robber = new Robber(tile);
                break;
            }
        }

        this.setupOrder = new ArrayList<>(players);
        List<Player> reverse = new ArrayList<>(players);
        Collections.reverse(reverse);
        this.setupOrder.addAll(reverse);

        this.currentTurn = new Turn(setupOrder.get(0), this);
        this.currentTurn.setState(new SetupState(false));


    }

    public void setOnTurnChangedListener(Runnable listener) {
        this.onTurnChanged = listener;
    }

    public List<Player> getPlayers() {return players;}

    public void proceedTurn() {
        if (isSetupPhase) {
            setupIndex++;
            if (setupIndex < setupOrder.size()) {
                boolean isSecondPass = setupIndex >= players.size();
                Player nextPlayer = setupOrder.get(setupIndex);
                this.currentTurn = new Turn(nextPlayer, this);
                this.currentTurn.setState(new SetupState(isSecondPass));
                logger.log("Setup: Vez de " + nextPlayer.getName());
            } else {
                this.isSetupPhase = false;

                this.currentTurn = new Turn(players.get(0), this);
                this.currentTurn.setState(new WaitingRollState());
                logger.log("Setup finalizado!");
                logger.log(currentTurn.getState().getName());
            }
        } else {
            int currentIndex = players.indexOf(currentTurn.getCurrentPlayer());
            int nextIndex = (currentIndex + 1) % players.size();
            this.currentTurn = new Turn(players.get(nextIndex), this);
            this.currentTurn.setState(new WaitingRollState());
            logger.log("Vez de " + currentTurn.getCurrentPlayer().getName());
        }
        if (onTurnChanged != null) {
            onTurnChanged.run();
        }
    }

    public Dice getDice1() {return dice1;}
    public Dice getDice2() {return dice2;}
    public Bank getBank() {return bank;}
    public Turn getCurrentTurn() {return currentTurn;}
    public IGameLogger getLogger() {return logger;}
    public Robber getRobber() {return robber;}

    public boolean rollDice(Player player) {
        if (!player.equals(currentTurn.getCurrentPlayer())) {
            logger.error(player.getName() + ", não tá na sua vez de rolar os dados, zé!");
            return false;
        }
        return currentTurn.getState().rollDice(currentTurn);
    }


    public void distributeResources(int roll) {
        bank.distributeResources(roll, board, robber, logger);
    }

    public void applyPortBonus(Player player, Port port) {
        if (port.getResource() == null) {
            for (ResourceType type : ResourceType.values()) {
                if (type != ResourceType.DESERT) {
                    player.setTradeRate(type, 3);
                }
            }
            logger.log(player.getName() + " agora possui porto 3 pra 1!");
        } else {
            player.setTradeRate(port.getResource(), 2);
            logger.log(player.getName() + " agora possui porto de " + port.getResource() + "!" );
        }
    }
}
