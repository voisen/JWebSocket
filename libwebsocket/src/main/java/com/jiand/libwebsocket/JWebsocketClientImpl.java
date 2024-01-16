package com.jiand.libwebsocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jiand.libwebsocket.ex.JWebsocketException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

final class JWebsocketClientImpl extends JWebsocketClient{

    private static final String TAG = "JWebsocketClient";
    private final Creator mCreator;
    private final OkHttpClient mOkHttpClient;

    private final WebsocketEventListener mWebsocketEventListener = new WebsocketEventListener();

    private WebSocket mWebSocket;

    private Handler mHandler;

    private final Thread handerThread;

    private final AtomicBoolean released = new AtomicBoolean(false);

    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean disconnectByUser = new AtomicBoolean(false);

    @SuppressWarnings("ALL")
    public JWebsocketClientImpl(Creator creator) {
        mCreator = creator;
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(mCreator.readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(mCreator.writeTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(mCreator.connectTimeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(mCreator.retryOnConnectionFailure)
                .pingInterval(mCreator.pingInterval, TimeUnit.MILLISECONDS)
                .build();
        if (mCreator.enableAutoReconnect){
            handerThread = new Thread(()->{
                Looper.prepare();
                Looper myLooper = Looper.myLooper();
                if (myLooper == null){
                    myLooper = Looper.getMainLooper();
                }
                mHandler = new Handler(myLooper);
                Looper.loop();
            });
            handerThread.start();
        }else{
            mHandler = null;
            handerThread = null;
        }
    }


    private void afterClose(){
        if (mCreator.enableAutoReconnect && mHandler != null && !disconnectByUser.get()){
            Log.i(TAG, "afterClose: 尝试重新连接...");
            mHandler.postDelayed(() -> {
                try {
                    connect_();
                } catch (JWebsocketException e) {
                    e.printStackTrace();
                    afterClose();
                }
            }, mCreator.reconnectInterval);
        }
    }

    @Override
    public void connect() throws JWebsocketException{
        connect_();
        disconnectByUser.set(false);
    }

    private synchronized void connect_() throws JWebsocketException{
        checkReleased();
        if (isConnected.get() || isConnecting.get()){
            Log.i(TAG, "connect: 服务已连接或者正在连接中.");
            return;
        }
        Request.Builder builder = new Request.Builder()
                .url(mCreator.url);
        if (mCreator.headers != null) {
            for (String key : mCreator.headers.keySet()){
                String s = mCreator.headers.get(key);
                if (s == null) {
                    continue;
                }
                builder.addHeader(key, s);
            }
        }
        isConnecting.set(true);
        mWebSocket = mOkHttpClient.newWebSocket(builder.build(), mWebsocketEventListener);
    }

    @Override
    public void send(String text) throws JWebsocketException {
        checkReleased();
        if (mWebSocket == null){
            throw new JWebsocketException("请检查您的WebSocket连接是否已经建立。");
        }
        mWebSocket.send(text);
    }

    @Override
    public synchronized void send(byte[] bytes) throws JWebsocketException {
        checkReleased();
        if (mWebSocket == null){
            throw new JWebsocketException("请检查您的WebSocket连接是否已经建立。");
        }
        boolean send = mWebSocket.send(ByteString.of(bytes));
        if (!send){
            throw new JWebsocketException("发送数据失败。");
        }
    }

    @Override
    public synchronized void disconnect() throws JWebsocketException {
        disconnect_();
        mHandler.removeCallbacksAndMessages(null);
        disconnectByUser.set(true);
    }

    private void disconnect_()  throws JWebsocketException {
        checkReleased();
        if (mWebSocket != null){
            mWebSocket.cancel();
            mWebSocket.close(1000, "closed by client");
            mWebSocket = null;
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected.get();
    }

    @Override
    public boolean isConnecting() {
        return isConnecting.get();
    }

    @Override
    public synchronized void release() {
        if (mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
            mHandler = null;
        }
        if (handerThread != null){
            handerThread.interrupt();
            try {
                handerThread.join(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            disconnect_();
        }catch (Exception e){
            e.printStackTrace();
        }
        released.set(true);
    }

    private void checkReleased() throws JWebsocketException {
        if (released.get()){
            throw new JWebsocketException("当前websocket资源已释放，无法再次使用。");
        }
    }

    class WebsocketEventListener extends WebSocketListener{
        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            Log.i(TAG, "onClosed: " + code + " " + reason);
            isConnected.set(false);
            isConnecting.set(false);
            if (mCreator.callback != null) {
                mCreator.callback.onClosed(JWebsocketClientImpl.this, code, reason);
            }
            afterClose();
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosing(webSocket, code, reason);
            Log.i(TAG, "onClosing: " + code + " " + reason);
            mWebSocket.close(code, reason);
            isConnecting.set(false);
            isConnected.set(false);
            if (mCreator.callback != null) {
                mCreator.callback.onClosing(JWebsocketClientImpl.this, code, reason);
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            Log.e(TAG, "onFailure: " + t.getMessage(), t);
            isConnected.set(false);
            isConnecting.set(false);
            if (mCreator.callback != null) {
                mCreator.callback.onFailure(JWebsocketClientImpl.this, new JWebsocketException(t));
            }
            afterClose();
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
            Log.i(TAG, "onMessage: " + text);
            if (mCreator.callback != null) {
                mCreator.callback.onMessage(JWebsocketClientImpl.this, text);
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
            Log.i(TAG, "onMessage: " + bytes.size());
            if (mCreator.callback != null) {
                mCreator.callback.onMessage(JWebsocketClientImpl.this, bytes.toByteArray());
            }
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);
            Log.i(TAG, "onOpen: " + response.request().url());
            isConnected.set(true);
            isConnecting.set(false);
            if (mCreator.callback != null) {
                mCreator.callback.onOpen(JWebsocketClientImpl.this);
            }
        }
    }

}
