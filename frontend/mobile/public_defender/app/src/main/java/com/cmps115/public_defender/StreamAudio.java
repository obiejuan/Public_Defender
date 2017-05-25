package com.cmps115.public_defender;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.lang.Thread.State;

/*****************************************************************************
 * Streaming/Recording/Streaming service. Allows for the main thread to lose
 * focus and still run. There is some variability in that the android system
 * can still decide to stop services in a low memory situation. This hasn't
 * happened in any of the tests so far.
 *
 * Usage:
 *
 * 1.) Bind to your interface thread (MainActivity.java). This call is
 *      asynchronous so it will call 'onServiceConnected' when it is finished
 *      connecting to the service. Ex.:
 *
 *  Intent streamIntent = new Intent(this, StreamAudio.class);
 *  bindService(streamIntent, mConnection, Context.BIND_AUTO_CREATE);
 *
 * 2.) Override methods inside of ServiceConnection to handle callbacks (from above
 *      and get a Service instance to use for communication.
 *
 * 3.) Use the StreamAudio Service functions to setup and stream the audio.
 *
 ****************************************************************************/

public class StreamAudio extends Service {

    // class level variables:
    private final IBinder mBinder = new StreamBinder();
    private Thread streamToServThread = null;
    private Thread initStreamThread = null;
    private PDAudioRecordingManager rec = null;
    private boolean isStreaming = false;
    private URL url = null;
    private String recording_out;
    private JSONObject jsonResponse = null;
    private JSONObject jsonRequest = null;
    private GoogleSignInAccount acct;
    private String idToken = null;
    private static int CONNECTION_TIMEOUT = 2000;


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
         /********
         * Just in case there are uncaught exceptions in the streaming thread
         */
        streamToServThread = new Thread(new Runnable() {
            public void run() {
                streamToServer();
            }
        });
        streamToServThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Log.d("streamToServThread", "uncaughtException: " + e.getMessage());
            }
        });

        /*********
         * Just in case there are uncaught exceptions in the stream initialization thread
         */
        initStreamThread = new Thread(new Runnable() {
            public void run() {
                initStream();
            }
        });
        streamToServThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Log.d("initStreamThread", "uncaughtException: " + e.getMessage());
            }
        });
    }
    public void init_stream(Intent intent)
            throws JSONException, MalformedURLException,
                    InterruptedException, StreamException {

        /****
         *  Re-initialize threads. Shouldn't be necessary
         *  but it doesn't take too long and can ensure other
         *  errors don't happen further down the function.
         */
        init_threads();

        // setUncaughtExceptionHandler
        acct = (GoogleSignInAccount) SharedData.getKey("google_acct");
        idToken = acct.getIdToken();
        String userId = acct.getId();

        // get data from intent
        String url_data = intent.getStringExtra("host_string");
        recording_out = intent.getStringExtra("output_dir");
        String geo_data = intent.getStringExtra("geo");

        // initialize audio recorder
        rec = new PDAudioRecordingManager(); // exception from here?

        // prepare JSON request
        jsonRequest = new JSONObject();
        jsonRequest.put("current_location", geo_data);
        jsonRequest.put("user", userId);

        // create URL
        url = new URL(url_data);

        /******
        *  Make sure the thread is in the correct state
        *  Shouldn't ever get to this point but put in just in case
        *  (safety first ;)
        */
        if (initStreamThread.getState() == State.NEW) {
            initStreamThread.start();
            initStreamThread.join(); // waits on threads to finish what they are doing
        }
        else { // thread must be in inoperable state
            throw new StreamException("Streaming thread in incorrect state, please restart the app and try again.");
        }

        if (jsonResponse != null) {
            if (jsonResponse.get("status").equals("error")) {
                throw new StreamException(jsonResponse.getString("msg"));
            }
            else if (jsonResponse.has("timeOut")) {
                throw new StreamException("Connection timed out. ");
            }
            else { //no error (best case)
                url = new URL(url.toString() + jsonResponse.get("url").toString()); //source of errors
            }
        }
        else {
            throw new StreamException("Connection error. Are you connected to the internet?");
        }
    }

    /**********
     * start the recording/streaming thread.
     */
    public void stream_recording(){
        isStreaming = true;
        streamToServThread.start();
    }

    /*********
     * Stop the stream thread.
     */
    public void stopStream(){
        if (rec != null) {
            rec.stopRecording();
            isStreaming = false;
            streamToServThread.interrupt();
        }
        jsonResponse = null;
        init_threads();
        url = null;
    }

    /**
     * The service is starting, This isn't used directly. Instead
     * we bind to the main thread to have more full access to different
     * methods and getting errors to the UI thread easily.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    /**
     *  Called when The service is being destroyed
     */
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
    private void initStream() {
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
            conn.setConnectTimeout(CONNECTION_TIMEOUT); //set timeout to 2 seconds

            out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(jsonRequest.toString());
            out.flush();
            out.close();

            // If request is not successful
            if ((resp_code = conn.getResponseCode()) != 200) {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            // Successful request
            else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }

            // Parse result into JSON
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            jsonResponse = new JSONObject(response.toString());
        }

        // Catch error if there is a time-out while making the request
        catch (SocketTimeoutException e) {
            jsonResponse = new JSONObject();
            jsonResponse.put("timeOut", true);
            throw e;
        }
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
            conn.setRequestProperty("Auth-Key", idToken);
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);
            conn.setConnectTimeout(CONNECTION_TIMEOUT); // time out 5 sec

            out = new BufferedOutputStream(conn.getOutputStream());
            rec.startRecording(recording_out, out); // <--- need to pass this in
            /************************************
             * This loop actually just sleeps until interrupted
             * Alternatives wouldn't restore from suspended state
             * properly.
             */
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
        }
        // return error to user about unable to connect?
        catch (java.net.SocketTimeoutException e) {
            jsonResponse = new JSONObject();
            jsonResponse.put("timeOut", true);
        }
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
