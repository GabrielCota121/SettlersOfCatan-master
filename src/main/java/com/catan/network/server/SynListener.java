package com.catan.network.server;

import com.catan.network.SocketConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class SynListener extends Thread{
    // todo todo o resto do código do servidor será feito em uma única thread. Vai ter uma pra conexão TCP e essa, pra receber e responder pacotes udp
    DatagramSocket socket = new DatagramSocket(SocketConfig.getServerUdpPort());
    private boolean stop = false;

    public SynListener() throws IOException {
    }

    public void setStop(boolean stop){ // quando for para parar de responder a syns
        this.stop = stop;
    }

    // essa thread vai escutar por syns. É parte do servidor. escuta na porta udp 25567
    @Override
    public void run(){
        byte[] buffer = new byte[1024]; // todo investigar qual o tamanho correto do buffer. Por enquanto, coloquei 1024
        while(!stop){
            try {
                DatagramSocket recieveSocket = new DatagramSocket(SocketConfig.getServerUdpPort());
                DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length); // o buffer tem o tamanho de um pacote... E se mais de um cliente mandar um pacote de uma vez?
                socket.receive(recievedPacket);
                //String message =

            } catch (SocketException e) {
                // todo acertar exceptions depois
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
