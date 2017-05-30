package com.cmps115.public_defender;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Oliver Davies on 5/29/2017.
 * Based on suggestions from Fedor on stackoverflow
 */

public class ReliableGeolocationProvider {
    Timer timeoutTimer;
    LocationManager locationManager;
    LocationResult locationResult;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;

    public static abstract class LocationResult {
        public abstract void gotLocation(Location location);
    }

    public boolean getLocation(Context context, LocationResult result) {
        locationResult = result;
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        catch (Exception ex) {
            Log.d("Reliable Failed: ", "Failed on isGPSEnabled");
        }
        try {
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        catch (Exception ex) {
            Log.d("Reliable Failed: ", "Failed on isNeworkEnabled");
        }

        if (!isGPSEnabled && !isNetworkEnabled) {
            return false;
        }

        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        }
        if(isNetworkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        }

        // If we don't have location by the end of 10 seconds.. give us the last known location.
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new GetLastLocation(), 1000);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            timeoutTimer.cancel();
            locationResult.gotLocation(location);
            locationManager.removeUpdates(this);
            locationManager.removeUpdates(locationListenerNetwork);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            timeoutTimer.cancel();
            locationResult.gotLocation(location);
            locationManager.removeUpdates(this);
            locationManager.removeUpdates(locationListenerGps);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    class GetLastLocation extends TimerTask {
        @Override
        public void run() {
            locationManager.removeUpdates(locationListenerGps);
            locationManager.removeUpdates(locationListenerNetwork);

            Location networkLocation = null;
            Location gpsLocation = null;
            if(isGPSEnabled) {
                gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if(isNetworkEnabled) {
                networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if(gpsLocation!=null && networkLocation!=null){
                if(gpsLocation.getTime()>networkLocation.getTime()) {
                    locationResult.gotLocation(gpsLocation);
                }
                else {
                    locationResult.gotLocation(networkLocation);
                }
                return;
            }

            if(gpsLocation != null) {
                locationResult.gotLocation(gpsLocation);
                return;
            }
            if(networkLocation != null) {
                locationResult.gotLocation(networkLocation);
                return;
            }
            locationResult.gotLocation(null);
        }
    }
}

