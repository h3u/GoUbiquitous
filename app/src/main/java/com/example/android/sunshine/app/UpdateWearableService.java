package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.app.common.data.Weather;
import com.example.android.sunshine.app.common.data.WeatherData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;

/**
 * An {@link IntentService} subclass to send weather data to connected wearable.
 */
public class UpdateWearableService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String LOG_TAG = "UpdateWearableService";

    public static final String KEY_WEATHER_UPDATE_INTENT = "key_weather_update_intent";

    private GoogleApiClient mGoogleApiClient;

    public UpdateWearableService() {
        super("UpdateWearableService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient.blockingConnect();
        Weather actual = intent.getParcelableExtra(KEY_WEATHER_UPDATE_INTENT);
        String actualJson = "{}";
        try {
            actualJson = actual.toJson();
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(WeatherData.PATH);
        putDataMapReq.getDataMap().putString(WeatherData.KEY_DATA, actualJson);
        putDataMapReq.getDataMap().putLong("ts", System.currentTimeMillis());
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

        if (mGoogleApiClient.isConnected()) {
            Wearable.DataApi.putDataItem(
                    mGoogleApiClient, putDataReq).await();
        } else {
            Log.e(LOG_TAG, "Failed to send data item: " + putDataMapReq
                    + " - Client disconnected from Google Play Services");
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "onConnectionFailed: " + connectionResult.getErrorMessage() );
    }
}
