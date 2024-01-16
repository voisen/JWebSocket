package com.jiand.libwebsocket.callback;

import com.jiand.libwebsocket.JWebsocketClient;
import com.jiand.libwebsocket.ex.JWebsocketException;

import org.jetbrains.annotations.NotNull;

/**
 * @author jiand
 */
public interface IWebsocketCallback {
    void onClosed(JWebsocketClient webSocket, int code, String reason);

    default void onClosing(JWebsocketClient webSocket, int code, String reason){}

    void onFailure(JWebsocketClient webSocket, JWebsocketException t);

    void onMessage(JWebsocketClient webSocket, String text);

    default void onMessage(JWebsocketClient webSocket, byte[] bytes){}

    void onOpen(JWebsocketClient webSocket);
}
