package com.cmps115.public_defender;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;

import java.net.MalformedURLException;

/*
Please note that if you want to change the draw/drop theme for this activity you need to ensure you're setting the contraints.
This means that you will need to hit the little golden stars after you place an element. It is the bar above the drag/drop editor.
-Oliver
 */


public class MainActivity extends AppCompatActivity implements
                                    GoogleApiClient.OnConnectionFailedListener,
                                                        View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_LOCATION_FINE_PERMISSION = 420;
    GeoHandler geoHandler = null;
    private Intent streamIntent = null;
    private boolean isRecording = false;

    // Requesting permissions
    private boolean permissionToRecordAccepted = false;
    private boolean permissionForLocationAccepted = false;

    // merged
    private TextView mStatusTextView;
    private ProgressDialog mProgressDialog;
    private Boolean isSignedIn = false;

    // Set this = your local ip
    private static final String DEV_EMULATOR = "10.0.2.2";
    private static final String DEV_REAL_PHONE = "192.168.1.118"; // your local LAN IP (this is bryan's for example ;)
    private static final String PRODUCTION_SERVER = "138.68.200.193";

    private final String externalServerIP = DEV_REAL_PHONE;
    private final String externalServerPort = "3000";
    boolean mBound = false;
    StreamAudio mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isRecording = savedInstanceState.getBoolean("is_recording");
        }

        setContentView(R.layout.activity_main);
        // Views
        mStatusTextView = (TextView) findViewById(R.id.status);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.CLIENT_ID))
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        // [END build_client]


        // [START customize_button]
        // Set the dimensions of the sign-in button.
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        // [END customize_button]


        //merged
    }


    // [START signIn]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]
    private void changeButtonState(boolean b) {
        Button rec = (Button) findViewById(R.id.record_button);
        rec.setEnabled(b);
        Button curr_events = (Button) findViewById(R.id.button7);
        curr_events.setEnabled(b);
        Button menu_btn = (Button) findViewById(R.id.button5);
        menu_btn.setEnabled(b);
    }

    // [START signOut]
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        changeButtonState(false);
                        isSignedIn = false;
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]


    @Override
    public void onStart() {
        super.onStart();

        //bind service
        Intent streamIntent = new Intent(this, StreamAudio.class);
        bindService(streamIntent, mConnection, Context.BIND_AUTO_CREATE);

        // Ask for the initial permission
        askForPermission(Manifest.permission.RECORD_AUDIO, REQUEST_RECORD_AUDIO_PERMISSION);

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            isSignedIn = true;
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            Log.d(TAG, "onStart: Did not cache sign-in");
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }

        if (geoHandler == null) {
            geoHandler = new GeoHandler(this);
        }
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

  /*  @Override
    protected void onResume() {
        super.onResume();
        hideProgressDialog();
    }*/

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);

        String requestCodeStatus = String.valueOf(requestCode == RC_SIGN_IN);
        if (requestCode == RC_SIGN_IN) {
            Log.d("requestCode ==", requestCodeStatus);
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        Log.d(TAG, "handleSignInResult: " + result.getStatus().toString());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getEmail()));
            Log.d(TAG, "handleSignInResult: "+ acct.getEmail());
            Log.d(TAG, "handleSignInResult: "+ acct.getIdToken());
            /*
                Send token to server?
                Alternatively, just send on every request and call verification on that.
             */
            SharedData.setKey("google_api_client", mGoogleApiClient);
            SharedData.setKey("google_acct", acct);
            Button rec = (Button) findViewById(R.id.record_button);
            changeButtonState(true);
            isSignedIn = true;
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
           changeButtonState(false);
            isSignedIn = false;
            updateUI(false);
        }
    }
    // [END handleSignInResult]

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    /**
     *  private Intent streamIntent = null;
     *  private boolean isRecording = false;
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("stream_intent", streamIntent);
        outState.putBoolean("is_recording", isRecording);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        streamIntent = savedInstanceState.getParcelable("stream_intent");
        isRecording = savedInstanceState.getBoolean("is_recording");

    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            StreamAudio.StreamBinder binder = (StreamAudio.StreamBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService.stopStream();
            mBound = false;
        }
    };

    public void broadCast(View view) {
       //if(!permissionToRecordAccepted || !permissionForLocationAccepted) return;

        if (isSignedIn) {
            Button r_button = (Button)findViewById(R.id.record_button);
            Context context = getApplicationContext();

            // Get the geolocation
            double[] geo = {0.0, 0.0};
            if(geoHandler.hasLocationOn())
                geo = geoHandler.getGeolocation();

            if(!(geo[0] == 0.0 && geo[1] == 0.0))
                Log.d("[GEO]", (geo[0] + ", " + geo[1]));
            // test data

            String geo_data = String.format("(%f, %f)", geo[1], geo[0]);

            if (!isRecording) {
                // USAGE EXAMPLE:
                Log.d(TAG, "broadCast: It should start...");
                Intent streamIntent = new Intent(this, StreamAudio.class);
                String full_url = "http://" + externalServerIP + ":"  + externalServerPort + "/upload/";
                streamIntent.putExtra("host_string", full_url);
                streamIntent.putExtra("output_dir", context.getExternalCacheDir().getAbsolutePath());
                streamIntent.putExtra("geo", geo_data);
                try {
                    mService.init_stream(streamIntent);
                    Log.d(TAG, "INIT STREAM STARTED!!");
                } catch (JSONException e) { // json
                    e.printStackTrace();
                } catch (MalformedURLException e) { //url
                    e.printStackTrace();
                } catch (InterruptedException e) { //join
                    e.printStackTrace();
                } catch (StreamException e) { // error response
                    Log.d(TAG, e.getMessage());
                    // handle error
                    isRecording = false;
                    CharSequence error_txt = "Hit Record";
                    int duration_error = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, error_txt, duration_error);
                    toast.show();
                    r_button.setText("Record");
                    return;
                }
                mService.stream_recording();
                r_button.setText("Stop Recording.");
            }
            if (isRecording) {
                // USAGE EXAMPLE:
                Log.d(TAG, "broadCast: It should STOP recording...");
                mService.stopStream();
                r_button.setText("Record");
            }
            isRecording = !isRecording;
        } else {
            promptSignIn();
        }
    }

//    public void broadCast(View view) {
//        if(!permissionToRecordAccepted || !permissionForLocationAccepted) return;
//
//        final Button recordButton = (Button) view;
//        if(mBroadcasting) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setMessage("Are you sure you want to stop broadcasting?");
//            builder.setCancelable(true);
//            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    recordButton.setBackgroundColor(Color.RED);
//                    recordButton.setText("Record");
//                    mBroadcasting = false;
//                }
//            });
//            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                }
//            });
//            AlertDialog alert = builder.create();
//            alert.show();
//        }
//        else {
//            recordButton.setBackgroundColor(Color.GRAY);
//            recordButton.setText("Broadcasting..");
//            mBroadcasting = true;
//        }
//
//    }

    public void gotoMenu(View view) {
        if (isSignedIn) {
            Intent intent = new Intent(this, Menu.class);
            Log.d("Map", "clicked menu");
            startActivity(intent);
        } else {
           promptSignIn();
        }
    }

    public void gotoCurrentEvents(View view) {
        if (isSignedIn) {
            Intent intent = new Intent(this, CurrentEvents.class);
            startActivity(intent);
        } else {
            promptSignIn();
        }
    }

    public void promptSignIn(){
        Context context = getApplicationContext();
        CharSequence text = "You must sign in";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    // [START revokeAccess]
    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]


    @Override
    protected void onStop() {
        super.onStop();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }

    // Setup the required premissions
    private void askForPermission(String PERMISSION, int CALLBACK_CODE) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION)) {
                ActivityCompat.requestPermissions(this, new String[]{PERMISSION}, CALLBACK_CODE);
            }
        }
    }

    // Prompt user for permission to record audio
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = (grantResults[0] == PackageManager.PERMISSION_GRANTED);

                // Request the next premission (cascading due to its async nature)
                askForPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_LOCATION_FINE_PERMISSION);
                break;
            case REQUEST_LOCATION_FINE_PERMISSION:
                permissionForLocationAccepted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                double[] location = geoHandler.getGeolocation();
                // Debug info
                Log.d("Geolocation permission", "Allowed: " + (grantResults[0] == PackageManager.PERMISSION_GRANTED));
                Log.d("Has location on", "Location on: " + geoHandler.hasLocationOn());
                Log.d("Geolocation", "Lat: " + location[0] + " Long: " + location[1]);
                break;
        }
        if (!permissionToRecordAccepted ) finish();
    }


}

