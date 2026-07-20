package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.createRenderableFilledPolygonMesh
import com.ancient.game.crpg.gameLogger
import com.ancient.game.crpg.triangle
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get


class FovRenderSystem(val viewport: Viewport, private val showDebug: Boolean = false)
    : IteratingSystem(all(CFoV::class.java).get()) {

    private val log = gameLogger(this::class.java)
    private val shapeRenderer = ShapeRenderer()
    private var fovToRender = mutableListOf<CFoV>()
    private val earTriangulator: EarClippingTriangulator = EarClippingTriangulator()


    override fun processEntity(entity: Entity, deltaTime: Float) {
        fovToRender.add(entity[CFoV.m()]!!)
    }


    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        draw(fovToRender)
        fovToRender.clear()
    }


    private fun draw(fovs: List<CFoV>) {
        viewport.camera.update()
        shapeRenderer.projectionMatrix = viewport.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        Gdx.gl20.glClearDepthf(1f)
        Gdx.gl20.glClear(GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl20.glDepthFunc(GL20.GL_LESS)
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl20.glDepthMask(true)
        Gdx.gl20.glColorMask(false, false, false, false)
        fovs.forEach { fov ->
            shapeRenderer.apply {
                color = Color.ORANGE
                fov.fovPoly?.let { poly ->
                    val tris =
                            earTriangulator.createRenderableFilledPolygonMesh(poly)
                    tris.forEach { tri ->
                        triangle(tri)
                    }
                }
            }
        }
        shapeRenderer.end()

        // Colour writes back on, depth writes off: the mask is built and must
        // not be modified by anything drawn after this.
        Gdx.gl20.glColorMask(true, true, true, true)
        Gdx.gl20.glDepthMask(false)

        if (showDebug) {
            // Drawn with the mask disarmed, so the whole polygon is visible for
            // inspection rather than being clipped by itself.
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST)
            drawDebugFill(fovs)
        }

        // Arm the mask for everyone downstream. Depth test stays ENABLED with
        // GL_EQUAL, so the map, sprites and anything else drawn this frame only
        // appear where a party member's visibility polygon wrote depth. This is
        // what makes the combined field of view work: the depth buffer holds the
        // union of all viewers' polygons, for free.
        //
        // RenderSystem disables the test again before it draws UI and debug
        // geometry, which must not be occluded.
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl20.glDepthFunc(GL20.GL_EQUAL)
    }

    /**
     * Draws the visibility polygon as a visible outline.
     *
     * The line-of-sight *computation* in FieldOfViewSystem is independent of the
     * masking that is currently inert, so this is how you can see whether the
     * geometry is right while the occlusion effect is switched off. Also the
     * reference to check Phase 3's framebuffer renderer against.
     */
    private fun drawDebugFill(fovs: List<CFoV>) {
        // Filled and translucent, not an outline: a visibility polygon is
        // star-shaped, so joining consecutive vertices draws as a fan of spokes
        // and is unreadable. Filling shows the lit region directly.
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        fovs.forEachIndexed { i, fov ->
            // Distinct colour per viewer, so overlapping polygons from separate
            // party members stay distinguishable.
            shapeRenderer.color = when (i % 3) {
                0 -> Color(1f, 1f, 0f, 0.30f)
                1 -> Color(0f, 1f, 1f, 0.30f)
                else -> Color(1f, 0f, 1f, 0.30f)
            }
            fov.fovPoly?.let { poly ->
                earTriangulator.createRenderableFilledPolygonMesh(poly).forEach { tri ->
                    shapeRenderer.triangle(tri)
                }
            }
        }
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // Log late, not on the first call: this system is registered before
        // FieldOfViewSystem, so on frame 1 no polygon has been computed yet and
        // every count reads zero.
        debugFrame++
        if (debugFrame == 60) {
            log.info(
                "FoV viewers=${fovs.size} " +
                    "vertexCounts=${fovs.map { it.fovPoly?.vertices?.size?.div(2) ?: 0 }}"
            )
        }
    }

    private var debugFrame = 0
}
