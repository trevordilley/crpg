package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector2


data class Movable(val speed: Float //How fast are you moving?
                   , val movementDirection: Float // What direction are you moving towards
                   , val destination: Vector2 // Where are you finally going
                   , val facingDirection: Float? = null // Where are you looking while you move?
        // If null then same as movement direction.
) : Component
