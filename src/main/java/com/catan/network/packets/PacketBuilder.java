package com.catan.network.packets;

import java.net.DatagramPacket;
import java.net.Inet4Address;

public class PacketBuilder {
    /**
     *
     * @param broadcast O endereço IP de broadcast da rede que vai receber o SYN
     * @param port A porta de destino dos servidores.
     * @param clientTcpPort A porta TCP que o cliente usará para receber as respostas
     * @param clientUdpPort A porta UDP que o cliente usará para receber as respostas
     * @return Um DatagramPacket SYN. Deve ser transmitido em broadcast.
     */
    public static DatagramPacket buildSyn(Inet4Address broadcast, int port, int clientTcpPort, int clientUdpPort){
        String message = Packets.getPacketName(Packets.SYN)+"\n"+clientTcpPort+"\n"+clientUdpPort+"\n";
        byte[] messageBytes = message.getBytes();
        return new DatagramPacket(messageBytes, messageBytes.length, broadcast, port);
    }
    public static DatagramPacket buildSynAck(Inet4Address ipCliente, int portaCliente, int portaServer){
        // todo construir o synack aqui
        return null;
    }

}
