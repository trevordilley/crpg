package com.ancient.game.crpg.desktop;

import com.ancient.game.crpg.Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {

    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("CRPG Combat Prototype");
        config.setWindowedMode(1920, 1080);
        config.setResizable(false);
        // Fixes the screen tearing seen during the 2023 revival work.
        config.useVsync(true);
        // r, g, b, a, depth, stencil, msaa samples — samples=4 matches the old config.
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);

        // NOTE: deliberately not calling setOpenGLEmulation(ANGLE_GLES20, ...).
        // That was a 2023 workaround for the HyperLap2D texture-array renderer,
        // which we no longer use. If the field-of-view framebuffer misbehaves on
        // Apple Silicon, re-adding it (plus the gdx-lwjgl3-angle dependency) is
        // the first thing to try.

        new Lwjgl3Application(new Application(), config);
    }
}
