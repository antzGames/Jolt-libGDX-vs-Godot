package com.antz.jolt.screens;

import com.antz.jolt.Main;
import com.badlogic.gdx.ScreenAdapter;
import jolt.JoltLoader;

public class InitScreen extends ScreenAdapter {

    private boolean init = false;

    @Override
    public void show() {
       JoltLoader.init((joltSuccess, e2) -> init = joltSuccess);
    }

    @Override
    public void render(float delta) {
        if(init) {
            init = false;
            Main.main.setScreen(new JoltScreen());
        }
    }
}
