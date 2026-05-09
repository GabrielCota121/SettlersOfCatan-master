package com.catan.network;

import com.catan.network.Exception.PortaEmUsoException;
import com.catan.network.Exception.PortaInvalidaException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class SocketConfig {
    private static int udpPort = 25565;
    private static int tcpPort = 25566;


    public static int getUdpPort() {
        return udpPort;
    }

    public static void setUdpPort(int udpPort) throws PortaInvalidaException, PortaEmUsoException {
        if(udpPort < 1 || udpPort > 65535){
            throw new PortaInvalidaException(udpPort);
        }
        boolean emUso = false;
        try{
            new ServerSocket(udpPort);
        }catch(IOException e){
            emUso = true;
        }
        if(udpPort == getTcpPort() || emUso){
            throw new PortaEmUsoException(udpPort);
        }
        SocketConfig.udpPort = udpPort;
    }

    public static int getTcpPort() {
        return tcpPort;
    }

    public static void setTcpPort(int tcpPort) throws PortaInvalidaException, PortaEmUsoException {
        if(tcpPort < 1 || tcpPort > 65535){
            throw new PortaInvalidaException(tcpPort);
        }
        boolean emUso = false;
        try{
            new ServerSocket(tcpPort);
        }catch(IOException e){
            emUso = true;
        }
        if(tcpPort == getUdpPort() || emUso){
            throw new PortaEmUsoException(tcpPort);
        }
        SocketConfig.tcpPort = tcpPort;
    }
}
