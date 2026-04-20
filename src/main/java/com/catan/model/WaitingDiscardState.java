package com.catan.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingDiscardState implements ITurnState {

    private final List<Player> pendingPlayers;
    private final Map<Player, Map<ResourceType, Integer>> confirmedDiscards;

    public WaitingDiscardState(List<Player> playersToDiscard) {
        this.pendingPlayers = new ArrayList<>(playersToDiscard);
        this.confirmedDiscards = new HashMap<>();
    }

    @Override
    public String getName() {
        return "Aguardando descartarem as cartas! BORA CARAI!!";
    }

    public List<Player> getPendingPlayers() {
        return pendingPlayers;
    }

    public boolean submitDiscard(Player player, Map<ResourceType, Integer> discardSelection, Turn currentTurn) {
        if (!pendingPlayers.contains(player)) {
            currentTurn.getGameManager().getLogger().log(player.getName() + " não precisa descartar cartas.");
            return false;
        }

        int requiredDiscardCount = player.getWallet().getTotalCards() / 2;
        int selectedCount = discardSelection.values().stream().mapToInt(Integer::intValue).sum();

        if (selectedCount != requiredDiscardCount) {
            currentTurn.getGameManager().getLogger().log("Erro: " + player.getName() + " deve descartar exatamente " + requiredDiscardCount + " cartas.");
            return false;
        }
        confirmedDiscards.put(player, discardSelection);
        pendingPlayers.remove(player);
        currentTurn.getGameManager().getLogger().log(player.getName() + " confirmou suas cartas para descarte.");
        checkCompletion(currentTurn);
        return true;
    }

    private void checkCompletion(Turn currentTurn) {
        if (pendingPlayers.isEmpty()) {
            CatanGameManager manager = currentTurn.getGameManager();
            manager.getLogger().log("Todos os descartes recebidos. Descartando as cartas!");
            for (Map.Entry<Player, Map<ResourceType, Integer>> entry : confirmedDiscards.entrySet()) {
                Player p = entry.getKey();
                Map<ResourceType, Integer> discards = entry.getValue();

                for (Map.Entry<ResourceType, Integer> res : discards.entrySet()) {
                    p.getWallet().removeResource(res.getKey(), res.getValue());
                }
            }

            manager.getLogger().log("Mova o ladrão!");
            currentTurn.setState(new MoveRobberState());
        }
    }

    @Override
    public boolean rollDice(Turn currentTurn) { return false; }

    @Override
    public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }

    @Override
    public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }

    @Override
    public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }

    @Override
    public boolean buyDevelopmentCard(Turn currentTurn) { return false; }

    @Override
    public boolean endTurn(Turn currentTurn) { return false; }

    @Override
    public boolean canEndTurn() { return false; }

    @Override
    public boolean canRollDice() { return false; }
}
