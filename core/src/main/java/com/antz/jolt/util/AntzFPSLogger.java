package com.antz.jolt.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;

/** A simple helper class to log the frames per seconds achieved. Just invoke the {@link #log()} method in your rendering method.
 * The output will be logged once per second.
 *
 * @author mzechner */
public class AntzFPSLogger {
    private long startTime;
    private int bound;
    private int frameNumber;
    public boolean onlyReportMode = false;

    public int min = 99999, average, max, fps;

    public AntzFPSLogger() {
        this(Integer.MAX_VALUE);
    }

    public void reset(String message){
        Gdx.app.log("AntzFPSLogger", message + " FPS MIN: " + min + "   FPS AVG: " + average + "   FPS MAX: " + max);
        min = 99999; average = 0; max = 0; frameNumber = 0; fps = 0;
    }

    /** @param bound only logs when they frames per second are less than the bound */
    public AntzFPSLogger(int bound) {
        this.bound = bound;
        startTime = TimeUtils.nanoTime();
    }

    public void setBound (int bound) {
        this.bound = bound;
        startTime = TimeUtils.nanoTime();
    }

    /** Logs the current frames per second to the console. */
    public void log () {
        final long nanoTime = TimeUtils.nanoTime();
        if (nanoTime - startTime > 1000000000) /* 1,000,000,000ns == one second */ {
            fps = Gdx.graphics.getFramesPerSecond();
            if (fps < bound) {
                startTime = nanoTime;
                frameNumber++;
                min = Math.min(min, fps);
                max = Math.max(max, fps);
                average =  Math.round((float)(average * (frameNumber - 1.0) / frameNumber + ((float) fps / frameNumber)));

                if (!onlyReportMode)
                    Gdx.app.log("AntzFPSLogger", "FPS: " + fps + "   MIN: " + min + "   AVG: " + average + "   MAX: " + max);
            }
        }
    }
}
