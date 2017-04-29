package com.cmps115.public_defender;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
        geoHandler = new GeoHandler(this);
    }

    public void geoButtonPressed(View v)
    {
        double[] geo = geoHandler.get_geolocation();
        if(!(geo[0] == 0.0 && geo[1] == 0.0))
            view_text.setText(geo[0] + ", " + geo[1]);
    }
}
