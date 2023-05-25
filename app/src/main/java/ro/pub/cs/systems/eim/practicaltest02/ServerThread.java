package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServerThread extends Thread {

    private ServerSocket serverSocket = null;

    private final HashMap<String, String> data;
    private final HashMap<String, Long> expirationTime;
    private final TextView keyValueTextView;

    public ServerThread(int port, TextView keyValueTextView) {
        Log.i(Constants.TAG, "[SERVER THREAD] Staring the server thread, port " + port);
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        }
        this.data = new HashMap<>();
        this.expirationTime = new HashMap<>();
        this.keyValueTextView = keyValueTextView;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public synchronized void setData(String key, String value) {
        this.data.put(key, value);
    }

    public synchronized void setExpirationTime(String key, Long value) {
        this.expirationTime.put(key, value);
    }

    public synchronized HashMap<String, String> getData() {
        return data;
    }

    public synchronized HashMap<String, Long> getExpirationTime() {
        return expirationTime;
    }

    @Override
    public void run() {
        try {
            // when running, they continuously check if the current thread is interrupted
            while (!Thread.currentThread().isInterrupted()) {
                Log.i(Constants.TAG, "[SERVER THREAD] Waiting for a client invocation...");
                // accept() method blocks the execution until a client connects to the server

                Socket socket = serverSocket.accept();
                Log.i(Constants.TAG, "[SERVER THREAD] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());

                // create a new CommunicationThread object for each client that connects to the server
                CommunicationThread communicationThread = new CommunicationThread(this, socket, keyValueTextView);
                communicationThread.start();
            }
        } catch (IOException clientProtocolException) {
            Log.e(Constants.TAG, "[SERVER THREAD] An exception has occurred: " + clientProtocolException.getMessage());
            if (Constants.DEBUG) {
                clientProtocolException.printStackTrace();
            }
        }
    }

    // when stopping, they interrupt the current thread and close the server socket. It is called in onDestroy() method from the MainActivity class
    public void stopThread() {
        interrupt();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[SERVER THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
