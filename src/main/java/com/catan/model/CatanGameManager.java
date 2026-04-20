package com.catan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CatanGameManager {

    private final Board board;
    private final List<Player> players;
    private final Dice dice1;
    private final Dice dice2;
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

    // olha que fofo esse UwU DEPOIS VAI PRA CLASSE BANCO
    public void distributeResources(int roll) {
        // super intuitivo pra gerar os recursos
        for (Tile tile : board.getTiles()) {
            if (tile.getNumberToken() == roll && !tile.equals(robber.getCurrentTile())) {
                ResourceType resourceYielded = tile.getResource();
                for (Vertex vertex : tile.getVertices()) {
                    if (!vertex.isEmpty()) {
                        VertexBuilding building = vertex.getBuilding();
                        Player owner = building.getOwner();
                        owner.getWallet().addResource(resourceYielded, building.getResourceYield());
                        logger.log(owner.getName() + " recebeu " + building.getResourceYield() + " " + resourceYielded);
                    }
                }
            } else if (tile.getNumberToken() == roll && tile.equals(robber.getCurrentTile())) {
                logger.log("O ladrão funcionou e bloqueau o recurso " + tile.getResource() + "!");
            }
        }
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
