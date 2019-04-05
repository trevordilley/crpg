package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.ashley.remove

/**
 * Consider these steps for an action:
 *
 * Build-up (will not apply effect yet):
 * Examples - Winding up for a weapon strike, incantations for spell cast, etc)
 * Probably has some kind of animation associated with it
 *
 * In-Flight (may apply effect on collision, otherwise upon reaching destination):
 * Examples - Sword swing coming down, arrow in flight
 * May have sprite while in flight
 *
 * On Execution (Moment we apply the effect):
 * Examples - Actually hitting opponent with sword, fireball exploding on ground
 * Could generate other effects (Fireball explosion for example may make a ring of fire
 * that does the actual damage) or immediately apply the effect to whatever it may have hit
 *
 * == LifeCycle Functions ==
 * What data do/should they have? Is the component system the right fit for them?
 */


// Actions create effects
class ActionC(
        var timePassed: Float,
        val staminaCost: Int,
        val duration: Float,
        val effect: ActionEffectC
) : Component

class BattleActionSystem : IteratingSystem(all(ActionC::class.java).get()) {
    private val actionMapper: ComponentMapper<ActionC> = mapperFor()
    private val healthMapper: ComponentMapper<HealthC> = mapperFor()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[actionMapper]!!.let { action ->
            action.apply {
                timePassed += deltaTime
                if (timePassed >= duration) {
                    applyEffect(action)
                    entity[healthMapper]?.let { health ->
                        health.damages.add(staminaCost)
                    }
                    entity.remove<ActionC>()
                }
            }
        }
    }

    // Consider pooling these entities, they'll be short lived
    // and happen quite a bit
    private fun applyEffect(action: ActionC) {
        println("BattleActionSystem.applyEffect adding effect")
        println("engine.entities.size() = ${engine.entities.size()}")
        engine.addEntity(Entity().add(action.effect))
        println("engine.entities.size() = ${engine.entities.size()}")
    }

}