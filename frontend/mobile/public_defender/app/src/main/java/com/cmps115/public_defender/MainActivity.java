package com.cmps115.public_defender;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;


/*
Please note that if you want to change the draw/drop theme for this activity you need to ensure you're setting the contraints.
This means that you will need to hit the little golden stars after you place an element. It is the bar above the drag/drop editor.
-Oliver
 */


public class MainActivity extends AppCompatActivity {

    GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    PDAudioRecordingManager pdarm;
    private boolean isRecording = false;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                //.requestIdToken("232250430081-nrqnos5eqst3rn7o2r9c8jas6m42i6p5.apps.googleusercontent.com")
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signIn();
        mGoogleApiClient.connect();

        String res = String.valueOf(mGoogleApiClient.  isConnected());

        Log.d("isconnected: ", res);


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
        // Do something in response to Record button
        Context context = getApplicationContext();
        CharSequence text = "Hit Record";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();


        PDAudioRecordingManager pdarm = new PDAudioRecordingManager();
        if (!isRecording) {
            pdarm.startRecording(context);
            r_button.setText("Stop Recording.");
        }
        if (isRecording) {
            pdarm.stopRecording();
            r_button.setText("Record");
            pdarm = null;
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
