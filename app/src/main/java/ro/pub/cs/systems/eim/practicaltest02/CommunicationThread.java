package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;


public class CommunicationThread extends Thread {

    private final ServerThread serverThread;
    private final Socket socket;
    private final TextView keyValueTextView;

    // Constructor of the thread, which takes a ServerThread and a Socket as parameters
    public CommunicationThread(ServerThread serverThread, Socket socket, TextView keyValueTextView) {
        this.serverThread = serverThread;
        this.socket = socket;
        this.keyValueTextView = keyValueTextView;
    }

    // run() method: The run method is the entry point for the thread when it starts executing.
    // It's responsible for reading data from the client, interacting with the server,
    // and sending a response back to the client.
    @Override
    public void run() {
        // It first checks whether the socket is null, and if so, it logs an error and returns.
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            // Create BufferedReader and PrintWriter instances for reading from and writing to the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            String result = "none";
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (request) !");

            // Read the city and informationType values sent by the client
            String request = bufferedReader.readLine();
            if (request == null || request.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (request) !");
                return;
            }

            // Parse the request
            String[] requestInformation = request.split(",");
            if (requestInformation.length == 0) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (request) !");
                return;
            }

            String requestType = requestInformation[0];
            if (requestType == null || requestType.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (request type) !");
                return;
            }

            if (requestType.equals("get")) {
                if (requestInformation.length != 2) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (request type) !");
                    return;
                }

                String key = requestInformation[1];
                if (key == null || key.isEmpty()) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (key) !");
                    return;
                }

                String value = serverThread.getData().get(key);
                Long timeStamp = serverThread.getExpirationTime().get(key);
                Long currentTime = requestTime();

                // Unix timestamp is in seconds
                if (value != null && timeStamp != null && (currentTime - timeStamp <= 5)) {
                    result = value;
                }
            }

            if (requestType.equals("put")) {
                if (requestInformation.length != 3) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (request type) !");
                    return;
                }

                String key = requestInformation[1];
                String value = requestInformation[2];
                if (key == null || key.isEmpty()) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (key) !");
                    return;
                }

                if (value == null || value.isEmpty()) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (value) !");
                    return;
                }

                serverThread.setData(key, value);
                serverThread.setExpirationTime(key, requestTime());
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Data added to the store: " + key + " -> " + value);
                result = "success";
            }

            // Send the result back to the client
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException | JSONException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
            keyValueTextView.post(() -> keyValueTextView.setText(ioException.getMessage()));
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
                keyValueTextView.post(() -> keyValueTextView.setText(ioException.getMessage()));
            }
        }
    }

    private Long requestTime() throws IOException, JSONException {
        Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
        HttpClient httpClient = new DefaultHttpClient();
        String pageSourceCode = "";

        // make the HTTP request to the web service
        HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS);
        HttpResponse httpGetResponse = httpClient.execute(httpGet);
        HttpEntity httpGetEntity = httpGetResponse.getEntity();
        if (httpGetEntity != null) {
            pageSourceCode = EntityUtils.toString(httpGetEntity);
        }
        if (pageSourceCode == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
            throw new IOException("[COMMUNICATION THREAD] Error getting the information from the webservice!");
        } else Log.i(Constants.TAG, pageSourceCode);

        // Parse the page source code into a JSONObject and extract the needed information
        JSONObject content = new JSONObject(pageSourceCode);
        return content.getLong("unixtime");
    }
}
