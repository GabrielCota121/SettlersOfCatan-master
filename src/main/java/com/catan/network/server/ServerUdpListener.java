package com.catan.network.server;

import com.catan.network.packets.PacketBuilder;
import com.catan.network.packets.Packets;
import com.catan.network.util.NetFunctions;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;

public class ServerUdpListener extends Thread{
    DatagramSocket socket = new DatagramSocket(ServerPorts.getServerUdpPort());
    DatagramSocket socketEnvio = new DatagramSocket(); // não entra porta aqui porque não vou escutar nesse socket
    private boolean stop = false;

    public ServerUdpListener() throws IOException {
    }

    public void setStop(boolean stop){ // quando for para parar de responder a syns
        this.stop = stop;
    }

    // essa thread vai escutar por syns. É parte do servidor. escuta na porta udp 25567
    @Override
    public void run(){
        System.out.println("SynListener started");
        byte[] buffer = new byte[50];
        while(!stop){
            try {
                // todo implementar cálculo de tamanho dinâmico
                DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length); // Lẽ o tamanho de um syn do buffer
                System.out.println("SUL: Aguardando recebimento de um pacote.");
                socket.receive(recievedPacket);
                System.out.println("SUL: Pacote recebido!");
                // gerar synack de resposta
                String[] messageLines = NetFunctions.getUdpPacketMessageLines(recievedPacket);
                // debug
                for(String line : messageLines){
                    System.out.println(line);
                }
                Inet4Address ipCliente = (Inet4Address) recievedPacket.getAddress();
                if(messageLines.length > 0){
                    if(messageLines[0].equals(Packets.getPacketName(Packets.SYN))){
                        System.out.println("SUL: Pacote é um SYN");
                        responderSyn(ipCliente, messageLines);
                    }else if(messageLines[0].equals(Packets.getPacketName(Packets.CONNECT))){
                        System.out.println("SUL: Pacote é um CONNECT");

                    }
                }
            } catch (SocketException e) {
                // todo acertar exceptions depois
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private void responderSyn(Inet4Address ipCliente, String[] messageLines) throws IOException {
        int portaUdpDestino = Integer.parseInt(messageLines[1]);
        DatagramPacket synAck = PacketBuilder.buildSynAck(ipCliente, portaUdpDestino, ServerData.getNomeServer(), ServerData.getNumeroJogadores());
        socketEnvio.send(synAck);
    }
    private void responderConnect(Inet4Address ipCliente, String[] messageLines) throws IOException {

    }

}
