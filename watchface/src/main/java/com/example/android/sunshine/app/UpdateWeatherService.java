package com.example.android.sunshine.app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.android.sunshine.app.common.data.WeatherData;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class UpdateWeatherService extends WearableListenerService {
    private static final String TAG = "UpdateWeatherService";
    public static final String KEY_PREFERENCES_WEATHER = "key_preferences_weather";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent dataEvent:dataEventBuffer) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (path.equals(WeatherData.PATH)) {
                    persistToPreferences(dataMap);
                }
            }
        }
    }

    private boolean persistToPreferences(DataMap dataMap) {
        String json = dataMap.get(WeatherData.KEY_DATA);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_PREFERENCES_WEATHER, json);
        return editor.commit();
    }
}