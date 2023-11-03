package com.ancient.game.crpg.desktop;

import com.ancient.game.crpg.Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
public class DesktopLauncher {

    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("CRPG Combat Prototype");

     //   config.setFromDisplayMode(Lwjgl3ApplicationConfiguration.getDesktopDisplayMode());
        //If I want to test windowed
////        boolean fullscreen = false;
////        if ( !fullscreen ) {
////            config.setFullscreenMode();
////            config.width /= 1.2f;
////            config.height /= 1.2f;
////        }
//        config.resizable = false;
//        config.samples = 4;
//        config.vSyncEnabled = true;

        new Lwjgl3Application(new Application(), config);
    }
}
