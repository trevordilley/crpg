package com.ancient.game.crpg.battle

import com.ancient.game.crpg.UserInputManager
import com.ancient.game.crpg.CTransform
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component that marks an entity as poisoned
 * @param damagePerTick Amount of health damage applied each tick
 * @param tickInterval Time in seconds between damage ticks
 * @param remainingDuration Total remaining duration of poison effect in seconds
 * @param remainingTimeUntilNextTick Time until next damage tick is applied
 */
class CPoison(
    val damagePerTick: Int,
    val tickInterval: Float,
    var remainingDuration: Float,
    var remainingTimeUntilNextTick: Float = 0f
) : Component {
    companion object {
        fun m() = mapperFor<CPoison>()
    }
}

/**
 * System that processes entities with poison component
 * Applies damage over time and removes poison component when duration expires
 */
class PoisonSystem : IteratingSystem(
    all(
        CPoison::class.java,
        CHealth::class.java
    )
    .exclude(
        CDead::class.java
    )
    .get()
) {
    private val poisonM: ComponentMapper<CPoison> = mapperFor()
    private val healthM: ComponentMapper<CHealth> = mapperFor()
    private val transformM: ComponentMapper<CTransform> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        val poison = entity[poisonM]!!
        val health = entity[healthM]!!
        
        // Update remaining duration
        poison.remainingDuration -= dt
        
        // If poison duration is over, remove the poison component
        if (poison.remainingDuration <= 0f) {
            entity.remove(CPoison::class.java)
            return
        }
        
        // Update time until next tick
        poison.remainingTimeUntilNextTick -= dt
        
        // Apply damage when tick interval is reached
        if (poison.remainingTimeUntilNextTick <= 0f) {
            // Reset tick timer
            poison.remainingTimeUntilNextTick = poison.tickInterval
            
            // Apply poison damage
            entity[transformM]?.let { transform ->
                health.damages.add(
                    Damage(
                        stamina = 0, // Poison doesn't affect stamina
                        originPosition = transform.position,
                        health = poison.damagePerTick
                    )
                )
            }
        }
    }
}
