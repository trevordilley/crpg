package com.ancient.game.crpg

import com.ancient.game.crpg.battle.CHealth
import com.ancient.game.crpg.battle.CMovable
import com.ancient.game.crpg.battle.CSelectable
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
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
class CTransform(var position: Vector2, var rotation: Float, val radius: Float) : Component

// TODO add the CRenderableMap to the system!
class RenderSystem(val batch: Batch, val viewport: Viewport,
                   val collisionPoints: Set<Vector2>) : IteratingSystem(
        all(CRenderableSprite::class.java, CTransform::class.java).get()) {

    private val log = gameLogger(this::class.java)

    private val spriteRenderMapper: ComponentMapper<CRenderableSprite> = mapperFor()
    private val transform: ComponentMapper<CTransform> = mapperFor()
    private val movable: ComponentMapper<CMovable> = mapperFor()
    private val healthMapper: ComponentMapper<CHealth> = mapperFor()
    private val selectableMapper: ComponentMapper<CSelectable> = mapperFor()
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
            val sprites: Array<Sprite>,
            val healthData: Array<Int?>,
            val staminaData: Array<Int?>,
            val maxStaminaData: Array<Int?>,
            val selected: BooleanArray
    )

    private fun draw(entities: List<Entity>) {
        viewport.camera.update()
        // Render sprites
        DrawData(
                FloatArray(entities.size),
                FloatArray(entities.size),
                FloatArray(entities.size),
                entities.map { it[spriteRenderMapper]!!.sprite }.toTypedArray(),
                arrayOfNulls(entities.size),
                arrayOfNulls(entities.size),
                arrayOfNulls(entities.size),
                BooleanArray(entities.size)
        ).apply {
            entities.forEachIndexed { idx, entity ->
                val position: Vector2 = entity[transform]!!.position
                x[idx] = position.x
                y[idx] = position.y
                r[idx] = entity[transform]!!.rotation
                healthData[idx] = entity[healthMapper]?.health
                staminaData[idx] = entity[healthMapper]?.stamina
                maxStaminaData[idx] = entity[healthMapper]?.maxStamina
                selected[idx] = entity[selectableMapper]?.selected ?: false
            }
        }.let { data ->


            batch.projectionMatrix = viewport.camera.combined
            batch.begin()
            data.sprites.forEachIndexed { index, sprite ->
                val width = sprite.width * SiUnits.PIXELS_TO_METER
                val height = sprite.height * SiUnits.PIXELS_TO_METER


                val widthOffset = width / 2
                val heightOffset = height / 2
                // https@//gamedev.stackexchange.com/questions/151624/libgdx-orthographic-camera-and-world-units
                batch.setColor(1f, 1f, 1f, 1f)
                batch.draw(
                        sprite,
                        data.x[index] - widthOffset,
                        data.y[index] - heightOffset,
                        widthOffset,
                        heightOffset,
                        width,
                        height,
                        1f, 1f,
                        data.r[index]

                )
            }
            batch.end()


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
            font.color = Color.WHITE


            data.sprites.forEachIndexed { index, sprite ->
                val width = sprite.width
                val height = sprite.height


                val widthOffset = width / 2
                val heightOffset = height / 2
                // https@//gamedev.stackexchange.com/questions/151624/libgdx-orthographic-camera-and-world-units
                val healthLabel = data.healthData[index]?.let { hp -> "Health: $hp" }
                val staminaLabel = data.staminaData[index]?.let { stm ->
                    val max = data.maxStaminaData[index]!!
                    "Stamina: $stm/$max"
                }
                font.draw(
                        batch,
                        "$staminaLabel  $healthLabel",
                        (data.x[index] * SiUnits.UNIT) - widthOffset,
                        (data.y[index] * SiUnits.UNIT) + heightOffset
                )
            }
            batch.end()
            batch.projectionMatrix = originalMatrix

            shapeRenderer.projectionMatrix = viewport.camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            data.sprites.forEachIndexed { idx, sprite ->
                if (data.selected[idx]) {
                    shapeRenderer.apply {
                        // Vulnerable
                        color = Color.RED
                        arc(
                                data.x[idx],
                                data.y[idx],
                                (sprite.width * SiUnits.PIXELS_TO_METER) / 2,
                                data.r[idx] + 90f,
                                180f
                        )

                        // Defensive arc
                        listOf(1f
                                , 180f
                        ).forEach { a ->
                            color = Color(0f, 0f, (360f - a) / 360f, 1f)
                            arc(
                                    data.x[idx],
                                    data.y[idx],
                                    (sprite.width * SiUnits.PIXELS_TO_METER) / 2,
                                    data.r[idx] - (a / 2),
                                    a
                            )
                        }

                        (Pair(data.x[idx], data.y[idx]))
                                .let { (x, y) ->
                                    Pair(
                                            x.toInt().toFloat(),
                                            y.toInt().toFloat()
                                    )
                                }
                                .let { (x, y) ->
                                    listOf(
                                            Vector2(x, y),
                                            Vector2(x + 1, y),
                                            Vector2(x, y + 1),
                                            Vector2(x + 1, y + 1)

                                    )
                                }.forEach { v ->
                                    color = Color.MAGENTA
                                    circle(v.x, v.y, 0.2f)
                                }

                    }
                }
            }
            collisionPoints.forEach { v ->
                shapeRenderer.apply {
                    color = Color.YELLOW
                    rect(v.x, v.y, 1f, 1f)
                }
            }

            entities.filter { it.has(movable) }.forEach {
                it[movable]!!.path.toList().let {
                    var prevPoint = it.firstOrNull()
                    if (prevPoint != null) {
                        it.forEach {
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

            shapeRenderer.end()

        }


    }
}