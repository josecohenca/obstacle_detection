package com.kukuriko.obstacle_detection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.compat.BuildConfig;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;


public class DetectedActivityReceiver extends BroadcastReceiver {

    protected static String RECEIVER_ACTION = BuildConfig.APPLICATION_ID + ".DetectedActivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (RECEIVER_ACTION == intent.getAction()) {
            Log.d("DetectedActivityReceiver", "Received an unsupported action.");
            return;
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                if (event.getActivityType()==DetectedActivity.WALKING &&
                event.getTransitionType()== ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    Log.d(MainActivity.TAG,"DetectedActivityReceiver started");
                    //Start service
                }
                else if(event.getActivityType()==DetectedActivity.WALKING &&
                        event.getTransitionType()== ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                    Log.d(MainActivity.TAG,"DetectedActivityReceiver ended");
                    //Stop service
                }
            }
        }
    }
}
