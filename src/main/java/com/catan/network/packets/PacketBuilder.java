package com.catan.network.packets;

import com.catan.GeneralConfig;
import com.catan.network.client.FoundServerInfo;
import com.catan.network.server.ServerPorts;
import com.catan.network.util.NetFunctions;
import com.catan.network.util.StringFunctions;

import java.net.DatagramPacket;
import java.net.Inet4Address;

public class PacketBuilder {
    /**
     *
     * @param broadcast O endereço IP de broadcast da rede que vai receber o SYN
     * @param clientUdpPort A porta UDP que o cliente usará para receber as respostas
     * @return Um DatagramPacket SYN. Deve ser transmitido em broadcast.
     */
    public static DatagramPacket buildSyn(Inet4Address broadcast, int clientUdpPort){
        //ajustando tamanho das strings de porta pra que todos os pacotes tenham o mesmo tamanho
        String clientUdpPortString = NetFunctions.adjustPortStringSize(String.valueOf(clientUdpPort));
        // iso 8859-1 porque o tamanho do caractere é fixo e suporta acentos
        String message = Packets.getPacketName(Packets.SYN)+"\n"+clientUdpPortString;
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, broadcast, ServerPorts.getServerUdpPort());
    }

    /**
     *
     * @param ipCliente IP do cliente para o qual quero enviar uma resposta
     * @param portaCliente Porta de destino do cliente. UDP. Vem na mensagem do pacote SYN
     * @param nomeServer Nome do servidor.
     * @param numeroJogadores Quantidade de jogadores humanos. Serve para saber se é possível entrar na partida.
     * @return Um pacote SYNACK destinado a responder um cliente em específico. Deve ser transmitido em UDP.
     */
    public static DatagramPacket buildSynAck(Inet4Address ipCliente, int portaCliente, String nomeServer, int numeroJogadores){
        //ajustar os tamanhos das strings. numero de jogadores não precisa porque tem apenas 1 dígito
        String nomeServerAjustado = NetFunctions.adjustTextStringSize(String.valueOf(nomeServer), GeneralConfig.maxServerNameSize);
        String message = Packets.getPacketName(Packets.SYNACK)+"\n"+nomeServerAjustado+"\n"+numeroJogadores;
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, ipCliente, portaCliente);
    }

    /**
     *
     * @return Uma mensagem Connect. Contém o identificador do pacote e o nome do player.
     */
    public static DatagramPacket buildConnect(FoundServerInfo server, int clientUdpPort){
        String  clientUdpPortString = NetFunctions.adjustPortStringSize(String.valueOf(clientUdpPort));
        String message =  Packets.getPacketName(Packets.CONNECT)+"\n"+NetFunctions.adjustTextStringSize(GeneralConfig.getPlayerName(), GeneralConfig.maxPlayerNameSize)+"\n"+clientUdpPortString;
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, server.getIp(), ServerPorts.getServerUdpPort());
    }
    public static DatagramPacket buildError(Inet4Address ipDestino, int portaDestino, String message){
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, ipDestino, portaDestino);
    }
    public static DatagramPacket buildConAck(Inet4Address ipCliente, int portaCliente, String cor){
        // todo montar o pacote
        return null;
    }
}
