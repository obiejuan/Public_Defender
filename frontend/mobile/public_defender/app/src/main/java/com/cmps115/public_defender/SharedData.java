package com.cmps115.public_defender;

/**
 * Created by bryan on 5/20/17.
 *
 *    Usage instructions:
 *    *************************************************
 *    Saving variables:
 *       SharedData.setKey("key", new Object());
 *    Retrieving variables:
 *       Object val = SharedData.getKey("AwesomeData");
 *
 */

import java.util.concurrent.ConcurrentHashMap;

public class SharedData {

    private ConcurrentHashMap data = new ConcurrentHashMap();

    private Object _getKey(String key) { return data.get(key); }
    private void _setKey(String key, Object data) {
        this.data.put(key, data);
    }


    public static void setKey(String key, Object data){
        getInstance()._setKey(key, data);
    }
    public static Object getKey(String key){
        return getInstance()._getKey(key);
    }


    private static final SharedData share = new SharedData();
    private static SharedData getInstance() {return share;}
}

