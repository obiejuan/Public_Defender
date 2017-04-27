package com.cmps115.public_defender;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

/*
Please note that if you want to change the draw/drop theme for this activity you need to ensure you're setting the contraints.
This means that you will need to hit the little golden stars after you place an element. It is the bar above the drag/drop editor.
-Oliver
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void broadCast(View view) {
        // Do something in response to Record button
        Context context = getApplicationContext();
        CharSequence text = "Hit Record";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
       }

    public void gotoMenu(View view){
        Intent intent = new Intent(this, Menu.class);
        Log.d("Menu", "clicked menu");
        startActivity(intent);
    }

    public void gotoCurrentEvents(View view){
        Intent intent = new Intent(this, CurrentEvents.class);
        startActivity(intent);
    }

}
