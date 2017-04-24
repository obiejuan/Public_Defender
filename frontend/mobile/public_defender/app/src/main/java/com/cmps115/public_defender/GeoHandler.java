package com.cmps115.public_defender;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * Created by Oliver Davies on 4/23/2017.
 */

public class GeoHandler implements LocationListener
{
    private double[] geoPosition = new double[2];
    private boolean isProviderDefined = false;

    public boolean hasGeolocation()
    {
        return isProviderDefined && ((geoPosition == null));
    }

    public double[] getGeolocation()
    {
        return geoPosition;
    }

    @Override
    public void onLocationChanged(Location loc)
    {
        geoPosition[0] = loc.getLatitude();
        geoPosition[1] = loc.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider)
    {
        isProviderDefined = true;
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        isProviderDefined = false;
    }
}
