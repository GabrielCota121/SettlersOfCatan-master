package com.catan.network.server;

import com.catan.network.exception.PortaEmUsoException;
import com.catan.network.exception.PortaInvalidaException;
import com.catan.network.util.NetFunctions;

public class ServerPorts {
    private static final int serverUdpPort = 25567; // não pode mudar porque é a porta que vai ser usada pelo servidor para receber os beacons
    private static int serverTcpPort = 25568;

    public static int getServerUdpPort(){
        return serverUdpPort;
    }

    public static int getServerTcpPort(){
        return serverTcpPort;
    }
    public static void setServerTcpPort(int port) throws PortaInvalidaException, PortaEmUsoException {
        if(!NetFunctions.isPortValid(port)){
            throw new PortaInvalidaException(port);
        }
        if(!NetFunctions.isPortFree(serverTcpPort) || serverTcpPort == serverUdpPort){
            throw new PortaEmUsoException(port);
        }
        serverTcpPort = port;
    }
}
