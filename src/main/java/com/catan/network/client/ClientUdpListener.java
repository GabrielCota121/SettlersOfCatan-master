package com.catan.network.client;

import com.catan.network.packets.Packets;
import com.catan.network.util.NetFunctions;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientUdpListener extends Thread {
    private boolean stop = false;
    @Override
    public void run() {
        try{
            DatagramSocket recievingSocket = UdpSocket.getUdpSocket();
            recievingSocket.setSoTimeout(0);
            byte[] buffer = new byte[1024];
            while(!stop){
                System.out.println("CUL: Estou aguardando um pacote");
                DatagramPacket recievedPacket = new DatagramPacket(buffer, buffer.length);
                recievingSocket.receive(recievedPacket);
                System.out.println("CUL: recebi um pacote");
                String[] message = NetFunctions.getUdpPacketMessageLines(recievedPacket);
                if(message.length > 0 && message[0].equals(Packets.getPacketName(Packets.CONACK))){
                    System.out.println("CUL: é um conack");
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
