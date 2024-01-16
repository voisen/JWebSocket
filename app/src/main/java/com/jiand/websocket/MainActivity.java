package com.jiand.websocket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jiand.libwebsocket.JWebsocketClient;
import com.jiand.libwebsocket.callback.IWebsocketCallback;
import com.jiand.libwebsocket.ex.JWebsocketException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiand
 */
public class MainActivity extends AppCompatActivity implements IWebsocketCallback {

    private JWebsocketClient mWebsocketClient;
    private TextView mTextView;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.tv_status);
    }

    private void sendMessage(){
        try {
            mHandler.removeCallbacksAndMessages(null);
            if (mWebsocketClient != null && mWebsocketClient.isConnected()){
                mWebsocketClient.send("发送一条消息" + System.currentTimeMillis());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void connectToServer(View view) {
        if (mWebsocketClient != null){
            try {
                mWebsocketClient.connect();
            } catch (JWebsocketException e) {
                e.printStackTrace();
            }
            return;
        }
        mTextView.setText("连接中...");
        mWebsocketClient = new JWebsocketClient.Creator("ws://127.0.0.1", this)
                .connectTimeout(10000) //连接超时时间
                .readTimeout(10000) //读取超时时间
                .writeTimeout(10000) //写入超时时间
                .pingInterval(10000) //心跳间隔时间
                .enableAutoReconnect(true) //是否自动重连
                .enableAutoReconnect(true, 20000) //重连间隔时间
                .retryOnConnectionFailure(true) //是否重连失败后重新连接
                .create();
        try {
            mWebsocketClient.connect();
        } catch (JWebsocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClosed(JWebsocketClient webSocket, int code, String reason) {
        mTextView.setText("连接已关闭");
    }

    @Override
    public void onFailure(JWebsocketClient webSocket, JWebsocketException t) {
        mTextView.setText("连接失败");
    }

    @Override
    public void onMessage(JWebsocketClient webSocket, String text) {
        mTextView.setText(text);
    }

    @Override
    public void onOpen(JWebsocketClient webSocket) {
        mTextView.setText("连接成功");
        sendMessage();
    }

    public void disconnectServer(View view) {
        if (mWebsocketClient != null){
            try {
                mWebsocketClient.disconnect();
            } catch (JWebsocketException e) {
                e.printStackTrace();
            }
        }
    }
}