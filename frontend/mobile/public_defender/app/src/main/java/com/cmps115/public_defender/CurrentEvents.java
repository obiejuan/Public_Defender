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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private ProgressDialog progress;
    private GoogleApiClient googleApiClient;
    private GoogleSignInAccount acct;
    private GeoHandler geoHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_events);
        acct = (GoogleSignInAccount) SharedData.getKey("google_acct");
        googleApiClient = (GoogleApiClient) SharedData.getKey("google_api_client");
        Log.d("google_api_client:", String.valueOf(googleApiClient.isConnected()));
        Log.d("google_acct:", String.valueOf(acct.getIdToken()));
    }

    @Override
    protected void onStart(){
        super.onStart();
        geoHandler = new GeoHandler(this);
        findCurrentEventsOnServer();
    }

    private class CustomArrayAdaptor extends ArrayAdapter<String> {

        Context context;
        int viewId;
        List<String> data;
        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
        boolean listEmpty = false;

        public CustomArrayAdaptor(Context context, int viewId, List<String> objects) {
            super(context, viewId, objects);
            this.context = context;
            this.viewId = viewId;
            this.data = objects;
            this.listEmpty = objects.size() < 1;
            ListView listView = (ListView)findViewById(R.id.current_events_list_view);
            listView.setVisibility(this.listEmpty ? View.GONE : View.VISIBLE);
            LinearLayout noNearbyIncidents = (LinearLayout)findViewById(R.id.no_nearby_incidents_layout);
            noNearbyIncidents.setVisibility(this.listEmpty ? View.VISIBLE : View.GONE);

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

                TextView titleText= (TextView)row.findViewById(R.id.eventText);
                titleText.setText(data.get(position));
                row.setTag(titleText);

                TextView addressText = (TextView)row.findViewById(R.id.addressText);
                addressText.setText("Place holder address here");

                TextView timeText = (TextView)row.findViewById(R.id.timeText);
                timeText.setText("1m ago");
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
    }

    public void gotoMap(View view){
        Intent intent = new Intent(this, EventMap.class);
        startActivity(intent);
    }

    private void findCurrentEventsOnServer()
    {
        double[] geo = {0.0, 0.0};
        if(geoHandler.hasGeolocation()) {
            geo = geoHandler.getGeolocation();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "We couldn't find your location! Try again later.", Toast.LENGTH_SHORT);
            return;
        }

        holdOnPopup();

        String geo_data = String.format("(%f, %f)", geo[1], geo[0]);
        Log.d("[GEO_DATA]", geo_data);
        SharedData.setKey("location", geo);

        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("current_location", geo_data);
            jsonRequest.put("distance", 100); //TODO hardcoded distance
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new getNearbyEvents().execute(jsonRequest);
    }

    private void populateListViewWithCurrentEvents(JSONObject events) throws JSONException {
        ListView listview = (ListView) findViewById(R.id.current_events_list_view);
        if (events.has("timeOut")) {
            return;
        }
        JSONArray event_list = events.getJSONArray("data");
        event_list.length();
        SharedData.setKey("event_list", event_list);
        final ArrayList<String> list = new ArrayList<String>();

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

    private class getNearbyEvents extends AsyncTask<JSONObject, Integer, JSONObject> {
        protected JSONObject doInBackground(JSONObject...input_json) {

            int number_req = input_json.length;
            JSONObject input = input_json[number_req-1]; //only process the last (most recent) request
            URL url = null;
            try {
                url = new URL("http://192.168.1.118:3000/nearby/");
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
