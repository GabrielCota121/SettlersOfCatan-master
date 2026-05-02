package com.catan.model.trade;

import com.catan.model.game.ResourceType;
import com.catan.model.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TradeOffer {
    private final Player proposer;
    private final Map<ResourceType, Integer> offeredResources;
    private final Map<ResourceType, Integer> requestedResources;
    private final List<Player> acceptedBy;

    public TradeOffer(Player proposer, Map<ResourceType, Integer> offeredResources, Map<ResourceType, Integer> requestedResources) {
        validateTradeRules(offeredResources, requestedResources); // <- Regra de negócio aqui!

        this.proposer = proposer;
        this.offeredResources = offeredResources;
        this.requestedResources = requestedResources;
        this.acceptedBy = new ArrayList<>();
    }

    private void validateTradeRules(Map<ResourceType, Integer> offered, Map<ResourceType, Integer> requested) {
        int totalOffered = 0;
        int totalRequested = 0;

        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;

            int offAmt = offered.getOrDefault(type, 0);
            int reqAmt = requested.getOrDefault(type, 0);

            if (offAmt > 0 && reqAmt > 0) {
                throw new IllegalArgumentException("Você não pode pedir a mesma ocisa que oferece, tá doidão??");
            }

            totalOffered += offAmt;
            totalRequested += reqAmt;
        }

        if (totalOffered == 0 && totalRequested == 0) {
            throw new IllegalArgumentException("Você precisa oferecer ou pedir algum recurso, geniO!");
        }
    }

    public Player getProposer() { return proposer; }
    public Map<ResourceType, Integer> getOfferedResources() { return offeredResources; }
    public Map<ResourceType, Integer> getRequestedResources() { return requestedResources; }
    public List<Player> getAcceptedBy() { return acceptedBy; }

    public void addAcceptance(Player player) {
        if (!acceptedBy.contains(player)) {
            acceptedBy.add(player);
        }
    }

    public boolean canPlayerAfford(Player player) {
        return player.getWallet().hasEnoughResources(requestedResources);
    }
}