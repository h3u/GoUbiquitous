package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;


/**
 * An {@link IntentService} to initiate a sync of weather data on handheld.
 */
public class RefreshWeatherIntentService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "RefreshWeatherService";
    private static final String REFRESH_WEATHER_CAPABILITY_NAME = "weather_refresh";
    private static final String PATH_MESSAGE = "/refresh-weather";

    private GoogleApiClient mGoogleApiClient;

    public RefreshWeatherIntentService() {
        super("RefreshWeatherIntentService");
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

    private String getCapableNode(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : connectedNodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient.blockingConnect();

        CapabilityApi.GetCapabilityResult result =
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient, REFRESH_WEATHER_CAPABILITY_NAME,
                        CapabilityApi.FILTER_REACHABLE).await();

        String nodeId = getCapableNode(result.getCapability());

        if (mGoogleApiClient.isConnected() && nodeId != null) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, PATH_MESSAGE, new byte[0])
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Logger.error(TAG, "Failed to connect to Google Api Client with status "
                                        + sendMessageResult.getStatus());
                            }
                        }
                    });
        } else {
            Logger.error(TAG, "Failed to send message: missing connection or node");
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
