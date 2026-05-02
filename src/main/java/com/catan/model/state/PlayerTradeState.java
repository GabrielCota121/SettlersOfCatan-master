package com.catan.model.state;

import com.catan.model.board.Edge;
import com.catan.model.board.Vertex;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.player.Player;
import com.catan.model.trade.TradeOffer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerTradeState implements ITurnState {
    private final TradeOffer offer;
    private final List<Player> playersToAsk;
    private int currentPlayerIndex = 0;
    private boolean waitingForProposerToChoose = false;

    public PlayerTradeState(TradeOffer offer, List<Player> allPlayers) {
        this.offer = offer;
        this.playersToAsk = allPlayers.stream()
                .filter(p -> !p.equals(offer.getProposer()))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        if (waitingForProposerToChoose) return "Fechando Acordo";
        return "Aguardando Resposta: " + getCurrentTargetPlayer().getName();
    }

    public Player getCurrentTargetPlayer() {
        if (currentPlayerIndex >= playersToAsk.size()) return null;
        return playersToAsk.get(currentPlayerIndex);
    }

    public TradeOffer getOffer() {
        return offer;
    }

    public boolean isWaitingForProposer() {
        return waitingForProposerToChoose;
    }

    public void registerResponse(boolean accepted, Turn currentTurn) {
        if (waitingForProposerToChoose) return;

        Player target = getCurrentTargetPlayer();

        if (accepted && offer.canPlayerAfford(target)) {
            offer.addAcceptance(target);
            currentTurn.getGameManager().getLogger().log(target.getName() + " aceitou a proposta!");
        } else {
            currentTurn.getGameManager().getLogger().log(target.getName() + " recusou a proposta.");
        }

        currentPlayerIndex++;

        if (currentPlayerIndex >= playersToAsk.size()) {
            if (offer.getAcceptedBy().isEmpty()) {
                currentTurn.getGameManager().getLogger().log("Ninguém aceitou a proposta. Voltando ao turno.");
                currentTurn.setState(new MainState());
            } else {
                currentTurn.getGameManager().getLogger().log("Respostas coletadas. " + offer.getProposer().getName() + " deve escolher com quem fechar negócio.");
                waitingForProposerToChoose = true;
            }
        }
    }

    public void executeTrade(Player chosenPartner, Turn currentTurn) {
        if (!waitingForProposerToChoose || !offer.getAcceptedBy().contains(chosenPartner)) return;

        Player proposer = offer.getProposer();

        proposer.getWallet().payCost(offer.getOfferedResources());
        for (Map.Entry<ResourceType, Integer> entry : offer.getOfferedResources().entrySet()) {
            if (entry.getValue() > 0) {
                chosenPartner.getWallet().addResource(entry.getKey(), entry.getValue());
            }
        }

        chosenPartner.getWallet().payCost(offer.getRequestedResources());
        for (Map.Entry<ResourceType, Integer> entry : offer.getRequestedResources().entrySet()) {
            if (entry.getValue() > 0) {
                proposer.getWallet().addResource(entry.getKey(), entry.getValue());
            }
        }

        String offeredStr = formatResourceMap(offer.getOfferedResources());
        String requestedStr = formatResourceMap(offer.getRequestedResources());

        currentTurn.getGameManager().getLogger().log("Troca concluída! " +
                proposer.getName() + " deu [" + offeredStr + "] para " + chosenPartner.getName() +
                " em troca de [" + requestedStr + "].");

        currentTurn.setState(new MainState());
    }

    private String formatResourceMap(Map<ResourceType, Integer> map) {
        return map.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> e.getValue() + " " + e.getKey().name())
                .collect(Collectors.joining(", "));
    }

    public void cancelTrade(Turn currentTurn) {
        currentTurn.getGameManager().getLogger().log(offer.getProposer().getName() + " cancelou a negociação!");
        currentTurn.setState(new MainState());
    }

    @Override public boolean playDevelopmentCard(IDevelopmentCard card, Turn currentTurn) {return false;}
    @Override public boolean buildSettlement(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buildCity(Vertex vertex, Turn currentTurn) { return false; }
    @Override public boolean buildRoad(Edge edge, Turn currentTurn) { return false; }
    @Override public boolean buyDevelopmentCard(Turn currentTurn) { return false; }
    @Override public boolean rollDice(Turn currentTurn) { return false; }
    @Override public boolean endTurn(Turn currentTurn) { return false; }
    @Override public boolean canEndTurn() { return false; }
    @Override public boolean canRollDice() { return false; }
}