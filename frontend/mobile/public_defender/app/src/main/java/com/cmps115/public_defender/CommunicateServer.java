package com.cmps115.public_defender;


import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bryan on 4/30/17.
 */

public class CommunicateServer {
    Thread communicationThread = null;
    PDAudioRecordingManager rec = null;
    boolean isStreaming = false;
    URL url = null;
    Context context = null;

    /*
        Pass in a recorder object, the urlstring and the current context
    */
    CommunicateServer(PDAudioRecordingManager recorder, String urlString, Context c){
        communicationThread = new Thread(new Runnable() {
            public void run() {
                postData();
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
        rec.stopRecording();
        isStreaming = false;
        communicationThread.interrupt();

    }

    /*
        start streaming audio
    */
    public void startStreamAudio(){
        communicationThread.start();
        isStreaming = true;
    }


    /*
        POST data as chunked streaming mode without length header
    */
    private void postData() {
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
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        while (isStreaming) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.disconnect();
    }


}

