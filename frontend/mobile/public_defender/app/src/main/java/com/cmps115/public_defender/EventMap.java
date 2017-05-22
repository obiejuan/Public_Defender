package com.cmps115.public_defender;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;


public class EventMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        JSONArray events = (JSONArray) SharedData.getKey("event_list");
        //events.length();
        // Add our current location, with a title, and small 'snippet'. Center Camera on us.
        double[] point_user = (double[]) SharedData.getKey("location");
        LatLng user_mark = new LatLng(point_user[0], point_user[1]);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(user_mark));
        if (events != null) {
            for (int i = 0; i < events.length(); ++i) {
                try {
                    String point = events.getJSONObject(i).getString("location");
                    double[] point_numeric = parse_point(point);
                    LatLng mark = new LatLng(point_numeric[0], point_numeric[1]);
                    builder.include(mark);
                    Log.d("[MapPoint]", point);
                    //String title = events.getJSONObject(i).getString();
                    mMap.addMarker(new MarkerOptions().position(mark));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        Marker you = mMap.addMarker(new MarkerOptions()
                .position(user_mark)
                .title("You")
                .snippet("This is where you are located.")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        builder.include(user_mark);
        LatLngBounds bounds = builder.build();
        CircleOptions a = new CircleOptions();
        a.center(user_mark);
        a.radius(1000*10); //meters/mile constant put here
        mMap.addCircle(a);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));

    }
    public double[] parse_point(String point_string) {
        double[] out = new double[2];
        String pattern = "(-?\\d*\\.\\d*)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(point_string);
        int count = 0;
        while (m.find()) {
            Log.d("[PARSE]", m.group(0));
            out[count] = Double.parseDouble(m.group(0));
            count++;
        }
        return out;
    }
}

