package com.catan.network.server;

public class PlayerColor {
    //todo placeholder
    public static String getPlayerColor(int id){
        return switch (id) {
            case 1 -> "RED";
            case 2 -> "PURPLE";
            case 3 -> "BLACK";
            case 4 -> "GREEN";
            case 5 -> "BRONZE";
            case 6 -> "YELLOW";
            default -> throw new IllegalArgumentException("Unexpected value: " + id);
        };
    }
}
