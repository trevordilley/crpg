package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.math.component1
import ktx.math.component2
import ktx.math.minus
import ktx.math.times
import java.util.Stack

data class CMovable(
        val movementSpeed: Float, //How fast are you moving?
        var destination: Vector2?, // Where are you finally going
        var path: Stack<Vector2>,
        val rotationSpeed: Float, // How fast can we turn
        var facingDirection: Float? = null, // Where are you looking while you move?
        var onArrival: (() -> Unit?)? = null // Do something when we get there?
) : Component {
    companion object {
        fun m() = mapperFor<CMovable>()
    }
}


class BattleMovementSystem(private val collisionPolys: List<Polygon>) : IteratingSystem(
        all(
                CMovable::class.java,
                CTransform::class.java
        )
                .exclude(CDead::class.java)
                .get()) {
    private val movableM: ComponentMapper<CMovable> = mapperFor()
    private val transformM: ComponentMapper<CTransform> = mapperFor()
    private val animatedM: ComponentMapper<CAnimated> = mapperFor()
    private val arrivalDistance = 0.2f
    private val positionUpdatesThisFrame: MutableMap<Entity, Vector2> = mutableMapOf()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)


        // Pathfinding
        val path = entity[movableM]!!.path

        // Position
        val destination = entity[movableM]!!.destination
        val speed = entity[movableM]!!.movementSpeed
        val position = entity[transformM]!!.position

        // Rotation
        val rotationSpeed = entity[movableM]!!.rotationSpeed * dt
        val facingDirection = entity[movableM]!!.facingDirection

        val onArrival = entity[movableM]!!.onArrival

        if (facingDirection != null) {
            // IMPORTANT: All assets that have a direction must
            // be facing RIGHT!!!
            entity[transformM]!!.rotation =
                    rotate(
                            entity[transformM]!!.rotation,
                            facingDirection,
                            rotationSpeed
                    )

        }

        if (destination != null) {
            val distToDest =
                    Vector2.dst(
                            position.x,
                            position.y,
                            destination.x,
                            destination.y
                    )
            if (distToDest > arrivalDistance) {
                val dest = if (path.empty()) destination else {
                    val checkNode = path.peek()
                    val distToNextNode =
                            Vector2.dst(
                                    position.x,
                                    position.y,
                                    checkNode.x,
                                    checkNode.y
                            )
                    if (distToNextNode <= arrivalDistance) {
                        path.pop()
                    }
                    if (!path.empty()) {
                        path.peek()
                    } else {
                        destination
                    }
                }

                positionUpdatesThisFrame[entity] =
                        position(
                                position,
                                dest,
                                speed,
                                dt
                        )

                val direction = (dest - position).nor()
                val targetAngle = if (facingDirection == null) {
                    direction?.angle()
                } else null

                if (targetAngle != null) {
                    // IMPORTANT: All assets that have a direction must
                    // be facing RIGHT!!!
                    entity[transformM]!!.rotation =
                            rotate(
                                    entity[transformM]!!.rotation,
                                    targetAngle,
                                    rotationSpeed
                            )
                }
            } else {
                // set destination to null and clear stack
                onArrival
                        ?.invoke()
                        ?.also { println(" invoking an onArrival") }
                entity[movableM]!!.onArrival = null
                entity[movableM]!!.destination = null
                entity[animatedM]?.anims?.values?.first()?.setAnimation<IdleAnimation>()
            }
        }
    }


    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        val dt = UserInputManager.deltaTime(deltaTime)

        positionUpdatesThisFrame
                .forEach { entity, newPosition ->
                    val (x, y) = newPosition
                            .let { (x, y) ->
                                Pair(
                                        x.toInt(),
                                        y.toInt()
                                )
                            }
                    if (collisionPolys.firstOrNull { it.contains(Vector2(x.toFloat(), y.toFloat())) } != null) {
                        // do some collision correction, move them slightly closer to the center of their
                        // containing cell
                        val centerOfTheirCell =
                                entity[transformM]!!.position
                                        .let {
                                            Vector2(
                                                    it.x.toInt().toFloat() + 0.5f,
                                                    it.y.toInt().toFloat() + 0.5f
                                            )
                                        }
                        entity[transformM]!!.position =
                                position(
                                        entity[transformM]!!.position,
                                        centerOfTheirCell,
                                        3f,
                                        dt
                                )
                    } else {
                        entity[transformM]!!.position = newPosition
                    }
                }
        positionUpdatesThisFrame.clear()
    }


    private fun direction(currentPosition: Vector2, destination: Vector2) =
            (destination - currentPosition).nor()


    private fun position(currentPosition: Vector2, destination: Vector2, speed: Float, dt: Float): Vector2 {
        val direction = direction(currentPosition, destination)
        val step = direction!! * (speed * dt )
        return Vector2(currentPosition.x, currentPosition.y).add(step)
    }
}
