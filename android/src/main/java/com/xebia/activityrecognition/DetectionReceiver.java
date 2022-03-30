package com.xebia.activityrecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactApplicationContext;

import java.util.ArrayList;

public class DetectionReceiver extends BroadcastReceiver {
    protected static final String TAG = DetectionReceiver.class.getSimpleName();
    protected static final String PACKAGE_NAME = DetectionReceiver.class.getPackage().getName();
    protected static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    private final ReactContext mReactContext;

    public DetectionReceiver(ReactContext reactContext) {
        this.mReactContext = reactContext;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onHandleIntent");
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();


        Log.d(TAG, "Detected activities:");
        for (DetectedActivity da : detectedActivities) {
            Log.d(TAG, getActivityString(da.getType()) + " (" + da.getConfidence() + "%)");
        }

        onUpdate(mReactContext, detectedActivities);
    }

    public static String getActivityString(int detectedActivityType) {
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON_BICYCLE";
            case DetectedActivity.ON_FOOT:
                return "ON_FOOT";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            case DetectedActivity.UNKNOWN:
                return "UNKNOWN";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "UNIDENTIFIABLE";
        }
    }

    // Create key-value map with activity recognition result
    static void onUpdate(ReactContext context, ArrayList<DetectedActivity> detectedActivities) {
        WritableMap params = Arguments.createMap();
        for (DetectedActivity activity : detectedActivities) {
            params.putInt(DetectionReceiver.getActivityString(activity.getType()), activity.getConfidence());
        }
        sendEvent(context, "DetectedActivity", params);
    }

    // Send result back to JavaScript land
    static private void sendEvent(ReactContext context, String eventName, @Nullable WritableMap params) {
        try {
            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
        } catch (RuntimeException e) {
            Log.e(TAG, "java.lang.RuntimeException: Trying to invoke JS before CatalystInstance has been set!", e);
        }
    }
}
