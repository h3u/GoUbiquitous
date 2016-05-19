package com.example.android.sunshine.app.common.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Hold minimal data for actual weather with high-/low-temperature.
 * Created by Uli Wucherer (u.wucherer@gmail.com) on 05/05/16.
 */
public class Weather implements Parcelable {

    private static final String KEY_ID = "id";
    private static final String KEY_HIGH_TEMPERATURE = "high_temp";
    private static final String KEY_LOW_TEMPERATURE = "low_temp";
    private static final String KEY_RESOURCE_NAME = "resource_name";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final long MAX_AGE = 1000 * 60 * 60 * 3; // 3 hours

    private int mWeatherId;
    private double mHighTemperature;
    private double mLowTemperature;
    private String mResourceName;
    private long mTimestamp;

    public Weather(int weatherId, double highTemperature, double lowTemperature, String resourceName) {
        mResourceName = resourceName;
        mHighTemperature = highTemperature;
        mLowTemperature = lowTemperature;
        mWeatherId = weatherId;
        mTimestamp = System.currentTimeMillis();
    }

    public Weather() {
        mResourceName = "";
        mHighTemperature = Double.MIN_VALUE;
        mLowTemperature = Double.MIN_VALUE;
        mTimestamp = System.currentTimeMillis();
    }

    public boolean hasData() {
        return !mResourceName.equals("")
                && mHighTemperature != Double.MIN_VALUE
                && mLowTemperature != Double.MIN_VALUE;
    }

    public boolean isOutdated() {
        return (mTimestamp + MAX_AGE) < System.currentTimeMillis();
    }

    public String getResourceName() {
        return mResourceName;
    }

    public double getHighTemperature() {
        return mHighTemperature;
    }

    public double getLowTemperature() {
        return mLowTemperature;
    }

    public int getWeatherId() {
        return mWeatherId;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public String toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(KEY_ID, mWeatherId);
        object.put(KEY_HIGH_TEMPERATURE, mHighTemperature);
        object.put(KEY_LOW_TEMPERATURE, mLowTemperature);
        object.put(KEY_RESOURCE_NAME, mResourceName);
        object.put(KEY_TIMESTAMP, mTimestamp);

        return object.toString();
    }

    public static Weather fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        if (json.has(KEY_ID) && json.has(KEY_HIGH_TEMPERATURE)
                && json.has(KEY_LOW_TEMPERATURE) && json.has(KEY_RESOURCE_NAME)) {

            Weather latest = new Weather(
                    json.getInt(KEY_ID),
                    json.getDouble(KEY_HIGH_TEMPERATURE),
                    json.getDouble(KEY_LOW_TEMPERATURE),
                    json.getString(KEY_RESOURCE_NAME));
            if (json.has(KEY_TIMESTAMP)) latest.setTimestamp(json.getLong(KEY_TIMESTAMP));

            return latest;
        }

        return new Weather();
    }

    @Override
    public String toString() {

        return "Weather{" +
                "mResourceName='" + mResourceName + '\'' +
                ", mWeatherId=" + mWeatherId +
                ", mTimestamp=" + mTimestamp +
                ", mHighTemperature=" + mHighTemperature +
                ", mLowTemperature=" + mLowTemperature +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mWeatherId);
        dest.writeDouble(mHighTemperature);
        dest.writeDouble(mLowTemperature);
        dest.writeString(mResourceName);
        dest.writeLong(mTimestamp);
    }

    protected Weather(Parcel in) {
        mWeatherId = in.readInt();
        mHighTemperature = in.readDouble();
        mLowTemperature = in.readDouble();
        mResourceName = in.readString();
        mTimestamp = in.readLong();
    }

    public static final Creator<Weather> CREATOR = new Creator<Weather>() {
        @Override
        public Weather createFromParcel(Parcel source) {
            return new Weather(source);
        }

        @Override
        public Weather[] newArray(int size) {
            return new Weather[size];
        }
    };
}
