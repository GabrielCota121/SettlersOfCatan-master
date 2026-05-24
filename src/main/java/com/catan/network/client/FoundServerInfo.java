package com.catan.network.client;

import java.net.Inet4Address;

public class FoundServerInfo {
    private String nome;
    private int quantidadeJogadores;
    private Inet4Address ip;

    public FoundServerInfo(String nome, int quantidadeJogadores, Inet4Address ip) {
        setNome(nome);
        setQuantidadeJogadores(quantidadeJogadores);
        setIp(ip);
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

    @Override
    public String toString() {
        return getNome()+", "+getIp();
    }
}
