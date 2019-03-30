package com.ancient.game.crpg

import com.badlogic.gdx.Input
import ktx.app.KtxInputAdapter


sealed class UserInputType()
data class LeftClick(val screenX: Int, val screenY: Int) : UserInputType()
data class RightClick(val screenX: Int, val screenY: Int) : UserInputType()

interface UserInputListener {
    fun onInput(input: UserInputType)
}

class UserInputManager(
        val listeners: List<UserInputListener>
) : KtxInputAdapter {

    var timeSinceLastInput = 0f
    var previousInput: UserInputType? = null
    var currentInput: UserInputType? = null


    fun update(dt: Float) = currentInput?.let { input ->
        // Update relevant systems
        listeners.forEach { it.onInput(input) }
        previousInput = input
        currentInput = null
        timeSinceLastInput = 0f
    } ?: timeSinceLastInput+dt

    private fun touchToMouse(screenX: Int, screenY: Int, button: Int) =
            when (button) {
                Input.Buttons.LEFT -> LeftClick(screenX, screenY)
                Input.Buttons.RIGHT -> RightClick(screenX, screenY)
                else -> null
            }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        currentInput = touchToMouse(screenX, screenY, button)
        return false
    }
}