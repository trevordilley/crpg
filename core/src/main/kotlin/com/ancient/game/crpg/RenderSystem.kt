package com.ancient.game.crpg

import com.ancient.game.crpg.battle.CHealth
import com.ancient.game.crpg.battle.CMovable
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


class CRenderableSprite(val sprite: Sprite) : Component
class CTransform(var position: Vector2, var rotation: Float, val radius: Float, val scale: Float = 1f) : Component

// TODO add the CRenderableMap to the system!
class RenderSystem(val batch: Batch, val viewport: Viewport,
                   val collisionPoints: Set<Vector2>, val mapManager: MapManager,
                   val showDebug: Boolean = false) : IteratingSystem(
        all(CRenderableSprite::class.java, CTransform::class.java).get()) {

    private val log = gameLogger(this::class.java)

    private val spriteRenderMapper: ComponentMapper<CRenderableSprite> = mapperFor()
    private val transform: ComponentMapper<CTransform> = mapperFor()
    private val movable: ComponentMapper<CMovable> = mapperFor()
    private val healthMapper: ComponentMapper<CHealth> = mapperFor()
    private val shapeRenderer = ShapeRenderer()
    private var spritesToRender = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[spriteRenderMapper]?.let { spritesToRender.add(entity) }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        draw(spritesToRender)
        spritesToRender.clear()
    }

    private class DrawData(
            val x: FloatArray,
            val y: FloatArray,
            val r: FloatArray,
            val s: FloatArray,
            val sprites: Array<Sprite>,
            val healthData: Array<Int?>,
            val staminaData: Array<Int?>,
            val maxStaminaData: Array<Int?>
    )


    private fun draw(entities: List<Entity>) {
        viewport.camera.update()

        shapeRenderer.projectionMatrix = viewport.camera.combined


        // Render sprites
        DrawData(
                FloatArray(entities.size),
                FloatArray(entities.size),
                FloatArray(entities.size),
                FloatArray(entities.size),
                entities.map { it[spriteRenderMapper]!!.sprite }.toTypedArray(),
                arrayOfNulls(entities.size),
                arrayOfNulls(entities.size),
                arrayOfNulls(entities.size)
        ).apply {
            entities.forEachIndexed { idx, entity ->
                val position: Vector2 = entity[transform]!!.position
                x[idx] = position.x
                y[idx] = position.y
                r[idx] = entity[transform]!!.rotation
                s[idx] = entity[transform]!!.scale
                healthData[idx] = entity[healthMapper]?.health
                staminaData[idx] = entity[healthMapper]?.stamina
                maxStaminaData[idx] = entity[healthMapper]?.maxStamina

            }
        }.let { data ->

            // Entity Renders
            batch.projectionMatrix = viewport.camera.combined
            batch.begin()
            Gdx.gl20.glColorMask(true, true, true, true)
            Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
            Gdx.gl20.glDepthFunc(GL20.GL_EQUAL)

            data.sprites.forEachIndexed { index, sprite ->
                val width = sprite.width * SiUnits.PIXELS_TO_METER
                val height = sprite.height * SiUnits.PIXELS_TO_METER

                val widthOffset = width / 2
                val heightOffset = height / 2
                val x =
                        data.x[index] - widthOffset

                val y =
                        data.y[index] - heightOffset

                val r =
                        data.r[index]

                val s = data.s[index]
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

            // UI Rendering
            val originalMatrix = batch.projectionMatrix.cpy()
            val uiMatrix = originalMatrix.scale(SiUnits.PIXELS_TO_METER, SiUnits.PIXELS_TO_METER, 1f)
            batch.projectionMatrix = uiMatrix
            batch.begin()
            val font = BitmapFont()
            if (UserInputManager.isPaused) {
                font.color = Color.MAGENTA
                font.draw(
                        batch,
                        "PAUSED",
                        (viewport.worldWidth * SiUnits.UNIT) / 2f,
                        200f
                )
            }
            batch.projectionMatrix = originalMatrix
            batch.end()
            debugDraw(showDebug, data)
        }
    }

    private fun debugDraw(displayDebug: Boolean, data: DrawData) {
        if (!displayDebug) return
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        if (showDebug) {
            collisionPoints.forEach { v ->
                shapeRenderer.apply {
                    color = Color.YELLOW
                    rect(v.x, v.y, 1f, 1f)
                }
            }

            mapManager.opaqueEdges.forEach { e ->
                shapeRenderer.apply {
                    color = Color.MAGENTA
                    line(e.p1, e.p2)
                }
            }

            entities
                    .filter { it.has(movable) }
                    .forEach { entity ->
                        entity[movable]!!
                                .path
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
        }
        shapeRenderer.end()
    }
}
