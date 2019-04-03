package com.ancient.game.crpg.battle

import com.ancient.game.crpg.Transform
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem

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

class ActionComponent(
        val startTime: Float,
        val timePassed: Float,
        val duringBuildUp: (() -> Unit)?,
        val executionTime: Float,
        val onExecute: () -> Unit,
        val coolDown: Float,
        val duringFlight: (() -> Unit)?
) : Component

class CombatActionSystem : IteratingSystem(all(Transform::class.java, ActionComponent::class.java).get()) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}