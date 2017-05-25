package com.cmps115.public_defender;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */


    // UI references.

    GoogleApiClient mGoogleApiClient;
//    GoogleApiClient mGoogleSignInButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("In function", "onCreate");

        super.onCreate(savedInstanceState);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d("is connected", "is connected");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        } else {
            setContentView(R.layout.activity_login);
            // Set up the login form
            Log.d("not connected", "not connected");
            SignInButton mGoogleSignInButton = (SignInButton) findViewById(R.id.google_sign_in_button);
            mGoogleSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    signInWithGoogle();
                }
            });

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("In function", "onActivityResult");


        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if(result.isSuccess()) {
                final GoogleApiClient client = mGoogleApiClient;
                String success = result.getSignInAccount().toString();
                Log.d("success", success);
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                //handleSignInResult(...)
            } else {

                //handleSignInResult(...);
            }
        } else {
            // Handle other values for requestCode
        }
    }

    private static final int RC_SIGN_IN = 9001;


    private void signInWithGoogle() {
        Log.d("In function", "signInWithGoogle");
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

}

