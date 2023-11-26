package com.ancient.game.crpg

import com.ancient.game.crpg.battle.CMovable
import com.ancient.game.crpg.map.Edge
import com.ancient.game.crpg.map.MapManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get
import ktx.ashley.has
import ktx.ashley.mapperFor
import javax.print.attribute.standard.JobKOctets


class CTransform(var position: Vector2, var rotation: Float, val radius: Float, val scale: Float = 1f) : Component {
    companion object {
        fun m() = mapperFor<CTransform>()
    }
}

// TODO add the CRenderableMap to the system!
class RenderSystem(val batch: Batch, val viewport: Viewport,
                   val mapManager: MapManager,
                   val showDebug: Boolean = false) : IteratingSystem(
        all(CAnimated::class.java, CTransform::class.java).get()) {


    private val shapeRenderer = ShapeRenderer(204 * 204 * 4)
    private var spritesToRender = mutableListOf<Pair<Sprite, CTransform>>()
    override fun processEntity(entity: Entity, deltaTime: Float) {

        entity[CAnimated.m()]
                ?.anims
                ?.values
                ?.filter { it.isActive }
                ?.map { it.currentFrame() to entity[CTransform.m()]!! }
                ?.forEach { spritesToRender.add(it) }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        draw(spritesToRender)
        spritesToRender.clear()
    }


    private fun draw(drawData: List<Pair<Sprite, CTransform>>) {
        viewport.camera.update()
//
        shapeRenderer.projectionMatrix = viewport.camera.combined
//
//        // Entity Renders
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        Gdx.gl20.glColorMask(true, true, true, true)
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl20.glDepthFunc(GL20.GL_LESS)
//
        drawData.forEach { (sprite, transform) ->
            val width = sprite.width // * SiUnits.PIXELS_TO_METER
            val height = sprite.height// * SiUnits.PIXELS_TO_METER

            val widthOffset = width / 2
            val heightOffset = height / 2
            val x =
                    transform.position.x - widthOffset

            val y =
                    transform.position.y - heightOffset

            val r =
                    transform.rotation
            val s = transform.scale
            // https@//gamedev.stackexchange.com/questions/151624/libgdx-orthographic-camera-and-world-units
            batch.setColor(1f, 1f, 1f, 1f)
            batch.draw(
                    sprite,
                    x,
                    y,
                    widthOffset,
                    heightOffset,
                    width,
                    height,
                    s, s,
                    r
            )
        }
        batch.end()
//        // UI Rendering
        // TODO: This stuff breaks the FoV render system?
//        val originalMatrix = batch.projectionMatrix.cpy()
//        val uiMatrix = originalMatrix.scale(SiUnits.PIXELS_TO_METER, SiUnits.PIXELS_TO_METER, 1f)
//        batch.projectionMatrix = uiMatrix
//        batch.begin()
//        val font = BitmapFont()
//        if (UserInputManager.isPaused) {
//            font.color = Color.MAGENTA
//            font.draw(
//                    batch,
//                    "PAUSED",
//                    (viewport.worldWidth * SiUnits.UNIT) / 2f,
//                    200f
//            )
//        }
//        batch.projectionMatrix = originalMatrix
//        batch.end()
        debugDraw(showDebug)
    }

    private fun debugDraw(displayDebug: Boolean) {
        if (!displayDebug) return
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        if (showDebug) {

            mapManager.occluders.forEach { poly ->
                shapeRenderer.apply {
                    color = Color.MAGENTA
                    poly.forEach {
                        line(it.p1, it.p2)
                    }
                }
            }

            val nodesOnPath = mutableMapOf<Int,Int>()
            entities
                .filter { it.has(CMovable.m()) }
                .forEach { entity ->
                    entity[CMovable.m()]!!
                        .path
                        .let { p ->
                            p.forEach { n -> nodesOnPath.set(n.x.toInt()/10,n.y.toInt()/10) }
                            p
                        }
                        .toList()
                        .let { path ->
                            var prevPoint = path.firstOrNull()
                            if (prevPoint != null) {
                                path.forEach {
                                    shapeRenderer.apply {
                                        color = Color.BLUE
                                        prevPoint?.let { p ->
                                            line(p.x, p.y, it.x, it.y)
                                            circle(it.x, it.y, 0.2f)
                                        }
                                        prevPoint = it
                                    }
                                }

                            }
                        }

                }

//            shapeRenderer.apply {
//                for(x in 0..<mapManager.pathNodes.size) {
//                    for(y in 0..<mapManager.pathNodes[x].size) {
//                        if(nodesOnPath[x] == y) {
//                            color = Color.BLUE
//                        }
//                        else if(mapManager.pathNodes[x][y] < 0) {
//                            color = Color.DARK_GRAY
//                        } else {
//                            color = Color.FOREST
//                        }
//                        rect((x.toFloat() * 10) - 5f, (y.toFloat() * 10) - 5f, 10f, 10f)
//                    }
//                }
//            }
//
        }
        shapeRenderer.end()
    }
}
