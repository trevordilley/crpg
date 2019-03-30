package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
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
import ktx.math.minus


object Selectable : Component
object PlayerControlled : Component

class BattleCommandSystem(val viewport: Viewport) : UserInputListener, IteratingSystem(
        all(Selectable::class.java, PlayerControlled::class.java, Movable::class.java).get()) {

    private var destination: Vector2? = null
    private var rotation: Float? = null
    private var changed = false
    private val movable: ComponentMapper<Movable> = mapperFor()

    private var rotationPivot: Vector2? = null

    override fun onInput(input: UserInput) {
        when (input) {
            is LeftClick -> {
                destination = viewport.unproject(Vector2(input.screenX, input.screenY))
                changed = true
                info { "Set Destination :$destination" }
            }
            is RightClickDown -> {
                rotationPivot = viewport.unproject(Vector2(input.screenX, input.screenY))
                info { "Right Click Down :$rotationPivot" }
            }
            is RightClickUp -> {
                rotation = rotationPivot?.let { pivot ->
                    val towards = viewport.unproject(Vector2(input.screenX, input.screenY))
                    rotationPivot = null
                    (towards - pivot).nor().angle().also {
                        info { "Right Click Up :$it" }
                    }
                }
            }
        }
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (changed) {
            entity[movable]?.destination = destination
            entity[movable]?.facingDirection = rotation
            changed = false
        }
    }

}
