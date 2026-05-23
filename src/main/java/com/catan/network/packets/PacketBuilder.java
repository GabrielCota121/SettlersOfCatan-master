package com.catan.network.packets;

import com.catan.GeneralConfig;
import com.catan.network.client.FoundServerInfo;
import com.catan.network.util.NetFunctions;
import com.catan.network.util.StringFunctions;

import java.net.DatagramPacket;
import java.net.Inet4Address;

public class PacketBuilder {
    /**
     *
     * @param broadcast O endereço IP de broadcast da rede que vai receber o SYN
     * @param port A porta de destino dos servidores.
     * @param clientUdpPort A porta UDP que o cliente usará para receber as respostas
     * @return Um DatagramPacket SYN. Deve ser transmitido em broadcast.
     */
    public static DatagramPacket buildSyn(Inet4Address broadcast, int port, int clientUdpPort){
        //ajustando tamanho das strings de porta pra que todos os pacotes tenham o mesmo tamanho
        String clientUdpPortString = NetFunctions.adjustPortStringSize(String.valueOf(clientUdpPort));
        // iso 8859-1 porque o tamanho do caractere é fixo e suporta acentos
        String message = Packets.getPacketName(Packets.SYN)+"\n"+clientUdpPortString+"\n";
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, broadcast, port);
    }
    // todo colocar a porta tcp que o cliente vai usar pra receber no connect em vez do syn

    /**
     *
     * @param ipCliente IP do cliente para o qual quero enviar uma resposta
     * @param portaCliente Porta de destino do cliente. UDP. Vem na mensagem do pacote SYN
     * @param portaTcpServer Porta TCP do server. Pode ser alterada. É a que vai ser usada para abrir um socket TCP no caso de uma conexão.
     * @param nomeServer Nome do servidor.
     * @param numeroJogadores Quantidade de jogadores humanos. Serve para saber se é possível entrar na partida.
     * @return Um pacote SYNACK destinado a responder um cliente em específico. Deve ser transmitido em UDP.
     */
    public static DatagramPacket buildSynAck(Inet4Address ipCliente, int portaCliente, int portaTcpServer, int portaUdpServer, String nomeServer, int numeroJogadores){
        //ajustar os tamanhos das strings. numero de jogadores não precisa porque tem apenas 1 dígito
        String portaTcpServerString = NetFunctions.adjustPortStringSize(String.valueOf(portaTcpServer));
        String portaUdpServerString = NetFunctions.adjustPortStringSize(String.valueOf(portaUdpServer));
        String nomeServerAjustado = NetFunctions.adjustTextStringSize(String.valueOf(nomeServer), GeneralConfig.maxServerNameSize);
        String message = Packets.getPacketName(Packets.SYNACK)+"\n"+portaTcpServerString+"\n"+nomeServerAjustado+"\n"+numeroJogadores+"\n"+portaUdpServerString;
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, ipCliente, portaCliente);
    }

    /**
     *
     * @return Uma mensagem Connect. Contém o identificador do pacote e o nome do player.
     */
    public static DatagramPacket buildConnect(FoundServerInfo server){
        String message =  Packets.getPacketName(Packets.CONNECT)+"\n"+NetFunctions.adjustTextStringSize(GeneralConfig.getPlayerName(), GeneralConfig.maxPlayerNameSize);
        byte[] messageBytes = StringFunctions.getStringBytesAsIso88591(message);
        return new DatagramPacket(messageBytes, messageBytes.length, server.getIp(), server.getPortaUdp());
    }

    public static String buildConAckMessage(){
        return null;
    }
}
