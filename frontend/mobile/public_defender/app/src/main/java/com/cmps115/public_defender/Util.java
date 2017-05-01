package com.cmps115.public_defender;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.support.v7.app.AppCompatActivity;


/**
 * Created by seth on 4/26/17.
 */

public class Util extends AppCompatActivity implements View.OnClickListener{

    public Util() {

    }
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        setContentView(R.layout.activity_main);
        startActivity(intent);
        //goHome(view);
    }

    private void gotoMenu(View view){
        Intent intent = new Intent(this, Menu.class);
        Log.d("Settings", "clicked menu");
        startActivity(intent);
    }

}
