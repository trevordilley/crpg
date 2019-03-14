package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2


fun Entity.sprite(texture: Texture, position: Vector2, size: Vector2) = apply {
    add(Renderable(texture))
    add(Position(position))
    add(Size(size))
}



