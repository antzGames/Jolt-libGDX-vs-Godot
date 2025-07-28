package com.antz.jolt;

import com.antz.jolt.screens.InitScreen;
import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    public static Main main;
    public static int threads;

    public Main(int threadsIn) {
        threads = threadsIn;
    }

    @Override
    public void create() {
        main = this;
        setScreen(new InitScreen());
    }
}
