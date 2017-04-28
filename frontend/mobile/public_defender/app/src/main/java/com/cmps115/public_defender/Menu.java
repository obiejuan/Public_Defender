package com.cmps115.public_defender;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.cmps115.public_defender.MainActivity;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        setContentView(R.layout.activity_main);
        startActivity(intent);
    }

    public void gotoSettings(View view){
        Intent intent = new Intent(this, Settings.class);
        Log.d("Settings", "Settings");
        startActivity(intent);
    }

    public void gotoMyRecordings(View view){
        Intent intent = new Intent(this, MyRecordings.class);
        startActivity(intent);
    }
}
