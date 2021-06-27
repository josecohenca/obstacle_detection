package com.kukuriko.obstacle_detection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainService extends Service {

    private Context myContext;
    private int[][] depthResult;
    public static final String NOTIFICATION = "com.kukuriko.obstacle_detection.MainService";
    private String notifChannelId = "my_channel_obstacle";
    private static int FOREGROUND_ID = 56790;
    private NotificationManager mNotificationManager;
    private NotificationChannel mChannel;
    private Notification notification;
    private Config config;
    private Session session;
    private Thread startDetecting;
    private boolean isRunning=false;

    @Override
    public void onCreate(){
        super.onCreate();
        myContext = this.getApplicationContext();
        mNotificationManager = (NotificationManager) myContext.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

        Log.i(MainActivity.TAG, "Service onCreate");

        CharSequence text = getText(R.string.remote_service_started);
        int importance = NotificationManager.IMPORTANCE_LOW;
        if (Build.VERSION.SDK_INT >= 26) {
            mChannel = new NotificationChannel(notifChannelId, text, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);

        // Set the info for the views that show in the notification panel.

        notification = new NotificationCompat.Builder(this, notifChannelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)  // the status icon
                .setTicker(text)  // the status text
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();


        createSession();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.i(MainActivity.TAG, "Service onStartCommand");
        startForeground(FOREGROUND_ID, notification);
        Log.i(MainActivity.TAG, "Start foreground");

        mNotificationManager.notify(FOREGROUND_ID, notification);

        isRunning = true;
        startDetecting = new Thread(new Runnable(){
            public void run() {
                startDetectingLoop();
            }
        });
        startDetecting.start();

        return Service.START_NOT_STICKY;
    }

    private void startDetectingLoop(){
        while(isRunning) {
            if (session == null) {
                return;
            }
            Frame frame;
            try {
                frame = session.update();
            } catch (CameraNotAvailableException e) {
                Log.e(MainActivity.TAG, "Camera not available during startDetectingLoop", e);
                return;
            }
            Camera camera = frame.getCamera();

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                try (Image depthImage = frame.acquireDepthImage()) {
                    Image.Plane plane = depthImage.getPlanes()[0];
                    int pixelStride = plane.getPixelStride();
                    int rowStride = plane.getRowStride();
                    ByteBuffer buffer = plane.getBuffer().order(ByteOrder.nativeOrder());
                    int maxHeight = depthImage.getHeight() / rowStride;
                    int maxWidth = depthImage.getWidth() / pixelStride;
                    depthResult = new int[maxWidth][maxHeight];
                    for (int i = 0; i < maxWidth; i++) {
                        for (int j = 0; j < maxHeight; j++) {
                            depthResult[i][j] = getMillimetersDepth(buffer, i, j, pixelStride, rowStride);
                        }
                    }
                } catch (NotYetAvailableException e) {
                    // This normally means that depth data is not available yet. This is normal so we will not
                    // spam the logcat with this.
                    Log.d(MainActivity.TAG, "Depth Camera not available yet");
                    continue;
                }
            }
        }
    }

    private int getMillimetersDepth(ByteBuffer buffer, int x, int y, int pixelStride, int rowStride) {
        // The depth image has a single plane, which stores depth for each
        // pixel as 16-bit unsigned integers.
        int byteIndex = x * pixelStride + y * rowStride;
        short depthSample = buffer.getShort(byteIndex);
        return depthSample;
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        isRunning = false;
        session.close();
        Log.i( MainActivity.TAG,  "Service onDestroy");
        stopForeground(true);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void createSession() {
        try {
            // Create a new ARCore session.
            session = new Session(this);

            config = session.getConfig();

            // Check whether the user's device supports the Depth API.
            boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            if (isDepthSupported) {
                config.setDepthMode(Config.DepthMode.AUTOMATIC);
            }
            session.configure(config);
        }
        catch(Exception e){

        }
    }

}
