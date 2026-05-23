package com.catan.network.client;

import com.catan.network.packets.PacketBuilder;
import com.catan.network.server.ServerPorts;
import com.catan.network.util.NetFunctions;
import com.catan.network.util.StringFunctions;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static com.catan.network.util.NetFunctions.isIpV4Address;

public class ScanForGames extends Thread {
    DatagramSocket socketUdpRecebimento = UdpSocket.getUdpSocket();

    public ScanForGames() throws SocketException {
    }

    public void scan(){
        start();
    }
    @Override
    public void run() {
        ArrayList<Inet4Address> broadcastAddresses = new ArrayList<>();
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
                                broadcastAddresses.add(broadcast);
                            }
                        }
                    }
                }
            }
            // passo 2: criados os sockets de broadcast, criar um server socket pra cada rede local e aguardar respostas do servidores

            socketUdpRecebimento.setSoTimeout(1000); // vou esperar apenas 1s antes de desistir e passar para a próxima rede
            byte[] buffer = new byte[1024]; // o buffer que vou usar para receber os synacks. Posso receber uma pancada de uma única rede
            for(Inet4Address broadcastAddr:broadcastAddresses){
                System.out.println("SFG: Cliente enviará um Syn para "+broadcastAddr.toString());
                DatagramSocket socketBroad = new DatagramSocket(); // crio um socket de broadcast em qualquer porta aberta
                socketBroad.setBroadcast(true);
                socketBroad.send(PacketBuilder.buildSyn(broadcastAddr, ServerPorts.getServerUdpPort(), ClientPorts.getUdpPort()));
                System.out.println("SFG: Enviado");
                // enviado, aguardar respostas aqui.
                // lança socketTimeoutException se o tempo expirar
                boolean timedOut = false;
                while(!timedOut){ // enquanto não der um timeout, significa que estou recebendo respostas
                    try{
                        System.out.println("SFG: Esperando respostas da rede "+broadcastAddr.toString());
                        DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length); // todo verificar tamanho do pacote pra ler somente o necessário do buffer
                        socketUdpRecebimento.receive(recievedPacket);
                        System.out.println("SFG: recebi uma resposta");
                        String[] message = NetFunctions.getUdpPacketMessageLines(recievedPacket);
                        //debug
                        for(String line : message){
                            System.out.println(line);
                        }
                        // adiciona os dados do servidor encontrado a uma lista estática de servidores encontrados
                        System.out.println("Debug... Porta de origem do pacote igual à porta udp do server? "+(recievedPacket.getPort()==ServerPorts.getServerUdpPort()));
                        FoundServers.addServer(message[2], Integer.parseInt(message[3]), (Inet4Address)recievedPacket.getAddress(), Integer.parseInt(message[1]), Integer.parseInt(message[4]));
                    }catch(SocketTimeoutException e){
                        System.out.println("SFG: Deu timeout, sem respostas nessa rede por 1 segundo.");
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
