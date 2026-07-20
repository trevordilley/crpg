package com.ancient.game.crpg.systems

import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.Viewport

/**
 * Draws the tilemap as an engine system rather than directly from the screen's
 * render loop.
 *
 * This exists for ordering. Field of view is composited through the depth
 * buffer: FovRenderSystem writes every party member's visibility polygon into
 * depth, which unions them for free, and everything drawn afterwards uses a
 * GL_EQUAL depth test so it only appears where somebody can see.
 *
 * That only works if the map is drawn *after* the mask is built. Rendering it
 * from BattleScreen.render() put it before, so it was tested against the
 * previous frame's mask and culled almost everywhere.
 *
 * Register order matters and is load-bearing:
 *   FovRenderSystem -> MapRenderSystem -> RenderSystem -> UI
 */
class MapRenderSystem(
    private val viewport: Viewport,
    private val mapRenderer: OrthogonalTiledMapRenderer
) : EntitySystem() {

    override fun update(deltaTime: Float) {
        mapRenderer.setView(viewport.camera as OrthographicCamera)
        mapRenderer.render()
    }
}
