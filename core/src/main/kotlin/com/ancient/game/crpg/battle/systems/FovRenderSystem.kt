package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.createRenderableFilledPolygonMesh
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


class FovRenderSystem(val viewport: Viewport)
    : IteratingSystem(all(CFoV::class.java).get()) {

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
    }
}
