package com.xebia.activityrecognition;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactApplicationContext;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class ActivityRecognizer {
    protected static final String TAG = ActivityRecognizer.class.getSimpleName();
    private Context mContext;
    private ReactContext mReactContext;
    private GoogleApiClient mGoogleApiClient;
    private GoogleApiAvailability mGoogleApiAvailability;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private DetectionReceiver mDetectionReceiver;
    private boolean started;
    private long interval;
    private Timer mockTimer;
    private PendingIntent pendingIntent;
    public ActivityRecognizer(ReactApplicationContext reactContext) {
        mGoogleApiAvailability = GoogleApiAvailability.getInstance();
        mContext = reactContext.getApplicationContext();
        pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(DetectionReceiver.BROADCAST_ACTION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        mActivityRecognitionClient = ActivityRecognition.getClient(mContext);
        mDetectionReceiver = new DetectionReceiver(reactContext);
        mContext.registerReceiver(mDetectionReceiver, new IntentFilter(DetectionReceiver.BROADCAST_ACTION));
        mReactContext = reactContext;
        started = false;
    }

    // Subscribe to activity updates. If not connected to Google Play Services, connect first and try again from the onConnected callback.
    public void start(long detectionIntervalMillis) {
        if (!checkPlayServices()) {
            throw new Error("No Google API client. Your device likely doesn't have Google Play Services.");
        }
        interval = detectionIntervalMillis;
        if (!started) {
            Task<Void> task = mActivityRecognitionClient
                    .requestActivityUpdates(interval, pendingIntent);
            task.addOnSuccessListener(
                    new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "Succesfully added or removed activity detection updates");
                        }
                    });
            task.addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error adding or removing activity detection updates", e);
                        }
                    });
            started = true;
        }
    }

    // Subscribe to mock activity updates.
    public void startMocked(long detectionIntervalMillis, final int mockActivityType) {
        mockTimer = new Timer();
        mockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final ArrayList<DetectedActivity> detectedActivities = new ArrayList<>();
                DetectedActivity detectedActivity = new DetectedActivity(mockActivityType, 100);
                detectedActivities.add(detectedActivity);
                DetectionReceiver.onUpdate(mReactContext, detectedActivities);
            }
        }, 0, detectionIntervalMillis);

        started = true;
    }

    // Unsubscribe from mock activity updates.
    public void stopMocked() {
        if (started) {
            mockTimer.cancel();
            started = false;
        }
    }

    // Unsubscribe from activity updates and disconnect from Google Play Services. Also called when connection failed.
    public void stop() {
        if (!checkPlayServices()) {
            throw new Error("No Google API client. Your device likely doesn't have Google Play Services.");
        }

        if (started) {
            Task<Void> task = mActivityRecognitionClient
                    .removeActivityUpdates(pendingIntent);
            task.addOnSuccessListener(
                    new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "Succesfully added or removed activity detection updates");
                        }
                    });
            task.addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error adding or removing activity detection updates", e);
                        }
                    });
            started = false;
        }
    }

    // Verify Google Play Services availability
    public boolean checkPlayServices() {
        int resultCode = mGoogleApiAvailability.isGooglePlayServicesAvailable(mContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            String errorString = mGoogleApiAvailability.getErrorString(resultCode);
            if (mGoogleApiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, errorString);
            } else {
                Log.e(TAG, "This device is not supported. " + errorString);
            }
            return false;
        }
        return true;
    }



}
