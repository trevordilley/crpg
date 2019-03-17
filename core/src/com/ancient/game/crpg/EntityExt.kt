package com.ancient.game.crpg

import com.ancient.game.crpg.battle.*
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2


fun Entity.playerCharacter(sprite: Sprite,
                           position: Position,
                           speed: Float,
                           movementDirection: Float,
                           destination: Vector2,
                           facingDirection: Float? = null
) = apply {
    add(Renderable(sprite))
    add(Transform(position, facingDirection ?: movementDirection))
    add(Selectable)
    add(PlayerControlled)
    add(Movable(speed, movementDirection, destination, facingDirection))
}