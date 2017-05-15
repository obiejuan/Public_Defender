package com.cmps115.public_defender;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.*;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by Oliver Davies on 4/23/2017.
 *
 * Usage:
 * CALLED in onStart(..)
 * geoHandler = new GeoHandler(this);
 * .....
 *   public void geoButtonPressed(View v)
     {
         double[] geo = geoHandler.getGeolocation();
         if(!(geo[0] == 0.0 && geo[1] == 0.0))
         view_text.setText(geo[0] + ", " + geo[1]);
     }
 *
 *
 */

public class GeoHandler {
    // Geolocation components
    private LocationManager geoManager;
    private AppCompatActivity activity;

    private double[] geoPosition = new double[2];

    public GeoHandler(AppCompatActivity act) {
        activity = act;
        geoManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    }

    public double[] getGeolocation() {
        // Setup our geo system
        updateGeolocation();
        return geoPosition;
    }

    public boolean hasLocationOn()
    {
        return geoManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
    }

    // Setup the geolocation
    private void updateGeolocation() {
        // Just a required formality
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "You didn't enable location permissions!", Toast.LENGTH_SHORT).show();
            return;
        }

        Location geo = geoManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (geo != null && geoManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            geoPosition[0] = geo.getLatitude();
            geoPosition[1] = geo.getLongitude();
        } else {
            // Provider not enabled, prompt user to enable it
            Toast.makeText(activity, "Your geolocation wasn't turn on.. turn it on and hit back.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
        }
    }
}
