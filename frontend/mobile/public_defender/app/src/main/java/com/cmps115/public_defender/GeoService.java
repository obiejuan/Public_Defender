package com.cmps115.public_defender;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;


public class GeoService extends Service {

    private final IBinder mBinder = new GeoBinder();

    public class GeoBinder extends Binder { GeoService getService() {
            return GeoService.this;
        } }

    @Override
    public void onCreate() { }

    public void init_threads(Handler handler){
         final Runnable r = new Runnable() {
            public void run() {
                // this is where the geo computations need to happen.
                // this does not include the listeners or anything like that.
                // equiv. to the run() function inside GetLastLocation in the ReliableGeolocationProvider.java
            }
        };
        handler.postDelayed(r, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_REDELIVER_INTENT; }

    @Override
    public void onDestroy() { }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}

