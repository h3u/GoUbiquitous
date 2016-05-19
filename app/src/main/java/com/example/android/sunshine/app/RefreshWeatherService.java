package com.example.android.sunshine.app;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Service to listen for a refresh request of connected wearable.
 */
public class RefreshWeatherService extends WearableListenerService {

    private static final String PATH_MESSAGE = "/refresh-weather";

    public RefreshWeatherService() {}

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (messageEvent.getPath().equals(PATH_MESSAGE)) {
            SunshineSyncAdapter.syncImmediately(this);
        }
    }
}
