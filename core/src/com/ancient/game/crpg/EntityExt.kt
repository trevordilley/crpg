package com.ancient.game.crpg

import com.ancient.game.crpg.battle.Movable
import com.ancient.game.crpg.battle.PlayerControlled
import com.ancient.game.crpg.battle.Selectable
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2


fun Entity.playerCharacter(sprite: Sprite,
                           position: Vector2,
                           speed: Float,
                           movementDirection: Float,
                           rotationSpeed: Float,
                           destination: Vector2? = null,
                           facingDirection: Float? = null
) = apply {
    add(Renderable(sprite))
    add(Transform(position, facingDirection ?: movementDirection))
    add(Selectable)
    add(PlayerControlled)
    add(Movable(speed, destination, rotationSpeed, facingDirection))
}