package com.cmps115.public_defender;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuInflater;
import android.view.MenuItem;


/**
 * Created by Brandon on 05/31/17.
 */

public class AppCompatActivityWithPDMenu extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        getSupportActionBar().setTitle("Public Defender");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_sendToFAQ:
                Intent sendToFAQ = new Intent(this, HowtoFaqs.class);
                this.startActivity(sendToFAQ);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

}
