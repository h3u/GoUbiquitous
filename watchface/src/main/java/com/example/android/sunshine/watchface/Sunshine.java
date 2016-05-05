/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class Sunshine extends CanvasWatchFaceService {

    private static final String TAG = "Sunshine";

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Sunshine.Engine> mWeakReference;

        public EngineHandler(Sunshine.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Sunshine.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener {
        private static final String TAG = "Engine";
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mHourTextPaint;
        Paint mColonTextPaint;
        Paint mMinuteTextPaint;
        Paint mDateTextPaint;
        Paint mSeparatorPaint;
        Paint mWeatherIconPaint;
        Bitmap mWeatherIconBitmapAmbient;
        Bitmap mWeatherIconBitmap;
        Paint mTemperatureHighTextPaint;
        Paint mTemperatureLowTextPaint;

        Time mTime;
        private boolean mHasTemperature;
        private int mTemperatureHigh;
        private int mTemperatureLow;
        private int mPrimaryTextColor;
        private int mSecondaryTextColor;
        private int mAmbientTextColor;
        private float mCenterX;
        private float mCenterY;
        private float mCenterPaddingY;
        private float mTimeTextHeight;
        private float mTemperatureTextHeight;
        private float mTemperatureMargin;
        private static final float SEPARATOR_STROKE_WIDTH = 2f;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        /**
         * Ambient mode
         */
        boolean mAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(Sunshine.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.RIGHT) // place bar right for square watchfaces
                    .setViewProtectionMode(WatchFaceStyle.PROTECT_STATUS_BAR | WatchFaceStyle.PROTECT_HOTWORD_INDICATOR)
                    .setHotwordIndicatorGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL)
                    .build());
            Resources resources = Sunshine.this.getResources();

            // colors that need to be switched
            mAmbientTextColor = resources.getColor(R.color.ambient_mode_text);
            mPrimaryTextColor = resources.getColor(R.color.primary_text);
            mSecondaryTextColor = resources.getColor(R.color.secondary_text);

            mCenterPaddingY = resources.getDimension(R.dimen.vertical_center_padding);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mHourTextPaint = createTextPaint(mPrimaryTextColor);
            mColonTextPaint = createTextPaint(mPrimaryTextColor);
            mMinuteTextPaint = createTextPaint(mPrimaryTextColor);
            mTimeTextHeight = resources.getDimension(R.dimen.time_text_size_round);

            mSeparatorPaint = new Paint();
            mSeparatorPaint.setStrokeWidth(SEPARATOR_STROKE_WIDTH);

            mDateTextPaint = createTextPaint(mSecondaryTextColor);

            mWeatherIconPaint = new Paint();

            mTemperatureHighTextPaint = createTextPaint(mPrimaryTextColor);
            mTemperatureLowTextPaint = createTextPaint(mSecondaryTextColor);
            mTemperatureTextHeight = resources.getDimension(R.dimen.temperature_text_size_round);
            mTemperatureMargin = resources.getDimension(R.dimen.temperature_horizontal_margin);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mCenterX = width / 2f;
            mCenterY = height / 2f;
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            Sunshine.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            Sunshine.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = Sunshine.this.getResources();
            boolean isRound = insets.isRound();

            float timeTextSize = resources.getDimension(isRound
                    ? R.dimen.time_text_size_round : R.dimen.time_text_size);
            mHourTextPaint.setTextSize(timeTextSize);
            mHourTextPaint.setTypeface(BOLD_TYPEFACE);
            mColonTextPaint.setTextSize(timeTextSize);
            mColonTextPaint.setTypeface(BOLD_TYPEFACE);
            mMinuteTextPaint.setTextSize(timeTextSize);

            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.date_text_size_round : R.dimen.date_text_size);
            mDateTextPaint.setTextSize(dateTextSize);

            float highTempTextSize = resources.getDimension(isRound
                    ? R.dimen.temperature_text_size_round : R.dimen.temperature_text_size);
            mTemperatureHighTextPaint.setTextSize(highTempTextSize);
            mTemperatureHighTextPaint.setTypeface(BOLD_TYPEFACE);
            mTemperatureLowTextPaint.setTextSize(highTempTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHourTextPaint.setAntiAlias(!inAmbientMode);
                    mColonTextPaint.setAntiAlias(!inAmbientMode);
                    mMinuteTextPaint.setAntiAlias(!inAmbientMode);
                    mDateTextPaint.setAntiAlias(!inAmbientMode);
                    mTemperatureHighTextPaint.setAntiAlias(!inAmbientMode);
                    mTemperatureLowTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
                mSeparatorPaint.setColor(mAmbientTextColor);
                mDateTextPaint.setColor(mAmbientTextColor);
                mTemperatureLowTextPaint.setColor(mAmbientTextColor);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
                mSeparatorPaint.setColor(mSecondaryTextColor);
                mDateTextPaint.setColor(mSecondaryTextColor);
                mTemperatureLowTextPaint.setColor(mSecondaryTextColor);
            }

            float hourWidth;
            float colonWidth;
            float maxTempWidth;
            mTime.setToNow();
            Locale locale = Locale.getDefault();

            // separator line in center of screen
            float separatorLength = getResources().getDimension(R.dimen.separator_length);
            canvas.drawLine(
                    mCenterX - (separatorLength / 2), mCenterY,
                    mCenterX + (separatorLength / 2), mCenterY,
                    mSeparatorPaint);

            // date text
            String dateText = String.format(locale, "%tA %te %tb",
                    mTime.toMillis(true), mTime.toMillis(true), mTime.toMillis(true));
            canvas.drawText(dateText,
                    mCenterX - (mDateTextPaint.measureText(dateText)/2),
                    mCenterY - mCenterPaddingY,
                    mDateTextPaint);

            // time text
            // format strings and collect sizes
            String hourText = String.format(locale, "%d", mTime.hour);
            hourWidth = mHourTextPaint.measureText(hourText);

            String colonText = ":";
            colonWidth = mColonTextPaint.measureText(colonText);
            String minuteText = String.format(locale, "%02d", mTime.minute);

            // hour text
            canvas.drawText(hourText,
                    mCenterX - hourWidth - (colonWidth/2),
                    mCenterY - mTimeTextHeight,
                    mHourTextPaint);

            // colon text
            canvas.drawText(colonText,
                    mCenterX - (colonWidth/2),
                    mCenterY - mTimeTextHeight,
                    mColonTextPaint);

            // minute text
            canvas.drawText(minuteText,
                    mCenterX + (colonWidth/2),
                    mCenterY - mTimeTextHeight,
                    mMinuteTextPaint);

            if (mHasTemperature) {
                // high temp text
                String tempHighText = String.format(locale, "%2d°", mTemperatureHigh);
                maxTempWidth = mTemperatureHighTextPaint.measureText(tempHighText);
                canvas.drawText(tempHighText,
                        mCenterX - (maxTempWidth / 2),
                        mCenterY + mCenterPaddingY + mTemperatureTextHeight,
                        mTemperatureHighTextPaint);

                // low temp text
                String tempLowText = String.format(locale, "%2d°", mTemperatureLow);
                canvas.drawText(tempLowText,
                        mCenterX + (maxTempWidth / 2) + mTemperatureMargin,
                        mCenterY + mCenterPaddingY + mTemperatureTextHeight,
                        mTemperatureLowTextPaint);

                // weather icon
                if (isInAmbientMode()) {
                    canvas.drawBitmap(mWeatherIconBitmapAmbient,
                            mCenterX - (maxTempWidth/2) - mTemperatureMargin - mWeatherIconBitmapAmbient.getWidth(),
                            mCenterY + mCenterPaddingY,
                            mWeatherIconPaint);
                } else {
                    canvas.drawBitmap(mWeatherIconBitmap,
                            mCenterX - (maxTempWidth/2) - mTemperatureMargin - mWeatherIconBitmap.getWidth(),
                            mCenterY + mCenterPaddingY,
                            mWeatherIconPaint);
                }
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent:dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    if (path.equals("/weather-today")) {
                        mTemperatureHigh = dataMap.getInt("high-temp");
                        mTemperatureLow = dataMap.getInt("low-temp");
                        String weatherCondition = dataMap.getString("weather-condition");
                        if (weatherCondition != null) {
                            // active mode
                            mWeatherIconBitmap = createFromCondition("ic_%s", weatherCondition);
                            // ambient mode
                            mWeatherIconBitmapAmbient = createFromCondition("ic_%s_ambient", weatherCondition);
                        }
                        mHasTemperature = true;
                        invalidate();
                    }
                }
            }
        }

        private Bitmap createFromCondition(String format, String condition) {
            String resourceName = String.format(format, condition);
            int resource = 0;
            try {
                resource = R.drawable.class.getField(resourceName).getInt(null);
            } catch (Exception e) {
                Log.e(TAG, "createFromCondition: " + e.getLocalizedMessage());
            }
            return BitmapFactory.decodeResource(getResources(), resource);
        }
    }
}
