package com.jiand.libwebsocket.ex;

/**
 * @author jiand
 */
public class JWebsocketException extends Exception{
    public JWebsocketException(String message) {
        super(message);
    }

    public JWebsocketException(Throwable cause) {
        super(cause);
    }
}
