package com.cmps115.public_defender;

import android.content.Context;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    // Geolocation components
    LocationManager geoManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    GeoHandler geoHandler = new GeoHandler();

    // To show Geo
    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        text = (TextView) findViewById(R.id.geotext);
        //setupGeolocation();
        setContentView(R.layout.activity_main);
    }

    private void setupGeolocation()
    {
        // Setup Geolocation
        try {
            geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, geoHandler);
        } catch(SecurityException se) {/* We don't have premission */}
    }

    public void geoButtonPressed(View v)
    {
        if(geoHandler.hasGeolocation())
        {
            double[] geo = geoHandler.getGeolocation();
            text.setText(geo[0] + ", " + geo[1]);
        }
    }
}
