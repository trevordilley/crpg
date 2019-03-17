package com.ancient.game.crpg

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor

data class Renderable(val sprite: Sprite) : Component
data class Transform(var position: Vector2, var rotation: Float? = null) : Component

class RenderSystem(val batch: Batch) : IteratingSystem(
        all(Renderable::class.java, Transform::class.java).get()) {

    private val renderMapper: ComponentMapper<Renderable> = mapperFor()
    private val transform: ComponentMapper<Transform> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {

        batch.begin()
        val renderable = entity[renderMapper]!!
        val transform = entity[transform]!!
        val sprite = renderable.sprite
        // Rotate the sprite if the rotation is present on the transform
        transform.rotation?.let {
            sprite.rotation = it
        }

        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(sprite
                , transform.position.x
                , transform.position.y
        )
        batch.end()
    }

}