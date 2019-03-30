package com.ancient.game.crpg

import com.badlogic.gdx.Input
import ktx.app.KtxInputAdapter
import ktx.log.info


sealed class UserInput

// Left Mouse Button Motions
data class LeftClick(val screenX: Float, val screenY: Float) : UserInput()

// Right Mouse Button Motions
data class RightClickDown(val screenX: Float, val screenY: Float) : UserInput()

data class RightClickUp(val screenX: Float, val screenY: Float) : UserInput()

interface UserInputListener {
    fun onInput(input: UserInput)
}

class UserInputManager(
        val listeners: List<UserInputListener>
) : KtxInputAdapter {

    var timeSinceLastInput = 0f
    var previousInput: UserInput? = null
    var currentInput: UserInput? = null


    fun update(dt: Float) = currentInput?.let { input ->
        // Update relevant systems
        listeners.forEach { it.onInput(input) }
        previousInput = input
        currentInput = null
        timeSinceLastInput = 0f
    } ?: timeSinceLastInput+dt


    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        currentInput = when (button) {
            Input.Buttons.LEFT -> LeftClick(screenX.toFloat(), screenY.toFloat())
            Input.Buttons.RIGHT -> RightClickDown(screenX.toFloat(), screenY.toFloat())
            else -> null
        }
        return false
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        info { "Before UP?" }
        currentInput = when (button) {
            Input.Buttons.RIGHT -> RightClickUp(screenX.toFloat(), screenY.toFloat()).also {
                info { "Up?" }
            }
            else -> null
        }
        return false
    }

}