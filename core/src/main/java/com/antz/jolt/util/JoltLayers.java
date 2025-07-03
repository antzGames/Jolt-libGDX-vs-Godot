package com.antz.jolt.util;

public class JoltLayers {
    // Object layers
    public static int NON_MOVING = 4;
    public static int MOVING = 5;
    public static int DEBRIS = 6; // Example: Debris collides only with NON_MOVING
    public static int SENSOR = 7; // Sensors only collide with MOVING objects
    public static int NUM_LAYERS = 8;
}
