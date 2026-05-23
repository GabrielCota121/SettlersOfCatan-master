package com.catan.network.client;

import java.net.Inet4Address;

public class FoundServerInfo {
    private String nome;
    private int quantidadeJogadores;
    private Inet4Address ip;
    private int portaTcp;
    private int portaUdp;

    public FoundServerInfo(String nome, int quantidadeJogadores, Inet4Address ip, int portaTcp, int portaUdp) {
        setNome(nome);
        setQuantidadeJogadores(quantidadeJogadores);
        setIp(ip);
        setPortaTcp(portaTcp);
        setPortaUdp(portaUdp);
    }
    public void setNome(String nome){
        this.nome = nome.substring(0, nome.indexOf("\0")); // tira os /0 do final
    }
    public String getNome(){
        return this.nome;
    }

    public int getQuantidadeJogadores() {
        return quantidadeJogadores;
    }

    public void setQuantidadeJogadores(int quantidadeJogadores) {
        this.quantidadeJogadores = quantidadeJogadores;
    }

    public Inet4Address getIp() {
        return ip;
    }

    public void setIp(Inet4Address ip) {
        this.ip = ip;
    }

    public int getPortaTcp() {
        return portaTcp;
    }

    public int getPortaUdp() {
        return portaUdp;
    }

    public void setPortaTcp(int portaTcp) {
        this.portaTcp = portaTcp;
    }
    public void setPortaUdp(int portaUdp) {
        this.portaUdp = portaUdp;
    }
}
