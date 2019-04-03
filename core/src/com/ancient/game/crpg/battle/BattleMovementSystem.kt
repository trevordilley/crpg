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
        all(Movable::class.java, Transform::class.java)
                .exclude(DeadComponent::class.java)
                .get()) {
    private val movableMapper: ComponentMapper<Movable> = mapperFor()
    private val transformMapper: ComponentMapper<Transform> = mapperFor()
    private val arrivalDistance = 5f
    override fun processEntity(entity: Entity, deltaTime: Float) {

        // Position
        val destination = entity[movableMapper]!!.destination
        val speed = entity[movableMapper]!!.movementSpeed
        val position = entity[transformMapper]!!.position

        // Rotation
        val rotationSpeed = entity[movableMapper]!!.rotationSpeed
        val direction = destination?.let { (destination - position).nor() }
        val targetAngle = entity[movableMapper]!!.facingDirection ?: direction?.angle()

        if (targetAngle != null) {
            // IMPORTANT: All assets that have a direction must
            // be facing RIGHT!!!
            entity[transformMapper]!!.rotation =
                    rotate(entity[transformMapper]!!.rotation, targetAngle, rotationSpeed)

        }


        if (destination != null) {
            val distToDest = Vector2.dst(position.x, position.y, destination.x, destination.y)
            if (distToDest > arrivalDistance) {
                entity[transformMapper]!!.position =
                        position(position, destination, speed, deltaTime)
            }
        }


    }


    private fun direction(currentPosition: Vector2, destination: Vector2) =
            (destination - currentPosition).nor()


    private fun position(currentPosition: Vector2, destination: Vector2, speed: Float, dt: Float): Vector2 {
        val direction = direction(currentPosition, destination)
        val step = direction!! * (speed * dt)
        currentPosition.add(step) // This mutates the reference to position in the entity
        return currentPosition
    }


    private fun rotate(currentRotation: Float, targetRotation: Float, rotationSpeed: Float): Float {
        val rotDelta = Math.abs(targetRotation - currentRotation)


        return if (rotDelta < rotationSpeed) {
            // Rotation difference is so small we should just
            // set it to the target. This may look "snappy" with
            // high rotationSpeed values.
            targetRotation
        } else {
            val clockwise = currentRotation - rotationSpeed
            val clockwiseDistance = Math.abs(targetRotation - clockwise)
            val counterClockwise = currentRotation + rotationSpeed
            val counterClockwiseDistance = Math.abs(targetRotation - counterClockwise)

            if (clockwiseDistance < counterClockwiseDistance) {
                clockwise
            } else {
                counterClockwise
            }
        }
    }
}
