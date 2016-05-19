package com.example.android.sunshine.app.common;

import com.example.android.sunshine.app.common.data.Weather;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class WeatherUnitTest {

    /**
     * Test instance without data return false.
     * Instance with data given returns true.
     */
    @Test
    public void hasDataReflectCondition() {
        Weather w = new Weather();
        assertFalse(w.hasData());

        w = new Weather(1, 20L, 15L, "clear");
        assertTrue(w.hasData());
    }

    /**
     *  Weather instance isn't outdated within 3 hours after instantiation.
     */
    @Test
    public void isOutdated() {
        Weather w = new Weather();
        assertFalse(w.isOutdated());

        long current = System.currentTimeMillis();
        w.setTimestamp(current - (1000 * 60 * 60 * 3 + 1));
        assertTrue(w.isOutdated());
    }
}