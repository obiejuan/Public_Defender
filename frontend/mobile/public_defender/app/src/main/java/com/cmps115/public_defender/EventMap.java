package com.cmps115.public_defender;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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


    @Override
    protected void onStart(){
        super.onStart();
    }
    /**
     * Map stuff
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        JSONArray events = (JSONArray) SharedData.getKey("event_list");
        double[] point_user = (double[]) SharedData.getKey("user_location");
        Log.d("GEO", "LOCATION: " + String.valueOf(point_user));
        LatLng user_mark = new LatLng(point_user[0], point_user[1]);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(user_mark));
        if (events != null) {
            for (int i = 0; i < events.length(); ++i) {
                try {
                    String point = events.getJSONObject(i).getString("location");
                    Log.d("GEO", "onMapReady: " + point);
                    double lat = events.getJSONObject(i).getJSONObject("location").getDouble("y");
                    double lon = events.getJSONObject(i).getJSONObject("location").getDouble("x");
                    Log.d("GEO", "onMapReady: " + String.valueOf(lat) + ", " + String.valueOf(lon));
                    LatLng mark = new LatLng(lat, lon);
                    builder.include(mark);
                    //String title = events.getJSONObject(i).getString("location");
                    String distance = events.getJSONObject(i).getString("event_dist");
                    int id = events.getJSONObject(i).getInt("event_id");
                    mMap.addMarker(new MarkerOptions().position(mark)
                            .title("Event: " + String.valueOf(id))
                            .snippet(String.format("%.2f miles away.",
                                                    Double.parseDouble(distance))));

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

