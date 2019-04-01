package com.ancient.game.crpg

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get
import ktx.ashley.mapperFor

class Renderable(val sprite: Sprite) : Component
class Transform(var position: Vector2, var rotation: Float) : Component

class RenderSystem(val batch: Batch, val viewport: Viewport) : IteratingSystem(
        all(Renderable::class.java, Transform::class.java).get()) {

    private val renderMapper: ComponentMapper<Renderable> = mapperFor()
    private val transform: ComponentMapper<Transform> = mapperFor()
    private val shapeRenderer = ShapeRenderer()
    private var toRender = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[renderMapper]?.let { toRender.add(entity) }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        draw(toRender)
        toRender.clear()
    }

    private class DrawData(
            val x: FloatArray,
            val y: FloatArray,
            val r: FloatArray,
            val sprites: Array<Sprite>)

    private fun draw(entities: List<Entity>) {
        viewport.camera.update()

        DrawData(
                FloatArray(entities.size),
                FloatArray(entities.size),
                FloatArray(entities.size),
                entities.map {
                    it[renderMapper]!!.sprite
                }.toTypedArray()
        ).apply {
            entities.forEachIndexed { idx, entity ->
                val position = entity[transform]!!.position.let { viewport.project(it) }
                x[idx] = position.x
                y[idx] = position.y
                r[idx] = entity[transform]!!.rotation
            }
        }.let { data ->


            batch.projectionMatrix = viewport.camera.combined
            batch.begin()
            data.sprites.forEachIndexed { index, sprite ->
                val widthOffset = sprite.width / 2
                val heightOffset = sprite.height / 2


                batch.setColor(1f, 1f, 1f, 1f)
                batch.draw(
                        sprite,
                        data.x[index] - widthOffset,
                        data.y[index] - heightOffset,
                        widthOffset,
                        heightOffset,
                        sprite.width,
                        sprite.height,
                        1f,
                        1f,
                        data.r[index]
                )

            }
            batch.end()

            shapeRenderer.projectionMatrix = viewport.camera.combined
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            data.sprites.forEachIndexed { idx, sprite ->
                shapeRenderer.apply {
                    // Vulnerable
                    color = Color.RED
                    arc(
                            data.x[idx],
                            data.y[idx],
                            sprite.width / 2,
                            data.r[idx] + 90f,
                            180f
                    )

                    // Defensive arc
                    listOf(1f
                            , 30f, 45f, 90f, 180f
                    ).forEach { a ->
                        color = Color(0f, 0f, (360f - a) / 360f, 1f)
                        arc(
                                data.x[idx],
                                data.y[idx],
                                sprite.width / 2,
                                data.r[idx] - (a / 2),
                                a
                        )

                    }
                }
            }
            shapeRenderer.end()

        }


    }
}