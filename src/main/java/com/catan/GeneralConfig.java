package com.catan;

public class GeneralConfig {
    public static final int maxServerNameSize = 20; // tamanho máximo do nome do servidor
    public static final int maxPlayerNameSize = 15; // tamanho máximo do nome do player
    private static String playerName = "DefaultPlayer";
    public static void setPlayerName(String name){
        playerName = name;
    }
    public static String getPlayerName(){
        return playerName;
    }
}
