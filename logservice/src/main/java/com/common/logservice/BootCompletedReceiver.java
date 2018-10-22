package com.common.logservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;  
import android.util.Log;


public class BootCompletedReceiver extends BroadcastReceiver { 
    
    private static final String TAG = BootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v(TAG, "onReceive action = " + action);
        if (action != null && Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Intent newIntent = new Intent(context, LogService.class);
            context.startService(newIntent);
            Log.v(TAG, "onReceive startService");
        } else {
            Log.v(TAG, "onReceive do nothing");
        }
    }
}