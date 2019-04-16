package com.ancient.game.crpg.battle

import com.ancient.game.crpg.CTransform
import com.ancient.game.crpg.rotate
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.math.component1
import ktx.math.component2
import ktx.math.minus
import ktx.math.times

data class CMovable(val movementSpeed: Float //How fast are you moving?
                    , var destination: Vector2? // Where are you finally going
                    , val rotationSpeed: Float // How fast can we turn
                    , var facingDirection: Float? = null // Where are you looking while you move?
) : Component

class BattleMovementSystem(val collisionPoints: Set<Vector2>) : IteratingSystem(
        all(CMovable::class.java, CTransform::class.java)
                .exclude(CDead::class.java)
                .get()) {
    private val movableMapper: ComponentMapper<CMovable> = mapperFor()
    private val transformMapper: ComponentMapper<CTransform> = mapperFor()
    private val arrivalDistance = 0.2f

    private val positionUpdatesThisFrame: MutableMap<Entity, Vector2> = mutableMapOf()

    override fun processEntity(entity: Entity, deltaTime: Float) {

        // Position
        val destination = entity[movableMapper]!!.destination
        val speed = entity[movableMapper]!!.movementSpeed
        val position = entity[transformMapper]!!.position

        // Rotation
        val rotationSpeed = entity[movableMapper]!!.rotationSpeed
        val direction = destination?.let { (destination - position).nor() }

        val facingDirection = entity[movableMapper]!!.facingDirection

        if (facingDirection != null) {
            // IMPORTANT: All assets that have a direction must
            // be facing RIGHT!!!
            entity[transformMapper]!!.rotation =
                    rotate(entity[transformMapper]!!.rotation, facingDirection, rotationSpeed)

        }


        if (destination != null) {
            val distToDest = Vector2.dst(position.x, position.y, destination.x, destination.y)
            if (distToDest > arrivalDistance) {

                positionUpdatesThisFrame.put(entity, position(position, destination, speed, deltaTime))

                val targetAngle = if (facingDirection == null) {
                    direction?.angle()
                } else null

                if (targetAngle != null) {
                    // IMPORTANT: All assets that have a direction must
                    // be facing RIGHT!!!
                    entity[transformMapper]!!.rotation =
                            rotate(entity[transformMapper]!!.rotation, targetAngle, rotationSpeed)
                }
            }
        }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        positionUpdatesThisFrame
                .filter { (entity, newPosition) ->


                    val (x, y) = newPosition
                            .let { (x, y) ->
                                Pair(
                                        x.toInt(),
                                        y.toInt()
                                )
                            }
                    !(collisionPoints.contains(Vector2(x.toFloat(), y.toFloat())))
                }.forEach { entity, newPosition ->
                    entity[transformMapper]!!.position = newPosition
                }
        positionUpdatesThisFrame.clear()
    }

    private fun direction(currentPosition: Vector2, destination: Vector2) =
            (destination - currentPosition).nor()


    private fun position(currentPosition: Vector2, destination: Vector2, speed: Float, dt: Float): Vector2 {
        val direction = direction(currentPosition, destination)
        val step = direction!! * (speed * dt)
        val newPosition = Vector2(currentPosition.x, currentPosition.y).add(step)
        return newPosition
    }


}
