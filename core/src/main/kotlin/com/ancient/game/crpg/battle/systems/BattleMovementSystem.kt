package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.*
import com.ancient.game.crpg.systems.CAnimated
import com.ancient.game.crpg.systems.CTransform
import com.ancient.game.crpg.systems.IdleAnimation
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.ancient.game.crpg.map.CreatureSize
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
        var onArrival: (() -> Unit?)? = null, // Do something when we get there?
        // Which nav graph this entity paths on. A wider creature needs more
        // clearance to round a corner, so it gets a sparser graph and can
        // legitimately fail to reach somewhere a smaller one can.
        val size: CreatureSize = CreatureSize.MEDIUM
) : Component {
    companion object {
        fun m() = mapperFor<CMovable>()
    }
}


class BattleMovementSystem(
        private val collidesAt: (Vector2) -> Boolean,
        // Re-path when a unit gets stuck. Nullable so tests and any caller that
        // does not care about recovery can omit it.
        private val repath: ((Vector2, Vector2, CreatureSize) -> List<Vector2>)? = null
) : IteratingSystem(
        all(
                CMovable::class.java,
                CTransform::class.java
        )
                .exclude(CDead::class.java)
                .get()) {
    private val arrivalDistance = 0.2f
    private val positionUpdatesThisFrame: MutableMap<Entity, Vector2> = mutableMapOf()

    // Re-path when an entity stops making progress, rather than when it is
    // merely blocked for a frame.
    //
    // A blocked-frame counter does not work: a unit pressed against a wall
    // oscillates — blocked, backs off, moves freely, blocked again — so any
    // reset-on-success resets every other frame and the counter never fires,
    // leaving the unit grinding forever. Watching the distance to the
    // destination instead catches oscillation, grinding and genuine stuckness
    // with one rule.
    private val framesWithoutProgressBeforeRepath = 30
    private val progressEpsilon = 0.01f
    private val bestDistanceToDestination: MutableMap<Entity, Float> = mutableMapOf()
    private val framesWithoutProgress: MutableMap<Entity, Int> = mutableMapOf()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)


        // Pathfinding
        val path = entity[CMovable.m()]!!.path

        // Position
        val destination = entity[CMovable.m()]!!.destination
        val speed = entity[CMovable.m()]!!.movementSpeed
        val position = entity[CTransform.m()]!!.position

        // Rotation
        val rotationSpeed = entity[CMovable.m()]!!.rotationSpeed * dt
        val facingDirection = entity[CMovable.m()]!!.facingDirection

        val onArrival = entity[CMovable.m()]!!.onArrival

        if (facingDirection != null) {
            // IMPORTANT: All assets that have a direction must
            // be facing RIGHT!!!
            entity[CTransform.m()]!!.rotation =
                    rotate(
                            entity[CTransform.m()]!!.rotation,
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
                    entity[CTransform.m()]!!.rotation =
                            rotate(
                                    entity[CTransform.m()]!!.rotation,
                                    targetAngle,
                                    rotationSpeed
                            )
                }
            } else {
                // set destination to null and clear stack
                onArrival
                        ?.invoke()
                        ?.also { println(" invoking an onArrival") }
                entity[CMovable.m()]!!.onArrival = null
                entity[CMovable.m()]!!.destination = null
                entity[CAnimated.m()]?.anims?.values?.first()?.setAnimation<IdleAnimation>()
            }
        }
    }


    override fun update(deltaTime: Float) {
        super.update(deltaTime)

        val dt = UserInputManager.deltaTime(deltaTime)

        positionUpdatesThisFrame
                .forEach { (entity, newPosition) ->
                    // Collision is now polygon containment rather than a lookup in
                    // a set of impassable tile coordinates, so the position is
                    // tested directly instead of being floored to a cell.
                    if (collidesAt(newPosition)) {
                        // Refuse the move and slide back along the vector we came
                        // from. The tile version nudged toward the centre of the
                        // occupied cell, which has no meaning without a grid.
                        val current = entity[CTransform.m()]!!.position
                        val backOff = current.cpy().sub(newPosition).nor().scl(dt * 3f)
                        val corrected = current.cpy().add(backOff)
                        if (!collidesAt(corrected)) {
                            entity[CTransform.m()]!!.position = corrected
                        }
                        // If even the corrected position collides, stay put rather
                        // than pushing the entity deeper into the obstacle.

                    } else {
                        entity[CTransform.m()]!!.position = newPosition
                    }

                    trackProgress(entity)
                }
        positionUpdatesThisFrame.clear()
    }

    /**
     * Watch how close the entity has ever got to its destination. If that stops
     * improving for long enough, it is stuck however busy it looks, so re-path.
     */
    private fun trackProgress(entity: Entity) {
        val destination = entity[CMovable.m()]?.destination
        if (destination == null) {
            bestDistanceToDestination.remove(entity)
            framesWithoutProgress.remove(entity)
            return
        }

        val distance = entity[CTransform.m()]!!.position.dst(destination)
        val best = bestDistanceToDestination[entity]

        if (best == null || distance < best - progressEpsilon) {
            bestDistanceToDestination[entity] = distance
            framesWithoutProgress[entity] = 0
            return
        }

        val stalled = (framesWithoutProgress[entity] ?: 0) + 1
        framesWithoutProgress[entity] = stalled
        if (stalled >= framesWithoutProgressBeforeRepath) {
            framesWithoutProgress[entity] = 0
            // Let the fresh path prove itself from wherever we are now.
            bestDistanceToDestination.remove(entity)
            recomputePath(entity)
        }
    }

    /**
     * Recompute the route to the current destination. Gives up and stops the
     * entity if nowhere is reachable, rather than leaving it pressed against a
     * wall replaying a dead path.
     */
    private fun recomputePath(entity: Entity) {
        val repath = repath ?: return
        val movable = entity[CMovable.m()] ?: return
        val destination = movable.destination ?: return
        val from = entity[CTransform.m()]!!.position

        val fresh = repath(from, destination, movable.size)
        if (fresh.isEmpty()) {
            movable.destination = null
            movable.path = Stack()
            bestDistanceToDestination.remove(entity)
            framesWithoutProgress.remove(entity)
            entity[CAnimated.m()]?.anims?.values?.first()?.setAnimation<IdleAnimation>()
        } else {
            movable.path = Stack<Vector2>().apply { fresh.reversed().forEach { push(it) } }
        }
    }


    private fun direction(currentPosition: Vector2, destination: Vector2) =
            (destination - currentPosition).nor()


    private fun position(currentPosition: Vector2, destination: Vector2, speed: Float, dt: Float): Vector2 {
        val direction = direction(currentPosition, destination)
        val step = direction!! * (speed * dt)
        return Vector2(currentPosition.x, currentPosition.y).add(step)
    }
}
