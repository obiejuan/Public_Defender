package com.cmps115.public_defender;

import android.Manifest;
import android.app.ProgressDialog;
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
 */

public class GeoHandler {
    // Geolocation components
    private LocationManager geoManager;
    private AppCompatActivity activity;
    private ProgressDialog progress;

    private double[] geoPosition = new double[2];

    public GeoHandler(AppCompatActivity act) {
        activity = act;
        geoManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        // Try to get location as early as possible
        findLocation();
    }

    public double[] getGeolocation() {
        // Setup our geo system

        updateGeolocation();
        return geoPosition;
    }

    public boolean hasGeolocation()
    {
        return !(geoPosition[0] == 0.0 && geoPosition[1] == 0.0) && checkForLocationEnabled();
    }

    private void findLocation()
    {
        ReliableGeolocationProvider.LocationResult locationResult = new ReliableGeolocationProvider.LocationResult(){
            @Override
            public void gotLocation(Location location){
                if(location == null)
                {
                    geoPosition[0] = 0.0;
                    geoPosition[1] = 0.0;
                    return;
                }

                geoPosition[0] = location.getLatitude();
                geoPosition[1] = location.getLongitude();
            }
        };
        ReliableGeolocationProvider reliableGeolocation = new ReliableGeolocationProvider();
        reliableGeolocation.getLocation(activity, locationResult);
    }

    private boolean hasPermissions()
    {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "You didn't enable location permissions!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkForLocationEnabled()
    {
        if(!geoManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Provider not enabled, prompt user to enable it
            Toast.makeText(activity, "Your geolocation wasn't turn on.. turn it on and hit back.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
            return false;
        }
        return true;
    }

    // Setup the geolocation
    private void updateGeolocation() {
        if(hasPermissions()) {
            if(checkForLocationEnabled()) {
                findLocation();
            }
        }
    }
}
