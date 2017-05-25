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
 *
 *  DEPRECATED -- USE STREAMAUDIO SERVICE
 *
 * Created by bryan on 4/30/17.
 * Streams data to server.
 *
 * Using the emulator, your computers IP will actually be
 *  = 10.0.2.2
 *
 * Instead of the usual localhost.
 *  = 127.0.0.1
 *
 *  Which on the emulator, is your phone's loopback interface.
 */

public class StreamToServer {
    Thread streamToServThread = null;
    Thread initStreamThread = null;
    PDAudioRecordingManager rec = null;
    boolean isStreaming = false;
    URL url = null;
    String output_dir;
    JSONObject jsonResponse = null;
    JSONObject jsonRequest = null;

    /***************************
     *  Pass in a recorder object, the urlstring and the current context
     */
    StreamToServer(PDAudioRecordingManager recorder, String urlString, String o_dir, JSONObject json){
        streamToServThread = new Thread(new Runnable() {
            public void run() {
                streamToServer();
            }
        });
        initStreamThread = new Thread(new Runnable() {
            public void run() {
                try {
                    initStream();
                }
                catch (java.net.SocketTimeoutException timeOutErr) {
                    timeOutErr.printStackTrace();
                }

            }
        });
        rec = recorder;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
         output_dir = o_dir;
        jsonRequest = json;
    }

    /*******************************
     *  stop streaming/recording audio
     */
    public void stopStreamAudio() {
        if (rec != null) {
            rec.stopRecording();
            isStreaming = false;
            streamToServThread.interrupt();
        }
    }

    /*******************************
     *  Start stream method:
     *  Starts the chain of events to create an event
     *  and stream the audio to the server.
     */
    public void startStreamAudio() {
        initStreamThread.start();
        try {
            initStreamThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            url = new URL(url.toString() + jsonResponse.get("url").toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        isStreaming = true;
        streamToServThread.start();

    }

    /*******************************
     *  Setup Stream/Event:
     *  Establish an event, verify credentials (unfinished), and
     *  populates the POST url for the streaming portion.
     */
    private void initStream() throws java.net.SocketTimeoutException {
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
        }
        // return error to user about unable to connect?
        finally {
            conn.disconnect();
            return;
        }

    }

    /*******************************
     *  Streaming Function:
     *  POST data as 'chunked' streaming mode without length header
     *  Uses sleep to reduce cpu usage (from 25% at streaming idle to < 3%)
     */
    private void streamToServer() {
        HttpURLConnection conn = null;
        BufferedOutputStream out = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);

            out = new BufferedOutputStream(conn.getOutputStream());
            rec.startRecording(output_dir, out); // <--- need to pass this in
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
        }
        // return error to user about unable to connect?
        catch (IOException e) {
            e.printStackTrace();
            rec.startRecording(output_dir, null); //continue recording without stream
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

