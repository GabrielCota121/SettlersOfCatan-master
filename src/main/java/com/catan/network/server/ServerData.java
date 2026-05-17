package com.catan.network.server;

import com.catan.GeneralConfig;
import com.catan.network.exception.NomeServerMuitoLongoException;

public class ServerData {
    private static String nomeServer = "Unnamed Server";
    private static int numeroJogadores = 0;

    public static void setNomeServer(String nomeServer) throws NomeServerMuitoLongoException {
        if(nomeServer.length()> GeneralConfig.maxServerNameSize){
            throw new NomeServerMuitoLongoException();
        }
        nomeServer = nomeServer;
    }

    public static String getNomeServer() {
        return nomeServer;
    }
    public static int getNumeroJogadores(){
        return numeroJogadores;
    }
}
