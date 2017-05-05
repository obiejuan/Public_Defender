package com.cmps115.public_defender;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bryan on 4/30/17.
 * Streams data to server.
 */

public class StreamToServer {
    Thread streamToServThread = null;
    Thread initStreamThread = null;
    PDAudioRecordingManager rec = null;
    boolean isStreaming = false;
    URL url = null;
    Context context = null;
    JSONObject jsonResponse = null;
    /*
        Pass in a recorder object, the urlstring and the current context
    */
    StreamToServer(PDAudioRecordingManager recorder, String urlString, Context c){
        streamToServThread = new Thread(new Runnable() {
            public void run() {
                streamToServer();
            }
        });
        initStreamThread = new Thread(new Runnable() {
            public void run() {
                initStream();
            }
        });
        rec = recorder;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        context = c;
    }
    /*
        stop streaming audio
    */
    public void stopStreamAudio() {
        if (rec != null) {
            rec.stopRecording();
            isStreaming = false;
            streamToServThread.interrupt();
        }
    }
    /*
        start streaming audio
    */
    public void startStreamAudio(){
        initStreamThread.start();
        try {
            initStreamThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("[START-STREAM-AUDIO]", jsonResponse.toString());
        try {
            url = new URL(url.toString() + jsonResponse.get("upload_token").toString());
            Log.d("STRING", url.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        isStreaming = true;
        streamToServThread.start();

    }

    private void initStream() {
        HttpURLConnection conn = null;
        DataOutputStream out = null;
        JSONObject j = new JSONObject();
        try {
            j.put("location", "(-97.515678, 35.512363)");
            j.put("user", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("[STREAM]", url.toString());
        Log.d("[STREAM]", j.toString());

        try {
            conn = (HttpURLConnection) url.openConnection();
            Log.d("[STREAM]", "1");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setFixedLengthStreamingMode(j.toString().getBytes().length);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            Log.d("[STREAM]", "2");
            out = new DataOutputStream(conn.getOutputStream());
            Log.d("[STREAM]", "3");
            out.writeBytes(j.toString());
            out.flush();
            out.close();
            Log.d("[STREAM]", "4");
            int responseCode = conn.getResponseCode();
            Log.d("[STREAM]", String.valueOf(responseCode));
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            Log.d("[STREAM]", conn.getResponseMessage());
            Log.d("[STREAM]", response.toString());
            jsonResponse = new JSONObject(response.toString());
            //resp = in.readUTF();
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

    /*
        POST data as chunked streaming mode without length header
        Uses sleep to reduce cpu usage (from 25% at streaming idle to < 3%)
        Thread is largely passive except for setting up, stopping, reading
        and finally closing the connection.
    */
    private void streamToServer() {
        HttpURLConnection conn = null;
        BufferedOutputStream out = null;
        Log.d("[comm]", "Starting communication thread.");

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);

            out = new BufferedOutputStream(conn.getOutputStream());
            rec.startRecording(context, out); // <--- need to pass this in
            while (isStreaming) {
                try {
                    Thread.sleep(10000); //sleep so jvm can restore state after suspend
                } catch (InterruptedException e) {
                    continue;
                }
            }
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            byte b[] = new byte[in.available()];
            in.read(b, 0, b.length);
            String resp = new String(b);
            Log.d("[STREAM]", resp);
        }

        // return error to user about unable to connect?
        catch (IOException e) {
            e.printStackTrace();
            rec.startRecording(context, null); //continue recording without stream
            isStreaming = false;
            return;
        }
        finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            conn.disconnect();
            return;
        }
    }
}

