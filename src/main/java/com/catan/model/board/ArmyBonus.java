package com.catan.model.board;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;

public class ArmyBonus {
    private static final int MIN_KNIGHTS_FOR_BONUS = 3;
    private Player currentHolder = null;

    private int currentLargest = 2;
    private final IGameLogger logger;

    public ArmyBonus(IGameLogger logger) {
        this.logger = logger;
    }

    public void updateLargestArmy(Player player) {
        int playerKnights = player.getNumKnights();

        if (playerKnights > currentLargest) {

            if (currentHolder != null && currentHolder.equals(player)) {
                currentLargest = playerKnights;
                return;
            }

            if (currentHolder != null) {
                currentHolder.decrementVictoryPoints();
                currentHolder.decrementVictoryPoints();
                logger.log(player.getName() + " usou " + playerKnights + " Knights e tomou o Largest Army de " + currentHolder.getName() + "!");
            } else {
                logger.log(player.getName() + " usou " + playerKnights + " Knights e conquistou o Largest Army!");
            }

            currentHolder = player;
            currentLargest = playerKnights;
            player.incrementVictoryPoints();
            player.incrementVictoryPoints();
        }
    }

    public Player getCurrentHolder() { return currentHolder; }
    public int getCurrentLargest() { return currentLargest; }
}