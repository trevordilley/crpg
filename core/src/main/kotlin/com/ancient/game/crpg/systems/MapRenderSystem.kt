package com.ancient.game.crpg.systems

import com.ancient.game.crpg.SiUnits
import com.ancient.game.crpg.map.Level
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.viewport.Viewport

/**
 * Draws the level background as an engine system rather than from the screen's
 * render loop.
 *
 * This exists for ordering. Field of view is composited through the depth
 * buffer: FovRenderSystem writes every party member's visibility polygon into
 * depth, which unions them for free, and everything drawn afterwards uses a
 * GL_EQUAL depth test so it only appears where somebody can see.
 *
 * That only works if the background is drawn *after* the mask is built.
 * Rendering it from BattleScreen.render() put it before, so it was tested
 * against the previous frame's mask and culled almost everywhere.
 *
 * Register order matters and is load-bearing:
 *   FovRenderSystem -> MapRenderSystem -> RenderSystem -> UI
 */
class MapRenderSystem(
    private val viewport: Viewport,
    private val batch: Batch,
    private val background: Texture,
    private val level: Level
) : EntitySystem() {

    // The art is authored in pixels, so it converts with the same factor the
    // level loader applied to the geometry. If these ever disagree, the polygons
    // will not line up with the picture.
    private val width = background.width * SiUnits.PIXELS_TO_METER
    private val height = background.height * SiUnits.PIXELS_TO_METER

    override fun update(deltaTime: Float) {
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(
            background,
            level.backgroundPosition.x,
            level.backgroundPosition.y,
            width,
            height
        )
        batch.end()
    }
}
