package com.catan.network.exception;

import com.catan.GeneralConfig;

public class NomePlayerMuitoLongoException extends Exception {
    public NomePlayerMuitoLongoException() {
        super("O nome do player deve ter, no máximo, "+ GeneralConfig.maxPlayerNameSize+" caracteres.");
    }
}
