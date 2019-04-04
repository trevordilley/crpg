package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.log.info

class DeadSystem : IteratingSystem(all(DeadC::class.java).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        engine.removeEntity(entity).also {
            info { "Removing dead critter $entity." }
        }
    }

}