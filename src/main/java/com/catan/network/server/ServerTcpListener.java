package com.catan.network.server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerTcpListener extends Thread{
    private ServerSocket recievingSocket;
    private Map<Inet4Address, Socket> ConnectedClients = new HashMap<>();

    public ServerTcpListener() throws IOException {
        recievingSocket = new ServerSocket(ServerPorts.getServerTcpPort());
    }
    @Override
    public void run() {

    }
    private void executeConnect(){

    }
}
