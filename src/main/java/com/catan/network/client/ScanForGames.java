package com.catan.network.client;

import com.catan.network.Sockets;
import com.catan.network.packets.PacketBuilder;
import com.catan.network.server.ServerPorts;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
            DatagramSocket socketUdpRecebimento = new DatagramSocket(ClientPorts.getUdpPort()); // preparo um socket para receber os synacks
            socketUdpRecebimento.setSoTimeout(1000); // vou esperar apenas 1s antes de desistir e passar para a próxima rede
            byte[] buffer = new byte[1024]; // o buffer que vou usar para receber os synacks. Posso receber uma pancada de uma única rede
            for(Inet4Address broadcastAddr:Sockets.getAllBroadcastAddresses()){
                System.out.println("Cliente enviará um Syn para "+broadcastAddr.toString());
                DatagramSocket socketBroad = new DatagramSocket(); // crio um socket de broadcast em qualquer porta aberta
                socketBroad.setBroadcast(true);
                socketBroad.send(PacketBuilder.buildSyn(broadcastAddr, ServerPorts.getServerUdpPort(), ClientPorts.getUdpPort()));
                System.out.println("Enviado");
                // enviado, aguardar respostas aqui.
                // lança socketTimeoutException se o tempo expirar
                boolean timedOut = false;
                while(!timedOut){ // enquanto não der um timeout, significa que estou recebendo respostas
                    try{
                        System.out.println("Esperando respostas da rede "+broadcastAddr.toString());
                        DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length); // todo verificar tamanho do pacote pra ler somente o necessário do buffer
                        socketUdpRecebimento.receive(recievedPacket);
                        System.out.println("recebi uma resposta");
                        String message = new String(recievedPacket.getData(), 0, recievedPacket.getLength(), StandardCharsets.ISO_8859_1);                        //debug
                        System.out.println(message);
                    }catch(SocketTimeoutException e){
                        System.out.println("Deu timeout, sem respostas nessa rede por 1 segundo.");
                        timedOut = true;
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
