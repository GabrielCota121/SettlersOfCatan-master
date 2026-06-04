package com.example.network.room;

import com.example.model.board.Edge;
import com.example.model.board.Tile;
import com.example.model.board.Vertex;
import com.example.model.building.BuildingCost;
import com.example.model.building.City;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.player.Player;
import com.example.model.state.ITurnState;
import com.example.model.state.MainState;
import com.example.model.state.SetupState;
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

    /**
     * Executa a jogada de setup do bot: coloca o settlement no melhor
     * vértice livre e uma road adjacente válida.
     */
    public void playSetupTurn() {
        ITurnState state = manager.getCurrentTurn().getState();
        if (!(state instanceof SetupState setupState)) {
            return;
        }

        // 1. Escolhe o vértice livre de maior produção que respeita a regra de distância.
        Vertex melhor = null;
        int melhorProducao = -1;
        for (Vertex v : manager.getBoard().getVertices()) {
            if (v.isEmpty() && v.respectsDistanceRule()) {
                int prod = producaoDoVertice(v);
                if (prod > melhorProducao) {
                    melhorProducao = prod;
                    melhor = v;
                }
            }
        }
        if (melhor == null) return;

        // 2. Constrói o settlement
        boolean ok = setupState.buildSettlement(melhor, manager.getCurrentTurn());
        if (!ok) return;

        // 3. Constrói uma road adjacente válida (deve sair do settlement recém-colocado).
        //    SetupState.buildRoad já chama proceedTurn internamente.
        for (Edge e : melhor.getAdjacentEdges()) {
            if (e.isEmpty()) {
                boolean roadOk = manager.getCurrentTurn().getState()
                    .buildRoad(e, manager.getCurrentTurn());
                if (roadOk) return;
            }
        }
    }

    /** Valor de produção de um número de dado (quantidade de pontinhos). */
    private int valorProducao(int numberToken) {
        return switch (numberToken) {
            case 2, 12 -> 1;
            case 3, 11 -> 2;
            case 4, 10 -> 3;
            case 5, 9  -> 4;
            case 6, 8  -> 5;
            default    -> 0; // 7 ou deserto
        };
    }

    /** Produção total de um vértice = soma do valor de produção de todos os tiles adjacentes. */
    private int producaoDoVertice(Vertex v) {
        int total = 0;
        for (Tile t : v.getAdjacentTiles()) {
            if (t.getResource() != ResourceType.DESERT) {
                total += valorProducao(t.getNumberToken());
            }
        }
        return total;
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
