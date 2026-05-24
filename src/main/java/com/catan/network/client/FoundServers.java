package com.catan.network.client;

import java.net.Inet4Address;
import java.util.ArrayList;

public class FoundServers {
    private static final ArrayList<FoundServerInfo> foundServers = new ArrayList<>();
    public static void addServer(String nome, int quantidadeJogadores, Inet4Address ip) {
        foundServers.add(new FoundServerInfo(nome, quantidadeJogadores, ip));
    }
    public static ArrayList<FoundServerInfo> getAllFoundServers() {
        return foundServers;
    }
}
