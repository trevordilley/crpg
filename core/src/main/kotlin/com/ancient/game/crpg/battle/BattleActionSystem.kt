package com.ancient.game.crpg.battle

import com.ancient.game.crpg.UserInputManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.ashley.remove

class CAction(
        val onProgression: CAction.(deltaTime: Float, duration: Float) -> Boolean
) : Component {
    private var completed: Boolean = false
    private var duration: Float = 0f
    private var deltaTime: Float = 0f

    fun update(dt: Float): Boolean {
        deltaTime = dt
        duration += dt
        completed = this.onProgression(this, deltaTime, duration)
        return completed
    }
}

class BattleActionSystem : IteratingSystem(all(CAction::class.java).get()) {
    private val actionM: ComponentMapper<CAction> = mapperFor()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)

        entity[actionM]!!.update(dt).let { isCompleted ->
            if (isCompleted) {
                entity.remove<CAction>()
            }
        }
    }


}
