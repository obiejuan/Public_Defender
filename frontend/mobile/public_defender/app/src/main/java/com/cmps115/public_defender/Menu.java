package com.cmps115.public_defender;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cmps115.public_defender.MainActivity;

public class Menu extends AppCompatActivity {

    private TextView mStatusTextView;
    String user_name;
    String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();
        user_name = bundle.getString("user_name");
        email = bundle.getString("email");
        Log.d("User Name: ", user_name);
        setContentView(R.layout.activity_menu);
    }

    @Override
    public void onStart(){
        super.onStart();
        mStatusTextView = (TextView) findViewById(R.id.textView6);
        mStatusTextView.setText(getString(R.string.signed_in_fmt, user_name));
    }

    public void goHome(View view) {
//        Intent intent = new Intent(this, MainActivity.class);
////        setContentView(R.layout.activity_main);
//        startActivity(intent);
        finish();
    }

    public void gotoSettings(View view){
        Intent intent = new Intent(this, Settings.class);
        Log.d("Settings", "Settings");
        startActivity(intent);
    }

    public void gotoMyRecordings(View view){
        Intent intent = new Intent(this, FileBrowser.class);
        startActivity(intent);
    }
}
