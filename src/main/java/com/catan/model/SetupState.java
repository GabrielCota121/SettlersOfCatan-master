package com.catan.model;

// O Estado de Setup Inicial
public class SetupState implements ITurnState {
    @Override
    public String getName() { return "Setup"; }
    private boolean settlementPlaced = false;
    private boolean roadPlaced = false;
    private boolean isSecondPass = false;

    // construtor diferente pra quando for a segunda passada
    public SetupState(boolean secondPass){
        isSecondPass = secondPass;
    }

    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) {
        if (settlementPlaced) return false;
        if (!vertex.isEmpty() || !vertex.respectsDistanceRule()) {
            return false;
        }
        Player activePlayer = currentTurn.getCurrentPlayer();
        vertex.setBuilding(new Settlement(activePlayer, vertex));
        if (vertex.hasPort()) {
            currentTurn.getGameManager().applyPortBonus(activePlayer, vertex.getPort());
        }
        settlementPlaced = true;
        IGameLogger logger = currentTurn.getGameManager().getLogger();
        logger.log(activePlayer.getName() + " construiu um Settlement!");

        // Precisa dar recurso se for o segundo settlement a ser colocado!
        if (isSecondPass) {
            for (Tile tile : vertex.getAdjacentTiles()) {
                ResourceType type = tile.getResource();

                if (type != ResourceType.DESERT) {
                    activePlayer.getWallet().addResource(type, vertex.getBuilding().getResourceYield());
                    logger.log(activePlayer.getName() + " Recebeu " + vertex.getBuilding().getResourceYield() + " " + type);
                }
            }
        }
        return true;
    }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) {
        if (!settlementPlaced || roadPlaced) return false;
        Player activePlayer = currentTurn.getCurrentPlayer();

        if (!edge.isEmpty()) {
            return false;
        }

        // isso é FUNDAMENTAL pra garantir que o player possa construir uma road APENAS
        // em uma edge que incida em um vertex que ele tem vertexBuilding E que não tenha
        // nenhuma estrada! Ou o player poderia construir 2 estradas conectadas e um settlement
        // sem nenhuma road saindo dele!

        boolean connectsToOrphanV1 = !edge.getV1().isEmpty() &&
                edge.getV1().getBuilding().getOwner().equals(activePlayer) &&
                !edge.getV1().hasConnectingRoadFor(activePlayer);

        boolean connectsToOrphanV2 = !edge.getV2().isEmpty() &&
                edge.getV2().getBuilding().getOwner().equals(activePlayer) &&
                !edge.getV2().hasConnectingRoadFor(activePlayer);

        if (!connectsToOrphanV1 && !connectsToOrphanV2) {
            return false;
        }
        edge.setBuilding(new Road(activePlayer, edge));
        roadPlaced = true;
        currentTurn.getGameManager().proceedTurn();
        return true;
    }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) {
        return false;
    }

    @Override
    public boolean buyDevelopmentCard(Turn currentTurn) {
        return false;
    }

    @Override
    public boolean rollDice(Turn currentTurn) {
        return false;
    }

    @Override
    public boolean endTurn(Turn currentTurn) {
        return false;
    }

    @Override
    public boolean canEndTurn() { return false; }

    @Override
    public boolean canRollDice() { return false; }
}
