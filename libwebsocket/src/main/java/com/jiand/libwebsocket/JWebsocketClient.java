package com.jiand.libwebsocket;


import com.jiand.libwebsocket.callback.IWebsocketCallback;
import com.jiand.libwebsocket.ex.JWebsocketException;

import java.util.Map;

/**
 * @author jiand
 */
public abstract class JWebsocketClient {

    /**
     * 连接到服务器
     */
    public abstract void connect() throws JWebsocketException;

    /**
     * 断开连接
     */
    public abstract void disconnect() throws JWebsocketException;

    public abstract void send(String text) throws JWebsocketException;

    public abstract void send(byte[] bytes) throws JWebsocketException;

    /**
     * 是否连接成功
     */
    public abstract boolean isConnected();

    /**
     * 是否正在连接
     */
    public abstract boolean isConnecting();

    /**
     * 释放资源
     */
    public abstract void release();


    public static class Creator {
        protected boolean retryOnConnectionFailure = true;
        protected long pingInterval = 0;
        protected boolean enableAutoReconnect = false;
        protected long reconnectInterval = 20000;
        protected final String url;
        protected IWebsocketCallback callback;

        protected Map<String, String> headers;

        protected long connectTimeout = 20000;
        protected long readTimeout = 20000;
        protected long writeTimeout = 20000;

        public Creator(String url, IWebsocketCallback callback) {
            this.url = url;
            this.callback = callback;
        }

        public Creator enableAutoReconnect(boolean enableAutoReconnect) {
            this.enableAutoReconnect = enableAutoReconnect;
            return this;
        }

        public Creator enableAutoReconnect(boolean enableAutoReconnect, long reconnectInterval) {
            this.enableAutoReconnect = enableAutoReconnect;
            if (enableAutoReconnect && reconnectInterval <= 0){
                throw new IllegalArgumentException("连接间隔错误， 请重新设置（间隔必须大于0）");
            }
            this.reconnectInterval = reconnectInterval;
            return this;
        }

        public Creator connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Creator readTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Creator writeTimeout(long writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Creator pingInterval(long pingInterval) {
            this.pingInterval = pingInterval;
            return this;
        }

        public Creator headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Creator retryOnConnectionFailure(boolean retryOnConnectionFailure) {
            this.retryOnConnectionFailure = retryOnConnectionFailure;
            return this;
        }

        public JWebsocketClient create() {
            return new JWebsocketClientImpl(this);
        }

        public JWebsocketClient connect() throws JWebsocketException {
            JWebsocketClientImpl jWebsocketClient = new JWebsocketClientImpl(this);
            jWebsocketClient.connect();
            return jWebsocketClient;
        }

    }

}
