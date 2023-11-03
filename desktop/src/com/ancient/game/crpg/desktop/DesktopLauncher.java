package com.ancient.game.crpg.desktop;

import com.ancient.game.crpg.Application;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
public class DesktopLauncher {

    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "CRPG Combat Prototype";

        config.setFromDisplayMode(LwjglApplicationConfiguration.getDesktopDisplayMode());
        //If I want to test windowed
        boolean fullscreen = false;
        if ( !fullscreen ) {
            config.fullscreen = false;
            config.width /= 1.2f;
            config.height /= 1.2f;
        }
        config.resizable = false;
        config.samples = 4;
        config.vSyncEnabled = true;

        new LwjglApplication(new Application(), config);
    }
}
