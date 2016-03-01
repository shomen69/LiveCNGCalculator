package com.shomen.smn.livecngcalculator;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class BackgroundService extends Service implements LocationListener,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static boolean IS_SERVICE_RUNNING = false;
    private static final String TAG = "BackgroundService";

    private List<Double> latitudes = new ArrayList<>();
    private List<Double> longitudes = new ArrayList<>();

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private IBinder mBinder = new ServiceBinder();

    @Override
    public IBinder onBind(Intent arg0){
        Log.d(TAG, "inside onbind....");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "inside onUnbind....");
        return true;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");

        buildGoogleApiClient();
        googleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");
            showNotification();
        }
        return START_NOT_STICKY;
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "onLocationChanged in fused " + location);
        latitudes.add(location.getLatitude());
        longitudes.add(location.getLongitude());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected fused");
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended fused");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed fused: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();
    }

    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);

    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.smartphone);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Live Track")
                .setTicker("Live Track")
                .setContentText("Live Track")
                .setSmallIcon(R.drawable.smartphone)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();

        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                notification);

    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
        super.onDestroy();
    }

/*    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "inside onTaskRemoved....");
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
        super.onTaskRemoved(rootIntent);
    }*/

    public List<Double> getLatitudesFromService(){
        return latitudes;
    }

    public List<Double> getLongitudesFromService(){
        return longitudes;
    }

    public class ServiceBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

}
