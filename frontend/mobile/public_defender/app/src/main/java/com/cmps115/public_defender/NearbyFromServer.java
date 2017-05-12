package com.cmps115.public_defender;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by bryan on 5/11/17.
 */

public class NearbyFromServer {
    JSONObject jsonResponse = null;
    JSONObject jsonRequest = null;
    URL url = null;
    Thread nearbyThread = null;

    public NearbyFromServer(double geo[], int dist, URL u){
        nearbyThread = new Thread(new Runnable() {
            public void run() {
                getNearbyEvents();
            }
        });
        url = u;
        // create json
        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("current_location", String.format("(%f, %f)", geo[1], geo[0])); //have to reverse them
            jsonRequest.put("distance", dist);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("[NEARBY]", jsonRequest.toString());
    }

    public JSONObject getEvents(){
        Log.d("[NEARBY]", "getEvents");
        nearbyThread.start();
        try {
            nearbyThread.join(); //wait for response.... TODO: temporary. Make async updates
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("[NEARBY-RESPONSE]", jsonResponse.toString());
        return jsonResponse;
    }

    private void getNearbyEvents() {
        Log.d("[NEARBY]", "getNearbyEvents");
        HttpURLConnection conn = null;
        DataOutputStream out = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setFixedLengthStreamingMode(jsonRequest.toString().getBytes().length);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000); //set timeout to 5 seconds

            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(jsonRequest.toString());
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            jsonResponse = new JSONObject(response.toString());
            Log.d("[NEARBY--end]", response.toString());
        }
        catch (java.net.SocketTimeoutException e){
            // TODO: handle connection issues.
            return;
        }
        // return error to user about unable to connect?
        catch (IOException e) {
            e.printStackTrace();
            return;
        }

        finally {
            conn.disconnect();
            return;
        }
    }
}
