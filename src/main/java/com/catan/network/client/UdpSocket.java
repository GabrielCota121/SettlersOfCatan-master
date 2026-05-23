package com.catan.network.client;

import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpSocket {
    private static DatagramSocket udpSocket = null;
    public static DatagramSocket getUdpSocket() throws SocketException {
        if(udpSocket == null){
            udpSocket = new DatagramSocket(ClientPorts.getUdpPort());
        }
        return udpSocket;
    }
}
