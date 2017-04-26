package com.cmps115.public_defender;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by Oliver Davies on 4/23/2017.
 */

public class GeoHandler {

    // Geolocation components
    LocationManager geoManager;
    AppCompatActivity activity;

    private double[] geoPosition = new double[2];

    public GeoHandler(AppCompatActivity act, LocationManager lm) {
        activity = act;
        geoManager = lm;
    }

    public boolean hasGeolocation() {
        return true;
    }

    public double[] getGeolocation() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return geoPosition;
        }

        Location l = geoManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        Toast.makeText(activity, "Tried to find Geolocation", Toast.LENGTH_SHORT).show();
        try {
            geoPosition[0] = l.getLatitude();
            geoPosition[1] = l.getLongitude();
        } catch (NullPointerException e){}
        return geoPosition;
    }
}
