package com.cmps115.public_defender;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by bryan on 5/3/17.
 */

public class GetNearby {

    public JSONObject makeRequest(JSONObject jsonRequest, URL url){
        HttpURLConnection conn = null;
        DataOutputStream out = null;
        JSONObject jsonResponse = new JSONObject();


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
        }
        // return error to user about unable to connect?
        finally {
            conn.disconnect();
            return jsonResponse;
        }

    }
}

