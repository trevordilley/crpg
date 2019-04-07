package com.ancient.game.crpg.battle

import com.ancient.game.crpg.gameLogger
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.log.info

class DeadSystem : IteratingSystem(all(CDead::class.java).get()) {
    private val log = gameLogger(this::class.java)
    override fun processEntity(entity: Entity, deltaTime: Float) {

        engine.removeEntity(entity).also {
            info { "Removing dead critter $entity." }
        }
    }

}