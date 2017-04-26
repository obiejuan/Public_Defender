package com.cmps115.public_defender;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        setContentView(R.layout.activity_main);
        startActivity(intent);
    }

}
