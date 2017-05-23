package com.cmps115.public_defender;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ErrorDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;



public class StreamAudio extends Service {
    /** interface for clients that bind */
    private final IBinder mBinder = new StreamBinder();

    Thread streamToServThread = null;
    Thread initStreamThread = null;
    PDAudioRecordingManager rec = null;
    boolean isStreaming = false;
    URL url = null;
    String recording_out;

    JSONObject jsonResponse = null;
    JSONObject jsonRequest = null;

    GoogleSignInAccount acct;
    String idToken = null;


    public class StreamBinder extends Binder {
         StreamAudio getService() {
            return StreamAudio.this;
        }
    }

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
        init_threads();
    }
    private void init_threads(){
        // Threads
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
    }
    public void init_stream(Intent intent)
            throws JSONException, MalformedURLException,
                    InterruptedException, StreamException {
        acct = (GoogleSignInAccount) SharedData.getKey("google_acct");
        idToken = acct.getIdToken();
        String userId = acct.getId();
        String url_data = intent.getStringExtra("host_string");
        recording_out = intent.getStringExtra("output_dir");
        String geo_data = intent.getStringExtra("geo");
        rec = new PDAudioRecordingManager(); // exception from here?
        jsonRequest = new JSONObject();
        jsonRequest.put("current_location", geo_data);
        jsonRequest.put("user", userId);
        url = new URL(url_data);
        initStreamThread.start();
        initStreamThread.join();
        if (jsonResponse.get("status").equals("error")) {
            throw new StreamException(jsonResponse.getString("msg"));
        }
        Log.d("[GETURL**]", jsonResponse.toString());
        url = new URL(url.toString() + jsonResponse.get("url").toString()); //source of errors
    }

    public void stream_recording(){
        isStreaming = true;
        streamToServThread.start();
    }
    public void stopStream(){
        if (rec != null) {
            rec.stopRecording();
            isStreaming = false;
            streamToServThread.interrupt();
        }
        jsonResponse = null;
        init_threads();
        url = null;
        //this.stopSelf();
    }

    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        if (rec != null) {
            rec.stopRecording();
            isStreaming = false;
            streamToServThread.interrupt();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /*******************************
     *  Setup Stream/Event:
     *  Establish an event, verify credentials (unfinished), and
     *  populates the POST url for the streaming portion.
     */
    private void initStream() throws java.net.SocketTimeoutException {
        HttpURLConnection conn = null;
        DataOutputStream out = null;
        StringBuffer response = new StringBuffer();
        BufferedReader in = null;
        int resp_code;
        Log.d("[initStream]", "initStream: Starting connect...");
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Auth-Key", idToken);
            conn.setFixedLengthStreamingMode(jsonRequest.toString().getBytes().length);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000); //set timeout to 5 seconds

            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(jsonRequest.toString());
            out.flush();
            out.close();
            if ((resp_code = conn.getResponseCode()) != 200) {
                Log.d("[INITSTREAM]", "THERE WAS AN ERROR!");
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            jsonResponse = new JSONObject(response.toString());
        }
        finally {
            Log.d("[initStream]", "initStream: ending connect...");
            Log.d("ENDSTREAM", response.toString());
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
            conn.setRequestProperty("Auth-Key", idToken);
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);

            out = new BufferedOutputStream(conn.getOutputStream());
            rec.startRecording(recording_out, out); // <--- need to pass this in
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
            rec.startRecording(recording_out, null); //continue recording without stream
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
