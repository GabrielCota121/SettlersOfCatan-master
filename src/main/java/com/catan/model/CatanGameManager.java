package com.catan.model;

import java.util.List;

public class CatanGameManager {

    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private final GameLogger logger;
    private Turn currentTurn;

    public CatanGameManager(Board board, List<Player> players, GameLogger logger) {
        this.board = board;
        this.players = players;
        this.logger = logger;
        this.dice = new Dice();

        if (!players.isEmpty()) {
            this.currentTurn = new Turn(players.get(0));
        }

        logger.log("Partida iniciada! O primeiro a jogar é: " + currentTurn.getCurrentPlayer().getName());
    }

    public void nextTurn() {
        int currentIndex = players.indexOf(currentTurn.getCurrentPlayer());
        int nextIndex = (currentIndex + 1) % players.size();

        currentTurn.setCurrentPlayer(players.get(nextIndex));
        // Dps aqui vai entrar pra resetar pra antes do prox player rolar
        // currentTurn.changeState(new WaitingRollState());

        logger.log("--- Turno do jogador: " + currentTurn.getCurrentPlayer().getName() + " ---");
    }

    // olha que fofo esse UwU
    public void rollDiceAndDistributeResources() {
        int roll = dice.roll();
        logger.log("Os dados smaram: " + roll);

        if (roll == 7) {
            logger.log(this.currentTurn.getCurrentPlayer() + " ativou o ladrão!");
            //TODO vai pro RobberState futuramente
            return;
        }
        // super intuitivo pra gerar os recursos
        for (Tile tile : board.getTiles()) {
            if (tile.getNumberToken() == roll) {
                ResourceType resourceYielded = tile.getResource();
                for (Vertex vertex : tile.getVertices()) {
                    if (!vertex.isEmpty()) {
                        VertexBuilding building = vertex.getBuilding();
                        Player owner = building.getOwner();
                        owner.getWallet().addResource(resourceYielded, building.getResourceYield());
                        logger.log(owner.getName() + " recebeu " + building.getResourceYield() + " " + resourceYielded);
                    }
                }
            }
        }
    }
}
