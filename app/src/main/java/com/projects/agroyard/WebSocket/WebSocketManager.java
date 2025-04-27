package com.projects.agroyard.WebSocket;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private OkHttpClient client;
    private WebSocket webSocket;
    private Context context;
    private String serverUrl;
    private WebSocketListenerCallback callback;

    public WebSocketManager(@NonNull Context context, @NonNull String serverUrl, @NonNull WebSocketListenerCallback callback) {
        this.context = context;
        this.serverUrl = serverUrl;
        this.callback = callback;
    }

    public void start() {
        client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        webSocket = client.newWebSocket(request, new AppWebSocketListener());
    }

    public void stop() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing connection");
            webSocket = null;
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }

    private class AppWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "WebSocket Connected");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(TAG, "Received message: " + text);

            if (context instanceof Activity && !((Activity) context).isFinishing()) {
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Bid is Active Now.", Toast.LENGTH_SHORT).show();
                    callback.onBidStatusUpdated();
                });
            } else {
                Log.w(TAG, "Context invalid, skipping UI update.");
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            Log.d(TAG, "WebSocket Closing: " + code + " / " + reason);
            webSocket.close(1000, null);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.e(TAG, "WebSocket Error: " + t.getMessage());
        }
    }

    public interface WebSocketListenerCallback {
        void onBidStatusUpdated();
    }
}
