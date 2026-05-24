package com.catan.network.client;

import com.catan.network.exception.PortaEmUsoException;
import com.catan.network.exception.PortaInvalidaException;
import com.catan.network.util.NetFunctions;

public class ClientPorts {
    private static int udpPort = 25565;

    public static int getUdpPort() {
        return udpPort;
    }

    public static void setUdpPort(int udpPort) throws PortaInvalidaException, PortaEmUsoException {
        if(!NetFunctions.isPortValid(udpPort)){
            throw new PortaInvalidaException(udpPort);
        }
        if(!NetFunctions.isPortFree(udpPort)){
            throw new PortaEmUsoException(udpPort);
        }
        ClientPorts.udpPort = udpPort;
    }


}
