package com.ancient.game.crpg.battle.hauling

import com.ancient.game.crpg.CTransform
import com.ancient.game.crpg.UserInputManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor


class CHaulable(val hauler: Entity?) : Component


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
}