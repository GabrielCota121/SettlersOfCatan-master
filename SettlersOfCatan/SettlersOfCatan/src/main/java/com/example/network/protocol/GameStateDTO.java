package com.example.network.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fotografia (snapshot) completa do estado dinâmico de uma partida, produzida
 * pelo servidor autoritativo e enviada aos clientes (mensagem GAME_STATE).
 *
 * <p>O layout estático do tabuleiro NÃO vai aqui: ele é reconstruído no cliente
 * a partir da {@code seed} enviada em GAME_STARTED. Aqui só viaja o que muda:
 * de quem é a vez, dados, ladrão, construções, recursos e pontos.
 */
public class GameStateDTO {

    private String currentPlayerName;
    private String stateName;        // ITurnState.getName()
    private boolean setupPhase;
    private boolean setupSecondPass; // relevante quando setupPhase == true
    private boolean canRollDice;
    private boolean canEndTurn;
    private int dice1;
    private int dice2;
    private int robberTileId = -1;
    private String winnerName;       // != null quando o jogo acabou

    private List<PlayerStateDTO> players = new ArrayList<>();
    private List<BuildingDTO> buildings = new ArrayList<>(); // settlements e cities (por vertexId)
    private List<BuildingDTO> roads = new ArrayList<>();      // estradas (por edgeId)
    private Map<String, Integer> bank = new HashMap<>();      // recursos do banco

    public String getCurrentPlayerName() { return currentPlayerName; }
    public void setCurrentPlayerName(String v) { this.currentPlayerName = v; }

    public String getStateName() { return stateName; }
    public void setStateName(String v) { this.stateName = v; }

    public boolean isSetupPhase() { return setupPhase; }
    public void setSetupPhase(boolean v) { this.setupPhase = v; }

    public boolean isSetupSecondPass() { return setupSecondPass; }
    public void setSetupSecondPass(boolean v) { this.setupSecondPass = v; }

    public boolean isCanRollDice() { return canRollDice; }
    public void setCanRollDice(boolean v) { this.canRollDice = v; }

    public boolean isCanEndTurn() { return canEndTurn; }
    public void setCanEndTurn(boolean v) { this.canEndTurn = v; }

    public int getDice1() { return dice1; }
    public void setDice1(int v) { this.dice1 = v; }

    public int getDice2() { return dice2; }
    public void setDice2(int v) { this.dice2 = v; }

    public int getRobberTileId() { return robberTileId; }
    public void setRobberTileId(int v) { this.robberTileId = v; }

    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String v) { this.winnerName = v; }

    public List<PlayerStateDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerStateDTO> v) { this.players = v == null ? new ArrayList<>() : v; }

    public List<BuildingDTO> getBuildings() { return buildings; }
    public void setBuildings(List<BuildingDTO> v) { this.buildings = v == null ? new ArrayList<>() : v; }

    public List<BuildingDTO> getRoads() { return roads; }
    public void setRoads(List<BuildingDTO> v) { this.roads = v == null ? new ArrayList<>() : v; }

    public Map<String, Integer> getBank() { return bank; }
    public void setBank(Map<String, Integer> v) { this.bank = v == null ? new HashMap<>() : v; }
}
