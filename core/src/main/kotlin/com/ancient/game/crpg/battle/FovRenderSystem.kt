package com.ancient.game.crpg.battle

import com.ancient.game.crpg.createRenderableFilledPolygonMesh
import com.ancient.game.crpg.triangle
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get


class FovRenderSystem(private val viewport: Viewport, private val batch: Batch)
    : IteratingSystem(all(CFoV::class.java).get()) {

    private val shapeRenderer = ShapeRenderer()
    private var fovToRender = mutableListOf<CFoV>()
    private val earTriangulator: EarClippingTriangulator = EarClippingTriangulator()

    private val fbo = FrameBuffer(
        Pixmap.Format.RGBA8888,
        viewport.screenWidth,
        viewport.screenHeight,
        // Maybe needs to be true?
        false
    )

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
        fbo.begin()
        shapeRenderer.projectionMatrix = viewport.camera.combined
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
//        Gdx.gl20.glColorMask(true, true, true, true)
//        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
//        Gdx.gl20.glDepthFunc(GL20.GL_LESS)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
//        Gdx.gl20.glClearDepthf(1f)
//        Gdx.gl20.glClear(GL20.GL_DEPTH_BUFFER_BIT)
//        Gdx.gl20.glDepthFunc(GL20.GL_LESS)
//        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
//        Gdx.gl20.glDepthMask(true)
//        Gdx.gl20.glColorMask(false, false, false, false)

        // HEY, so here's the thing...
        //
        // Right now, I think the things we're trying to do is take each FoV polygon, render it FIRST
        // Then do weird GL magic to make it so we only render stuff on those polygons.
        //
        // Instead, what if we...
        // Take the screen as a square polygon
        // For each FoV polygon, cut it out of the square polygon
        // Render the resulting shape.
        // OR
        // We render a black screen over the area, and then cut out these polygons?

        fovs.forEach { fov ->
            shapeRenderer.apply {
                setColor(1f,0f,0f,0f)
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
        fbo.end()

        batch.begin()
        batch.draw(
            fbo.colorBufferTexture,
            viewport.camera.position.x - viewport.camera.viewportWidth/2f,
            viewport.camera.position.y - viewport.camera.viewportHeight/2f,
            viewport.worldWidth,
            viewport.worldHeight, 0f,0f,1f,1f)
        batch.end()
    }
}
