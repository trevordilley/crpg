package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.log.info


object Selectable : Component
object PlayerControlled : Component

class BattleCommandSystem(val viewport: Viewport) : IteratingSystem(
        all(Selectable::class.java, PlayerControlled::class.java, Movable::class.java).get()) {

    private var destination: Vector2? = null

    private val movable: ComponentMapper<Movable> = mapperFor()
    fun onTouchDown(
            screenX: Int, screenY: Int, pointer: Int, button: Int
    ) {
        destination = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat()))
        info { "Set Destination :$destination" }
    }

    fun onTouchUp(
            screenX: Int, screenY: Int, pointer: Int, button: Int
    ) {
        info { "UP: $screenX - $screenY - $pointer - $button" }
    }


    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[movable]?.destination = destination
    }

}
