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
import ktx.math.minus
import ktx.math.times


data class Movable(val movementSpeed: Float //How fast are you moving?
                   , var destination: Vector2? // Where are you finally going
                   , val rotationSpeed: Float // How fast can we turn
                   , var facingDirection: Float? = null // Where are you looking while you move?
) : Component

class BattleMovementSystem : IteratingSystem(
        all(Movable::class.java, Transform::class.java).get()) {
    private val movableMapper: ComponentMapper<Movable> = mapperFor()
    private val transformMapper: ComponentMapper<Transform> = mapperFor()
    private val arrivalDistance = 5f
    override fun processEntity(entity: Entity, deltaTime: Float) {

        val destination = entity[movableMapper]!!.destination
        val speed = entity[movableMapper]!!.movementSpeed
        val rotationSpeed = entity[movableMapper]!!.rotationSpeed
        val facingDirection = entity[movableMapper]!!.facingDirection
        val position = entity[transformMapper]!!.position
        val rotation = entity[transformMapper]!!.rotation
        if (destination != null) {
            val direction = (destination - position).nor()

            // IMPORTANT: All assets that have a direction must
            // be facing RIGHT!!!
            val desiredRotation = direction.angle()
            val currentRot = desiredRotation // + (rotationSpeed * deltaTime)
            entity[transformMapper]!!.rotation = currentRot

            val step = direction * (speed * deltaTime)
            position.add(step) // This mutates the reference to position in the entity
            val distToDest = Vector2.dst(position.x, position.y, destination.x, destination.y)
            if (distToDest <= arrivalDistance) {
                entity[movableMapper]!!.destination = null
            }
        }


    }

}
