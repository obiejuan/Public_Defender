package com.cmps115.public_defender;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;


/*
Please note that if you want to change the draw/drop theme for this activity you need to ensure you're setting the contraints.
This means that you will need to hit the little golden stars after you place an element. It is the bar above the drag/drop editor.
-Oliver
 */


public class MainActivity extends AppCompatActivity {

    GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    GeoHandler geoHandler = null;
    PDAudioRecordingManager pdarm;
    StreamToServer serv;

    private boolean isRecording = false;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /*GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                //.requestIdToken("232250430081-nrqnos5eqst3rn7o2r9c8jas6m42i6p5.apps.googleusercontent.com")
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signIn();
        mGoogleApiClient.connect();

        String res = String.valueOf(mGoogleApiClient.  isConnected());

        Log.d("isconnected: ", res);*/
        if (geoHandler == null) {
            geoHandler = new GeoHandler(this);
        }




    }

    private static final int RC_SIGN_IN = 9001;

    private void signIn() {
        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if(result.isSuccess()) {
                GoogleSignInAccount gsa = result.getSignInAccount();
                String success = gsa.getId();
                Log.d("success id token", success);
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
                //handleSignInResult(...)
            } else {

                Log.d("not successful", result.toString());
                //handleSignInResult(...);
            }
        } else {
            // Handle other values for requestCode
        }
    }

    public void checkAndSignOut(View view){
        if(mGoogleApiClient.isConnected()){
            signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    public void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
//                        updateUI(false);
                        Log.d("Signout", "sigout");
                        // [END_EXCLUDE]
                    }
                });
    }

    public void broadCast(View view) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        Button r_button = (Button)findViewById(R.id.record_button);
        Context context = getApplicationContext();
        CharSequence text = "Hit Record";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        //Handler h = new Handler(){};
        double[] geo = geoHandler.get_geolocation();
        if(!(geo[0] == 0.0 && geo[1] == 0.0))
            Log.d("[GEO]", (geo[0] + ", " + geo[1]));
        // test data
        JSONObject json_test = new JSONObject();
        try {
            json_test.put("location", String.format("(%f, %f)", geo[1], geo[0]) ); //have to reverse them
            json_test.put("user", 2);
        } catch (JSONException e) {
            e.printStackTrace();
        }  // end test data

        if (!isRecording) {
            // USAGE EXAMPLE:
            pdarm = new PDAudioRecordingManager();
            serv = new StreamToServer(pdarm, "http://192.168.1.118:3000/upload/", context, json_test);
            serv.startStreamAudio();
            r_button.setText("Stop Recording.");
        }
        if (isRecording) {
            // USAGE EXAMPLE:
            serv.stopStreamAudio();
            r_button.setText("Record");
        }
        isRecording = !isRecording;
    }

    public void gotoMenu(View view){
        Intent intent = new Intent(this, Menu.class);
        Log.d("Menu", "clicked menu");
        startActivity(intent);
    }

    public void gotoLogin(View view){
        Intent intent = new Intent(this, LoginActivity.class);
        Log.d("Menu", "clicked menu");
        startActivity(intent);
    }

    public void gotoCurrentEvents(View view){
        Intent intent = new Intent(this, CurrentEvents.class);
        startActivity(intent);
        // get request, yay
        URL u = null;
        try {
            u = new URL("http://169.233.224.14:3000/nearby/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        double[] geo = geoHandler.get_geolocation();
        NearbyFromServer n = new NearbyFromServer(geo, 10, u);
        JSONObject output = n.getEvents();

    }

    // Prompt user for permission to record audio
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

}
