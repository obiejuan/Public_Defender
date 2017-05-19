package com.cmps115.public_defender;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
    /** indicates how to behave if the service is killed */
    int mStartMode;
    /** interface for clients that bind */
    private final IBinder mBinder = new StreamBinder();
    /** indicates whether onRebind should be used */
    boolean mAllowRebind;

    Thread streamToServThread = null;
    Thread initStreamThread = null;
    PDAudioRecordingManager rec = null;
    boolean isStreaming = false;
    URL url = null;
    String recording_out;
    Context context = null;
    JSONObject jsonResponse = null;
    JSONObject jsonRequest = null;

    /*public StreamAudio(PDAudioRecordingManager recorder, String urlString, Context c, JSONObject json) {
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
        context = c;
        jsonRequest = json;
    }*/

    public class StreamBinder extends Binder {
         StreamAudio getService() {
            return StreamAudio.this;
        }
    }

    /** Called when the service is being created. */
    @Override
    public void onCreate() {
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


    /** The service is starting, due to a call to startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url_data = intent.getStringExtra("host_string");
        recording_out = intent.getStringExtra("output_dir");
        String geo_data = intent.getStringExtra("geo");
        rec = new PDAudioRecordingManager();
        jsonRequest = new JSONObject();
        try {
            jsonRequest.put("location", geo_data);
            jsonRequest.put("user", 1); //TODO hardcoded user, will actually be unique user id
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            url = new URL(url_data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

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
        return mStartMode;
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
        Log.d("[initStream]", "initStream: Starting connect...");
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
                Log.d("[initStream]", inputLine);
            }
            in.close();
            jsonResponse = new JSONObject(response.toString());
            Log.d("[initStream]", jsonResponse.toString());
        }
        // return error to user about unable to connect?
        finally {
            Log.d("[initStream]", "initStream: ending connect...");
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
            rec.startRecording(recording_out, out); // <--- need to pass this in
            while (isStreaming) {
                try {
                    Log.d("STREAM", "streamToServer....");
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
