package com.catan.network.server;

import com.catan.GeneralConfig;
import com.catan.model.player.Player;
import com.catan.network.exception.NomePlayerMuitoLongoException;
import com.catan.network.exception.NomeServerMuitoLongoException;

import java.util.ArrayList;
import java.util.List;

public class ServerData {
    private static String nomeServer = "Unnamed Server";
    private static int lastPlayerId = 1;
    private static List<Player> players = new ArrayList<>(6);

    public static void setNomeServer(String nome) throws NomeServerMuitoLongoException {
        if(nomeServer.length()> GeneralConfig.maxServerNameSize){
            throw new NomeServerMuitoLongoException();
        }
        nomeServer = nome;
    }
    public static void addPlayer(String nome) throws NomePlayerMuitoLongoException {
        if(nome.length()> GeneralConfig.maxPlayerNameSize){
            throw new NomePlayerMuitoLongoException();
        }
        players.add(new Player(lastPlayerId, nome, PlayerColor.getPlayerColor(lastPlayerId)));
        lastPlayerId++;
    }
    public static List<Player> getPlayers(){
        return players;
    }
    public static String getNomeServer() {
        return nomeServer;
    }
    public static int getNumeroJogadores(){
        return players.size();
    }
}
