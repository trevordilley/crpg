package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.systems.CTransform
import com.ancient.game.crpg.UserInputManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

// TODO: Move into treasure system
class CTreasure(val value: Int): Component {
    companion object {
        fun m() = mapperFor<CTreasure>()
    }
}


class CHaulable(var hauler: Entity? = null) : Component {
    companion object {
        fun m() = mapperFor<CHaulable>()
    }
}


class HaulableSystem() : IteratingSystem(all(
        CHaulable::class.java,
        CTransform::class.java
).get()) {

    private val followDistance = 0.6f
    private val haulableM: ComponentMapper<CHaulable> = mapperFor()
    private val transformM: ComponentMapper<CTransform> = mapperFor()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)

        val haulable = entity[haulableM]!!
        val transform = entity[transformM]!!
        haulable.hauler?.let { hauler ->
            val haulerTransform = hauler[transformM]!!
            if (followDistance < transform.position.dst(haulerTransform.position)) {
                // now increment the current position towards
                val step =
                        haulerTransform
                                .position
                                .cpy()
                                .sub(transform.position)
                                .nor()
                                .scl(2f * dt)
                transform.position.add(step)
            }
        }
    }

    fun drop(haulable: CHaulable) {
        haulable.hauler = null
    }

    fun attemptToPickUp(entity: Entity, haulableE: Entity) {
        val haulable = haulableE[haulableM]!!
        val haulablePosition = haulableE[transformM]!!.position
        entity[transformM]?.let {transform ->
            if(transform.position.dst(haulablePosition) <= followDistance) {
                haulable.hauler = entity
            }
        } ?: {println("Entity $entity has no transform?")}.invoke()

    }
}
