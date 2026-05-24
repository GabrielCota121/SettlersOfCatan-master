package com.catan.network.server;

public class ServerPorts {
    private static final int serverUdpPort = 25567; // não pode mudar porque é a porta que vai ser usada pelo servidor para receber os beacons

    public static int getServerUdpPort(){
        return serverUdpPort;
    }

}
