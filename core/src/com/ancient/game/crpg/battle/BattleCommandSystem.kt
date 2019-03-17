package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.log.info


object Selectable : Component
object PlayerControlled : Component

class BattleCommandSystem : IteratingSystem(
        all(Selectable::class.java, PlayerControlled::class.java, Movable::class.java).get()) {

    fun onTouchUp(
            screenX: Int, screenY: Int, pointer: Int, button: Int
    ) {
        info {"UP: $screenX - $screenY - $pointer - $button"}
    }

    fun onTouchDown(
            screenX: Int, screenY: Int, pointer: Int, button: Int
    ) {
        info {"DOWN: $screenX - $screenY - $pointer - $button"}
    }

    override fun processEntity(entity: Entity?, deltaTime: Float) {
    }

}
