package com.ancient.game.crpg

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get
import ktx.ashley.mapperFor

data class Renderable(val sprite: Sprite) : Component
data class Transform(var position: Vector2, var rotation: Float? = null) : Component

class RenderSystem(val batch: Batch, val viewport: Viewport) : IteratingSystem(
        all(Renderable::class.java, Transform::class.java).get()) {

    private val renderMapper: ComponentMapper<Renderable> = mapperFor()
    private val transform: ComponentMapper<Transform> = mapperFor()

    private var toRender = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[renderMapper]?.let { toRender.add(entity) }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        draw(toRender)
        toRender.clear()
    }

    private fun draw(entities: List<Entity>) {
        viewport.camera.update()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        entities.forEach { entity ->
            val renderable = entity[renderMapper]!!
            val position = entity[transform]!!.position.let { viewport.project(it) }
            val sprite = renderable.sprite

            batch.setColor(1f, 1f, 1f, 1f)
            batch.draw(sprite
                    , position.x
                    , position.y
            )

        }
        batch.end()

    }
}