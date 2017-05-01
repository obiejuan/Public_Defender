package com.cmps115.public_defender;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Created by Oliver Davies on 4/23/2017.
 *
 * Usage:
 *
 * geoHandler = new GeoHandler(this);
 * .....
 *   public void geoButtonPressed(View v)
     {
         double[] geo = geoHandler.get_geolocation();
         if(!(geo[0] == 0.0 && geo[1] == 0.0))
         view_text.setText(geo[0] + ", " + geo[1]);
     }
 *
 *
 */


public class GeoHandler {
    // Geolocation components
    private LocationManager geo_manager;
    private AppCompatActivity activity;

    private double[] geo_pos = new double[2];

    public GeoHandler(AppCompatActivity act) {
        activity = act;
        // Need to do this first
        setup_permissions();
    }

    public double[] get_geolocation() {
        // Setup our geo system
        setup_geolocation();
        return geo_pos;
    }

    // Setup the required premissions
    private void setup_permissions() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);
            }
        }
    }

    // Setup the geolocation
    private void setup_geolocation() {
        geo_manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        Criteria geo_criteria = new Criteria();
        geo_criteria.setAccuracy(Criteria.ACCURACY_FINE);
        geo_criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        geo_criteria.setAltitudeRequired(false);
        geo_criteria.setBearingRequired(false);
        geo_criteria.setCostAllowed(true);

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Failed to setup geo", Toast.LENGTH_SHORT).show();
            return;
        }

        Location geo = geo_manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (geo != null && geo_manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            geo_pos[0] = geo.getLatitude();
            geo_pos[1] = geo.getLongitude();
        } else {
            // Provider not enabled, prompt user to enable it
            Toast.makeText(activity, "Please enable location to use this app", Toast.LENGTH_SHORT).show();
            Intent go_turn_on_location = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(go_turn_on_location);
        }
    }
}
