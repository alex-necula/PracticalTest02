package ro.pub.cs.systems.eim.practicaltest02;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


public class ClientThread extends Thread {

    private final String address;
    private final int port;
    private final String key;
    private final String value;
    private final String request;
    private final TextView keyValueTextView;

    private Socket socket;

    public ClientThread(String address, int port, String key, String value, String request, TextView keyValueTextView) {
        this.address = address;
        this.port = port;
        this.key = key;
        this.value = value;
        this.request = request;
        this.keyValueTextView = keyValueTextView;
    }

    public ClientThread(String address, int port, String key, String request, TextView keyValueTextView) {
        this.address = address;
        this.port = port;
        this.key = key;
        this.value = null;
        this.request = request;
        this.keyValueTextView = keyValueTextView;
    }

    @Override
    public void run() {
        try {
            // tries to establish a socket connection to the server
            socket = new Socket(address, port);

            // gets the reader and writer for the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);

            if (request.equals("get")) {
                // sends the key to the server
                printWriter.println(request + "," + key);
                printWriter.flush();

            } else if (request.equals("put")) {
                // sends the key and value to the server
                printWriter.println(request + "," + key + "," + value);
                printWriter.flush();
            }

            String value;

            // reads the value from the server
            while ((value = bufferedReader.readLine()) != null) {
                final String finalizedValue = value;

                // updates the UI with the value. This is done using post() method to ensure it is executed on UI thread
                keyValueTextView.post(() -> keyValueTextView.setText(finalizedValue));
            }

        } // if an exception occurs, it is logged
        catch (IOException ioException) {
            Log.e(Constants.TAG, "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
            keyValueTextView.post(() -> keyValueTextView.setText(ioException.getMessage()));
        } finally {
            if (socket != null) {
                try {
                    // closes the socket regardless of errors or not
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                    keyValueTextView.post(() -> keyValueTextView.setText(ioException.getMessage()));
                }
            }
        }
    }

}
