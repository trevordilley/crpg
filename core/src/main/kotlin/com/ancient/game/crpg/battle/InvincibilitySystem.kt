package com.ancient.game.crpg.battle

import com.ancient.game.crpg.UserInputManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

/**
 * Component that marks an entity as invincible for a duration
 * @param duration How long the invincibility lasts in seconds
 * @param timeRemaining Current time remaining for invincibility
 * @param blinkTimer Timer for visual blinking effect
 */
class CInvincible(
    var duration: Float,
    var timeRemaining: Float = duration,
    var blinkTimer: Float = 0f,
    var isVisible: Boolean = true
) : Component {
    companion object {
        fun m() = mapperFor<CInvincible>()
    }
}

/**
 * System that manages invincibility timers and effects
 */
class InvincibilitySystem : IteratingSystem(
    all(CInvincible::class.java).get()
) {
    
    private val invincibleM: ComponentMapper<CInvincible> = mapperFor()
    
    companion object {
        const val BLINK_INTERVAL = 0.1f // Blink every 0.1 seconds
    }
    
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        
        entity[invincibleM]?.let { invincible ->
            // Update the invincibility timer
            invincible.timeRemaining -= dt
            
            // Update blink timer for visual effect
            invincible.blinkTimer += dt
            if (invincible.blinkTimer >= BLINK_INTERVAL) {
                invincible.isVisible = !invincible.isVisible
                invincible.blinkTimer = 0f
            }
            
            // Remove invincibility when time is up
            if (invincible.timeRemaining <= 0f) {
                // Make sure entity is visible when invincibility ends
                invincible.isVisible = true
                entity.remove(CInvincible::class.java)
            }
        }
    }
}