package com.catan.network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sockets {
    /*
        Criarei um socket UDP em broadcast para cada rede a qual estou conectado e adiciono no HashMap
        Quando eu conectar a um servidor, crio um novo socket, usando o IP do servidor e porta, que virão no pacote conack
     */
    private static List<DatagramSocket> broadcastUdpSockets = new ArrayList<>();// usar na hora de detectar as partidas. Buscará em múltiplas redes
    private static DatagramSocket udpSocket = null;
    private static Socket tcpSocket = null;

    private Socket createTcpSocket(Inet4Address ip, int port) throws IOException {
        return new Socket(ip, port);
    }
    private static DatagramSocket createUdpSocket(Inet4Address ip, int port, boolean broadcast) throws IOException {
        return new DatagramSocket(port, ip);
    }
    public static void addNewBroadcastSocket(Inet4Address ipBroadcast, int port) throws IOException {
        broadcastUdpSockets.add(createUdpSocket(ipBroadcast, port, true)); // para eu poder recuperar um socket de broadcast dado meu ip na rede local
    }
    public static List<DatagramSocket> getAllBroadcastSockets(){
        return broadcastUdpSockets;
    }
    public static Socket getTCPSocket(){
        if(tcpSocket == null){
            //recievingTcpSocket
        }
        return null;
    }
}
