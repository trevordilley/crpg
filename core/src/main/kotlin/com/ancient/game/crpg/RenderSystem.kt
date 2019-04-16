package com.ancient.game.crpg

import com.ancient.game.crpg.battle.CHealth
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
import ktx.ashley.mapperFor


class CRenderableSprite(val sprite: Sprite) : Component
class CTransform(var position: Vector2, var rotation: Float, val radius: Float) : Component

// TODO add the CRenderableMap to the system!
class RenderSystem(val batch: Batch, val viewport: Viewport) : IteratingSystem(
        all(CRenderableSprite::class.java, CTransform::class.java).get()) {

    private val log = gameLogger(this::class.java)

    private val spriteRenderMapper: ComponentMapper<CRenderableSprite> = mapperFor()
    private val transform: ComponentMapper<CTransform> = mapperFor()
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

            val font = BitmapFont()

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
                val healthLabel = data.healthData[index]?.let { hp -> "Health: $hp" }
                val staminaLabel = data.staminaData[index]?.let { stm ->
                    val max = data.maxStaminaData[index]!!
                    "Stamina: $stm/$max"
                }
                font.draw(
                        batch,
//                        "$staminaLabel  $healthLabel",
                        "",
                        data.x[index] - widthOffset,
                        data.y[index] + heightOffset + 10
                )
            }
            batch.end()

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
                    }
                }
            }
            shapeRenderer.end()

        }


    }
}