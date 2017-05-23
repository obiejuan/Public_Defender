package com.cmps115.public_defender;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CurrentEvents extends AppCompatActivity {
    ProgressDialog progress;
    GoogleApiClient googleApiClient;
    GoogleSignInAccount acct;
    GeoHandler geoHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_events);
        findCurrentEventsOnServer();
        holdOnPopup();
        acct = (GoogleSignInAccount) SharedData.getKey("google_acct");
        googleApiClient = (GoogleApiClient) SharedData.getKey("google_api_client");
        Log.d("google_api_client:", String.valueOf(googleApiClient.isConnected()));
        Log.d("google_acct:", String.valueOf(acct.getIdToken()));
    }

    @Override
    protected void onStart(){
        super.onStart();
       /* if (geoHandler == null) {
            geoHandler = new GeoHandler(this);
        }*/
        geoHandler = new GeoHandler(this);

    }

    private class CustomArrayAdaptor extends ArrayAdapter<String> {

        Context context;
        int viewId;
        List<String> data;

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public CustomArrayAdaptor(Context context, int viewId, List<String> objects) {
            super(context, viewId, objects);
            this.context = context;
            this.viewId = viewId;
            this.data = objects;

            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;
            if(row == null)
            {
                LayoutInflater infl = ((Activity)context).getLayoutInflater();
                row = infl.inflate(viewId, parent, false);
                TextView textView = (TextView)row.findViewById(R.id.eventText);
                textView.setText(data.get(position));
                row.setTag(textView);
            }
            else
            {
                TextView textView = (TextView)row.findViewById(R.id.eventText);
                textView.setText(data.get(position));
            }
            return row;
        }
    }

    public void goHome(View view) {
        finish();
    }

    public void refresh(View view) {
        findCurrentEventsOnServer();
        holdOnPopup();
    }

    public void gotoMap(View view){
        Intent intent = new Intent(this, EventMap.class);
        startActivity(intent);
    }

    private void findCurrentEventsOnServer()
    {
        geoHandler = new GeoHandler(this);
        double[] geo = {0.0, 0.0};
        if(geoHandler.hasLocationOn())
            geo = geoHandler.getGeolocation();
        SharedData.setKey("location", geo);
        String geo_data = String.format("(%f, %f)", geo[1], geo[0]);
        Log.d("[GEO_DATA]", geo_data);


        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("current_location", geo_data);
            jsonRequest.put("distance", 10); //TODO hardcoded distance
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new getNearbyEvents().execute(jsonRequest);
    }

    private void populateListViewWithCurrentEvents(JSONObject events) throws JSONException {
        ListView listview = (ListView) findViewById(R.id.current_events);
        //String[] values = {"Test1", "Test2"};
        JSONArray event_list = events.getJSONArray("data");
        event_list.length();
        SharedData.setKey("event_list", event_list);
        final ArrayList<String> list = new ArrayList<String>();
        //event_list.getJSONObject();
        for (int i = 0; i < event_list.length(); ++i) {
            JSONObject this_obj = event_list.getJSONObject(i);
            String distance = this_obj.getString("event_dist");
            list.add("[" + this_obj.getString("event_id") + "]: " + distance.substring(0,  Math.max(0, Math.min(4, distance.length()))) + " miles away.");
        }

        final CustomArrayAdaptor adapter = new CustomArrayAdaptor(this, R.layout.current_events_list_item, list);
        listview.setAdapter(adapter);
    }

    private void holdOnPopup()
    {
        progress = new ProgressDialog(this);
        progress.setTitle("Finding nearby events..");
        progress.setMessage("Hold on while we search..");
        progress.setCancelable(false);
        progress.show();
    }

    //unfinished
    private class getNearbyEvents extends AsyncTask<JSONObject, Integer, JSONObject> {
        protected JSONObject doInBackground(JSONObject...input_json) {

            int number_req = input_json.length;
            JSONObject input = input_json[number_req-1]; //only process the last (most recent) request
            URL url = null;
            try {
                url = new URL("http://138.68.200.193:3000/nearby/");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            GetNearby req = new GetNearby();
            JSONObject output = req.makeRequest(input, url);
            Log.d("[getNearbyEvents]", output.toString());
            return output;
        }

        protected void onProgressUpdate(Integer... progress) {
            // not sure there's gonna be a use for this...
        }

        protected void onPostExecute(JSONObject result) {
            if(progress != null) progress.dismiss();
            try {
                populateListViewWithCurrentEvents(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // what to do after it's done. Maybe update the UI thread?
            Log.d("[onPostExecute]", result.toString());
        }
    }
}
