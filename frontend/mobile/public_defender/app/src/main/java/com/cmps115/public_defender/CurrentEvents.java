package com.cmps115.public_defender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CurrentEvents extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_events);
        ListView listview = (ListView) findViewById(R.id.current_events);
        String[] values = new String[] { "Pulled over at dank memes", "Getting pulled over melt steel beams", "Oww", "Six angry men" , "Getting pulled over melt steel beams", "Oww", "Six angry men" , "Getting pulled over melt steel beams", "Oww", "Six angry men" , "Getting pulled over melt steel beams", "Oww", "Six angry men" };

        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
        final CustomArrayAdaptor adapter = new CustomArrayAdaptor(this, android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);
    }

    private class CustomArrayAdaptor extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public CustomArrayAdaptor(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);
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

    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void refresh(View view) {

    }
}
