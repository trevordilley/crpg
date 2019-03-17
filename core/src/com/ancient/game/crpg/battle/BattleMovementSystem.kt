package com.ancient.game.crpg.battle

import com.ancient.game.crpg.Transform
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.math.plus
import ktx.math.times


data class Movable(val speed: Float //How fast are you moving?
                   , var destination: Vector2? // Where are you finally going
                   , var facingDirection: Float? = null // Where are you looking while you move?
) : Component

class BattleMovementSystem : IteratingSystem(
        all(Movable::class.java, Transform::class.java).get()) {
    private val movableMapper: ComponentMapper<Movable> = mapperFor()
    private val transformMapper: ComponentMapper<Transform> = mapperFor()
    private val arrivalDist = 10f
    override fun processEntity(entity: Entity, deltaTime: Float) {

        val movable = entity[movableMapper]!!
        val transform = entity[transformMapper]!!

        entity[transformMapper]?.position = movable.destination?.nor()?.let { dir ->
            dir * (movable.speed * deltaTime)
        }?.let { dir ->
            transform.position + dir
        } ?: transform.position
    }

}
