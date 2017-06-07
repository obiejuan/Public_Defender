package com.cmps115.public_defender;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/*
Please note that if you want to change the draw/drop theme for this activity you need to ensure you're setting the contraints.
This means that you will need to hit the little golden stars after you place an element. It is the bar above the drag/drop editor.
-Oliver
 */


public class MainActivity extends AppCompatActivityWithPDMenu implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_LOCATION_FINE_PERMISSION = 420;
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

    private final String externalServerIP = PRODUCTION_SERVER;
    private final String externalServerPort = "3000";

    boolean mBound = false;
    StreamAudio mService = null;

    Location mLastLocation;

    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            streamIntent = savedInstanceState.getParcelable("stream_intent");
            isRecording = savedInstanceState.getBoolean("is_recording");
            Log.d(TAG, "onCreate: INSTANCE STATE!!!!!");
        }
        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.status);

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        if(!requestLocation()) {
            showLocationSettingsAlert();
            finish();
        }

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

    }


    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void changeButtonState(boolean b) {
        Button rec = (Button) findViewById(R.id.record_button);
        rec.setEnabled(b);
        //Button curr_events = (Button) findViewById(R.id.current_events_button);
        //curr_events.setEnabled(b);
        //Button menu_btn = (Button) findViewById(R.id.my_recording_button);
        //menu_btn.setEnabled(b);
    }

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

    public void showLocationSettingsAlert() {
        Toast toast = Toast.makeText(getApplicationContext(), "You don't have location enabled! Enable it and restart Public Defender.", Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onStart() {
        super.onStart();

        //bind service
        Intent streamIntent = new Intent(this, StreamAudio.class);
        ComponentName serv = startService(streamIntent);
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
        if (isRecording) {
            final Button r_button = (Button) findViewById(R.id.record_button);
            r_button.setText("Stop");
        }
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }


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

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        Log.d(TAG, "handleSignInResult: " + result.getStatus().toString());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getEmail()));
            Log.d(TAG, "handleSignInResult: " + acct.getEmail());
            Log.d(TAG, "handleSignInResult: " + acct.getIdToken());
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

    public void gotoMyRecordings(View view) {
        Intent intent = new Intent(this, FileBrowser.class);
        startActivity(intent);
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
            getSupportActionBar().show();
        } else {
            mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
            getSupportActionBar().hide();
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
        Log.d(TAG, "onSaveInstanceState: *************");
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState: **********");
        streamIntent = savedInstanceState.getParcelable("stream_intent");
        isRecording = savedInstanceState.getBoolean("is_recording");

    }

    /******
     * Called immediately after the service is built and connects to the main
     * thread. So far just used to initialize mService, mBound variables.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
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

    private boolean hasPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_FINE_PERMISSION);
            Toast.makeText(this, "You didn't enable location permissions!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void toastErrorMessage(Exception e) {
        Context context = getApplicationContext();
        CharSequence error_txt = e.getMessage();
        int duration_error = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, error_txt, duration_error);
        toast.show();
    }
    public void toastMessage(String msg) {
        Context context = getApplicationContext();
        CharSequence msg_txt = msg;
        int duration_error = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, msg_txt, duration_error);
        toast.show();
    }

    public boolean safeGeo(){
        if (mLastLocation == null) {
            boolean canRequest = ((LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!canRequest) {
                toastMessage("Location not enabled! Enable it and try again.");
                return false;
            }
            progress = new ProgressDialog(this);
            progress.setTitle("Location");
            progress.setMessage("Finding location....");
            progress.setCancelable(false);
            progress.show();
            PendingResult result = LocationServices.FusedLocationApi.flushLocations(mGoogleApiClient);
            ResultCallback callback = new ResultCallback() {
                @Override
                public void onResult(@NonNull Result result) {
                    Log.d(TAG, "[result]" + result.getStatus().toString());
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    LocationRequest req = new LocationRequest();
                    LocationListener listen = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            progress.dismiss();
                            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        }
                    };
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, listen);
                }
            };
            result.setResultCallback(callback);

            return false;
        }
        return true;
    }

    public void broadCast(View view) {
        //if(!permissionToRecordAccepted || !permissionForLocationAccepted) return;

        if (isSignedIn) {
            final Button r_button = (Button) findViewById(R.id.record_button);
            Context context = getApplicationContext();

            double[] geo = {0.0, 0.0};

            if (!safeGeo()) {
                return;
            }
            geo[0] = mLastLocation.getLatitude();
            geo[1] = mLastLocation.getLongitude();
            String geo_data = String.format("(%f, %f)", geo[1], geo[0]);
            Log.d("[GEO]", (geo[1] + ", " + geo[0]));

            if (!isRecording) {
                progress = new ProgressDialog(this);
                progress.setTitle("Starting...");
                progress.setMessage("Please wait.");
                progress.setCancelable(false);
                progress.show();
                Log.d(TAG, "broadCast: It should start...");
                Intent streamIntent = new Intent(this, StreamAudio.class);
                String full_url = "http://" + externalServerIP + ":" + externalServerPort + "/upload/";
                streamIntent.putExtra("host_string", full_url);
                streamIntent.putExtra("output_dir", context.getExternalCacheDir().getAbsolutePath());
                streamIntent.putExtra("geo", geo_data);
                try {
                    mService.init_stream(streamIntent);
                    Log.d(TAG, "INIT STREAM STARTED!!");
                } catch (StreamException | MalformedURLException | InterruptedException | JSONException e) { // error response
                    isRecording = false;
                    toastErrorMessage(e);
                    r_button.setText("Record");
                    progress.hide();
                    return;
                }
                mService.stream_recording();
                r_button.setText("Stop");
                progress.hide();
                isRecording = !isRecording;
            } else if (isRecording) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure you want to stop broadcasting?");
                builder.setCancelable(true);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mService.stopStream();
                        r_button.setText("Record");
                        isRecording = !isRecording;
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        } else {
            promptSignIn();
        }
    }


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
            if(mLastLocation == null) {
                toastMessage("Location not enabled! If you've just enabled it. Please try again.");
                LocationServices.FusedLocationApi.flushLocations(mGoogleApiClient);
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                return;
            }
            intent.putExtra("mLastLocation", mLastLocation);
            startActivity(intent);
        } else {
            promptSignIn();
        }
    }

    public void promptSignIn() {
        Context context = getApplicationContext();
        CharSequence text = "You must sign in";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

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

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mBound = false;
        unbindService(mConnection);
        if (progress != null) {
            progress.dismiss();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (progress != null) {
            progress.dismiss();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (progress != null) {
            progress.dismiss();
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
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if(!permissionToRecordAccepted){
                    finish();
                }
                // Request the next premission (cascading due to its async nature)
                askForPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_LOCATION_FINE_PERMISSION);
                break;
            case REQUEST_LOCATION_FINE_PERMISSION:
                permissionForLocationAccepted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if(!permissionForLocationAccepted){
                    finish();
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: Location services connected!");
        setupLocation();
    }

    private void setupLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location servers not yet allowed!");
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLastLocation.getLatitude();
            mLastLocation.getLongitude();
        }
    }

    private boolean requestLocation() {
        boolean canRequest = ((LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(canRequest) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(getString(R.string.CLIENT_ID))
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            return true;
        }
        else{
            return false;
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnected: Location services suspended!");
    }
}

