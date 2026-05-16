package com.catan.network.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class SynListener extends Thread{
    // todo essa é a thread que responde a pacotes udp. O único pacote udp que o servidor recebe é syn
    DatagramSocket socket = new DatagramSocket(ServerPorts.getServerUdpPort());
    private boolean stop = false;

    public SynListener() throws IOException {
    }

    public void setStop(boolean stop){ // quando for para parar de responder a syns
        this.stop = stop;
    }

    // essa thread vai escutar por syns. É parte do servidor. escuta na porta udp 25567
    @Override
    public void run(){
        System.out.println("SynListener started");
        byte[] buffer = new byte[1024]; // todo investigar qual o tamanho correto do buffer. Por enquanto, coloquei 1024
        while(!stop){
            try {
                DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length); // o buffer tem o tamanho de um pacote... E se mais de um cliente mandar um pacote de uma vez?
                socket.receive(recievedPacket);
                String message = new String(recievedPacket.getData(), 0, recievedPacket.getLength(), StandardCharsets.ISO_8859_1);
                //debug
                System.out.println(message);

            } catch (SocketException e) {
                // todo acertar exceptions depois
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
