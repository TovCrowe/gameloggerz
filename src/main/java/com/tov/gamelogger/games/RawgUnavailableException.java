package com.tov.gamelogger.games;

public class RawgUnavailableException extends RuntimeException {

    public RawgUnavailableException(String message) {
        super(message);
    }

    public RawgUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
