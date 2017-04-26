package com.cmps115.public_defender;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    GeoHandler geoHandler;

    // To show Geo
    TextView view_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        view_text = (TextView) findViewById(R.id.geotext);
        setupPremissions();
        setupGeolocation();
    }

    private void setupPremissions()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void setupGeolocation() {
        geoHandler = new GeoHandler(this, (LocationManager) getSystemService(Context.LOCATION_SERVICE));
    }

    public void geoButtonPressed(View v)
    {
        Log.d("public_defender", "geo button down");
        if(geoHandler.hasGeolocation())
        {
            double[] geo = geoHandler.getGeolocation();
            view_text.setText(geo[0] + ", " + geo[1]);
        }
        else
        {
            view_text.setText("Doesn't look like you have geolocation enabled on your phone.");
        }
    }
}
