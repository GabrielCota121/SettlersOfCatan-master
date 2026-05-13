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

    /**
     *
     * @param ipCliente IP do cliente para o qual quero enviar uma resposta
     * @param portaCliente Porta de destino do cliente. UDP. Vem na mensagem do pacote SYN
     * @param portaTcpServer Porta TCP do server. Pode ser alterada. É a que vai ser usada para abrir um socket TCP no caso de uma conexão.
     * @param nomeServer Nome do servidor.
     * @param numeroJogadores Quantidade de jogadores humanos. Serve para saber se é possível entrar na partida.
     * @return Um pacote SYNACK destinado a responder um cliente em específico. Deve ser transmitido em UDP.
     */
    public static DatagramPacket buildSynAck(Inet4Address ipCliente, int portaCliente, int portaTcpServer, String nomeServer, int numeroJogadores){
        String message = Packets.getPacketName(Packets.SYNACK)+"\n"+portaTcpServer+"\n"+nomeServer+"\n"+numeroJogadores;
        byte[] messageBytes = message.getBytes();
        return new DatagramPacket(messageBytes, messageBytes.length, ipCliente, portaCliente);
    }

}
