package com.ancient.game.crpg.battle

import com.ancient.game.crpg.LeftClick
import com.ancient.game.crpg.UserInputListener
import com.ancient.game.crpg.UserInputType
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

class BattleCommandSystem(val viewport: Viewport) : UserInputListener,  IteratingSystem(
        all(Selectable::class.java, PlayerControlled::class.java, Movable::class.java).get()) {

    private var destination: Vector2? = null
    private var rotation: Float? = null
    private var changed = false
    private val movable: ComponentMapper<Movable> = mapperFor()

    override fun onInput(input: UserInputType) {
       when(input) {
           is LeftClick -> {
               destination = viewport.unproject(Vector2(input.screenX.toFloat(), input.screenY.toFloat()))
               changed = true
               info { "Set Destination :$destination" }
           }
       }
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (changed) {
            entity[movable]?.destination = destination
            changed = false
        }
    }

}
