package com.catan.network.exception;

import com.catan.GeneralConfig;

public class NomeServerMuitoLongoException extends Exception {
    public NomeServerMuitoLongoException() {
        super("O nome do server deve ter, no máximo, "+ GeneralConfig.maxServerNameSize+" caracteres.");
    }
}
