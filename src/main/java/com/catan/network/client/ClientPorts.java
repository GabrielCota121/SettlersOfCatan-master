package com.catan.network.client;

import com.catan.network.exception.PortaEmUsoException;
import com.catan.network.exception.PortaInvalidaException;
import com.catan.network.util.NetFunctions;

public class ClientPorts {
    private static int udpPort = 25565;
    private static int tcpPort = 25566;

    public static int getUdpPort() {
        return udpPort;
    }

    public static void setUdpPort(int udpPort) throws PortaInvalidaException, PortaEmUsoException {
        if(!NetFunctions.isPortValid(udpPort)){
            throw new PortaInvalidaException(udpPort);
        }
        if(udpPort == getTcpPort() || !NetFunctions.isPortFree(udpPort)){
            throw new PortaEmUsoException(udpPort);
        }
        ClientPorts.udpPort = udpPort;
    }

    public static int getTcpPort() {
        return tcpPort;
    }

    public static void setTcpPort(int tcpPort) throws PortaInvalidaException, PortaEmUsoException {
        if(!NetFunctions.isPortValid(tcpPort)){
            throw new PortaInvalidaException(tcpPort);
        }
        if(tcpPort == getUdpPort() || !NetFunctions.isPortFree(tcpPort)){
            throw new PortaEmUsoException(tcpPort);
        }
        ClientPorts.tcpPort = tcpPort;
    }
}
