package com.catan.network.exception;

import com.catan.network.client.FoundServerInfo;

public class ConexaoRecusadaException extends Exception {
    public ConexaoRecusadaException(FoundServerInfo foundServerInfo) {
        super("O servidor "+foundServerInfo.getNome()+" recusou a conexão.");
    }
}
