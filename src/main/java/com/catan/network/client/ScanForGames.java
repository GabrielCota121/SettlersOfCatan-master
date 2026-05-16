package com.catan.network.client;

import com.catan.network.Sockets;
import com.catan.network.packets.PacketBuilder;
import com.catan.network.server.ServerPorts;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.List;

import static com.catan.network.util.NetFunctions.isIpV4Address;

public class ScanForGames extends Thread {
    
    public void scan(){
        start();
    }
    @Override
    public void run() {
        System.out.println("Scan thread started");
       // passo 1: verificar as redes nas quais estou conectado e criar um socket de broadscast para cada uma delas
        try {
            Enumeration<NetworkInterface> netCards = NetworkInterface.getNetworkInterfaces(); // pego todas as placas de rede
            while (netCards.hasMoreElements()) { // para cada placa de rede
                NetworkInterface netCard = netCards.nextElement();
                Enumeration<InetAddress> inetAddresses = netCard.getInetAddresses(); // pego os endereços ip associados àquela placa (um ipv6 e um ipv4, normalmente)
                while (inetAddresses.hasMoreElements()) { // para cada endereço ip da placa
                    InetAddress ip = inetAddresses.nextElement();
                    if (isIpV4Address(ip) && !ip.isLoopbackAddress()) { // se não é um ipv6 e não é endereço de loopback
                        List<InterfaceAddress> interfaceAddresses = netCard.getInterfaceAddresses();
                        for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                            Inet4Address broadcast = (Inet4Address) interfaceAddress.getBroadcast();
                            if (broadcast != null) { // agora que já verifiquei tudo, posso adicionar à lista de endereços de broadcast
                                Sockets.addNewBroadcastAddress(broadcast);
                            }
                        }
                    }
                }
            }
            // passo 2: criados os sockets de broadcast, criar um server socket pra cada rede local e aguardar respostas do servidores
            //todo server socket não serve! ServerSocket é pra criar conexão e isso é UDP, que não é guiado por conexão. Preciso usar datagramSocket
            //ServerSocket socket = new ServerSocket(ClientPorts.getUdpPort()); // cria um server socket em uma porta para esperar as respostas
            for(Inet4Address broadcastAddr:Sockets.getAllBroadcastAddresses()){
                DatagramSocket socketBroad = new DatagramSocket(); // crio um socket de broadcast em qualquer porta aberta
                socketBroad.setBroadcast(true);
                socketBroad.send(PacketBuilder.buildSyn(broadcastAddr, ServerPorts.getServerUdpPort(), ClientPorts.getTcpPort(), ClientPorts.getUdpPort()));
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
