package com.example.network.room;

import com.example.model.board.Edge;
import com.example.model.board.Vertex;
import com.example.model.building.BuildingCost;
import com.example.model.building.City;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.player.Player;
import com.example.model.state.ITurnState;
import com.example.model.state.MainState;
import com.example.model.state.WaitingRollState;

import java.util.Map;

/**
 * Lógica dos bots "gigaburros". Roda no servidor, manipulando o
 * CatanGameManager autoritativo da partida.
 */
public class BotLogic {

    private final CatanGameManager manager;

    public BotLogic(CatanGameManager manager) {
        this.manager = manager;
    }

    /**
     * Executa um turno completo do bot na fase principal:
     * rola o dado (se preciso), tenta construir, e passa a vez.
     */
    public void playMainTurn() {
        ITurnState state = manager.getCurrentTurn().getState();
        Player bot = manager.getCurrentTurn().getCurrentPlayer();

        // 1. Rola o dado se ainda não rolou
        if (state instanceof WaitingRollState) {
            manager.rollDice(bot);
            state = manager.getCurrentTurn().getState();
        }

        // Se rolou 7 o estado vira MoveRobberState ou WaitingDiscardState —
        // esses casos são tratados nas fases 4 e 5. Por ora saímos.
        if (!(state instanceof MainState mainState)) {
            return;
        }

        // 2. Tenta construir: cidade → settlement → road
        boolean construiu = true;
        int seguranca = 0;
        while (construiu && seguranca++ < 10) {
            construiu = false;

            if (canAfford(bot, BuildingCost.CITY) && bot.getNumCities() < 4) {
                Vertex alvo = acharSettlementParaCidade(bot);
                if (alvo != null && mainState.buildCity(alvo, manager.getCurrentTurn())) {
                    construiu = true;
                    continue;
                }
            }

            if (canAfford(bot, BuildingCost.SETTLEMENT) && bot.getNumSettlements() < 5) {
                Vertex alvo = acharVerticeParaSettlement(bot);
                if (alvo != null && mainState.buildSettlement(alvo, manager.getCurrentTurn())) {
                    construiu = true;
                    continue;
                }
            }

            if (canAfford(bot, BuildingCost.ROAD) && bot.getNumRoads() < 15) {
                Edge alvo = acharEdgeParaRoad(bot);
                if (alvo != null && mainState.buildRoad(alvo, manager.getCurrentTurn())) {
                    construiu = true;
                    continue;
                }
            }
        }

        // 3. Passa a vez
        manager.getCurrentTurn().getState().endTurn(manager.getCurrentTurn());
    }

    // ── Helpers ────────────────────────────────────────────────

    private boolean canAfford(Player p, BuildingCost cost) {
        for (Map.Entry<ResourceType, Integer> e : cost.getCost().entrySet()) {
            if (p.getWallet().getResourceAmount(e.getKey()) < e.getValue())
                return false;
        }
        return true;
    }

    /** Settlement do bot que ainda não foi promovido a cidade. */
    private Vertex acharSettlementParaCidade(Player bot) {
        for (Vertex v : manager.getBoard().getVertices()) {
            if (!v.isEmpty()
                    && v.getBuilding().getOwner().equals(bot)
                    && !(v.getBuilding() instanceof City)) {
                return v;
            }
        }
        return null;
    }

    /** Vértice livre, conectado a uma estrada do bot, respeitando regra de distância. */
    private Vertex acharVerticeParaSettlement(Player bot) {
        for (Vertex v : manager.getBoard().getVertices()) {
            if (v.isEmpty()
                    && v.respectsDistanceRule()
                    && v.hasConnectingRoadFor(bot)) {
                return v;
            }
        }
        return null;
    }

    /** Edge livre adjacente a alguma construção ou estrada do bot. */
    private Edge acharEdgeParaRoad(Player bot) {
        for (Edge e : manager.getBoard().getEdges()) {
            if (e.isEmpty() && edgeConectaAoBot(e, bot)) {
                return e;
            }
        }
        return null;
    }

    private boolean edgeConectaAoBot(Edge e, Player bot) {
        for (Vertex v : new Vertex[]{e.getV1(), e.getV2()}) {
            if (!v.isEmpty() && v.getBuilding().getOwner().equals(bot))
                return true;
            for (Edge adj : v.getAdjacentEdges()) {
                if (!adj.isEmpty() && adj.getBuilding().getOwner().equals(bot))
                    return true;
            }
        }
        return false;
    }
}
