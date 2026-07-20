package com.ancient.game.crpg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import java.util.zip.Deflater

/**
 * Writes the current framebuffer to a PNG.
 *
 * This exists because macOS `screencapture` cannot see window contents without
 * Screen Recording permission — it silently returns byte-identical frames of the
 * desktop and menu bar, which is easy to misread as "the game renders black".
 * Reading the framebuffer from inside the app is the only trustworthy way to see
 * what actually got drawn.
 *
 * Two ways in:
 *
 *  - Press F12 while the game is running.
 *  - Run with `-Dcrpg.capture=<frame>` to capture automatically on that frame
 *    number and, unless told otherwise, exit afterwards. This is the mode to use
 *    when something else (a script, an agent) needs a deterministic artefact
 *    without touching the keyboard:
 *
 *      ./gradlew :desktop:run -Dcrpg.capture=60 -Dcrpg.capture.out=/tmp/frame.png
 *
 *    Note Gradle does not forward -D to the forked JVM by default; the `run`
 *    task in desktop/build.gradle passes these through explicitly.
 */
object Screenshot {

    private val log = gameLogger(Screenshot::class.java)

    private val captureFrame: Int? = System.getProperty("crpg.capture")?.toIntOrNull()
    private val captureOut: String = System.getProperty("crpg.capture.out") ?: "/tmp/crpg-frame.png"
    private val captureExit: Boolean = System.getProperty("crpg.capture.exit") != "false"

    private var frame = 0

    /**
     * Call once per rendered frame, after everything has been drawn. Handles both
     * the F12 binding and the automated `-Dcrpg.capture` path.
     */
    fun update() {
        frame++

        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            capture(nextManualFile())
        }

        if (captureFrame != null && frame == captureFrame) {
            capture(resolve(captureOut))
            if (captureExit) {
                log.info("Capture complete, exiting (set -Dcrpg.capture.exit=false to keep running)")
                Gdx.app.exit()
            }
        }
    }

    /**
     * Dumps the numbers that decide where things land on screen. Logical vs
     * backbuffer size disagreeing is the HiDPI case; worldWidth/Height and the
     * camera tell you what slice of the world should be visible, which is what
     * you compare a capture against.
     */
    fun logRenderState(tag: String, viewport: com.badlogic.gdx.utils.viewport.Viewport) {
        val cam = viewport.camera
        log.info(
            """
            --- render state [$tag] ---
            logical      : ${Gdx.graphics.width} x ${Gdx.graphics.height}
            backbuffer   : ${Gdx.graphics.backBufferWidth} x ${Gdx.graphics.backBufferHeight}
            density      : ${Gdx.graphics.density}
            viewport scr : ${viewport.screenWidth} x ${viewport.screenHeight} @ (${viewport.screenX}, ${viewport.screenY})
            viewport world: ${viewport.worldWidth} x ${viewport.worldHeight}
            camera pos   : ${cam.position}
            camera vp    : ${cam.viewportWidth} x ${cam.viewportHeight}
            unitsPerPixel: ${(viewport as? com.badlogic.gdx.utils.viewport.ScreenViewport)?.unitsPerPixel}
            visible world: x ${cam.position.x - viewport.worldWidth / 2} .. ${cam.position.x + viewport.worldWidth / 2}
                           y ${cam.position.y - viewport.worldHeight / 2} .. ${cam.position.y + viewport.worldHeight / 2}
            """.trimIndent()
        )
    }

    /** Grabs the whole backbuffer. Returns null if the read or write failed. */
    fun capture(file: FileHandle): FileHandle? {
        // Backbuffer, not logical size: on a HiDPI display these differ (e.g.
        // 3840x2160 vs 1920x1080) and the logical size would crop the capture.
        val width = Gdx.graphics.backBufferWidth
        val height = Gdx.graphics.backBufferHeight

        return try {
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height)
            try {
                // flipY = true: GL's origin is bottom-left, PNG's is top-left.
                // Without this the image comes out upside down.
                PixmapIO.writePNG(file, pixmap, Deflater.DEFAULT_COMPRESSION, true)
            } finally {
                pixmap.dispose()
            }
            log.info("Screenshot ${width}x${height} -> ${file.file().absolutePath}")
            file
        } catch (e: Exception) {
            log.error("Screenshot failed: ${e.message}")
            null
        }
    }

    private fun resolve(path: String): FileHandle =
        if (path.startsWith("/")) Gdx.files.absolute(path) else Gdx.files.local(path)

    /**
     * screenshots/crpg-0001.png, next unused index. Avoids clobbering earlier
     * captures when comparing before/after, which is the usual reason to take one.
     */
    private fun nextManualFile(): FileHandle {
        var i = 1
        while (true) {
            val f = Gdx.files.local("screenshots/crpg-%04d.png".format(i))
            if (!f.exists()) return f
            i++
        }
    }
}
