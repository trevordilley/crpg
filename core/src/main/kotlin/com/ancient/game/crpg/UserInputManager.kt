package com.ancient.game.crpg

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxInputAdapter


data class MouseInput(
        val left: MouseButtonAction?,
        val right: MouseButtonAction?
)

sealed class MouseButtonAction
data class MouseDown(val position: Vector2) : MouseButtonAction()
data class MouseButtonDragging(val position: Vector2) : MouseButtonAction()
data class MouseUp(val position: Vector2) : MouseButtonAction()

interface UserInputListener {
    fun onInput(mouseInput: MouseInput, left: Boolean, up: Boolean,
                right: Boolean, down: Boolean)
}


class UserInputManager(
        private val listeners: List<UserInputListener>
) : KtxInputAdapter {

    companion object {
        // TODO: How to make this mutable only from the UserInputManager?
        var isPaused = false

        fun deltaTime(dt: Float): Float {
            return if (isPaused) {
                0f
            } else {
                dt
            }
        }
    }

    private val logger = gameLogger(this::class.java)

    private var leftButtonAction: MouseButtonAction? = null
    private var rightButtonAction: MouseButtonAction? = null

    private var up: Boolean = false
    private var down: Boolean = false
    private var left: Boolean = false
    private var right: Boolean = false


    fun update() {
        // Update listening systems
        listeners.forEach { listener ->
            listener.onInput(
                    MouseInput(
                            leftButtonAction,
                            rightButtonAction
                    ),
                    left,
                    up,
                    right,
                    down
            )
        }

        rightButtonAction = null
        leftButtonAction = null
    }


    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        logger.info("down")

        assignMouseButtonAction(
                button,
                MouseDown(
                        position = Vector2(screenX.toFloat(), screenY.toFloat())
                )
        )
        return false
    }


    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        assignMouseButtonAction(
                pointer,
                MouseButtonDragging(
                        position = Vector2(screenX.toFloat(), screenY.toFloat())
                )
        )
        return false
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        assignMouseButtonAction(
                button,
                MouseUp(
                        position = Vector2(screenX.toFloat(), screenY.toFloat())
                )
        )
        return false
    }


    override fun keyDown(keycode: Int): Boolean {
        setArrowKeys(keycode, true)
        when (keycode) {
            Input.Keys.SPACE -> isPaused = !isPaused
        }
        return false
    }


    override fun keyUp(keycode: Int): Boolean {
        setArrowKeys(keycode, false)
        return false
    }


    private fun assignMouseButtonAction(pointer: Int, action: MouseButtonAction) {
        when (pointer) {
            Input.Buttons.LEFT -> {
                leftButtonAction = action
            }
            Input.Buttons.RIGHT -> {
                rightButtonAction = action
            }
        }
    }


    private fun setArrowKeys(keyCode: Int, isPressed: Boolean) {
        when (keyCode) {
            Input.Keys.LEFT -> left = isPressed
            Input.Keys.A -> left = isPressed
            Input.Keys.UP -> up = isPressed
            Input.Keys.W -> up = isPressed
            Input.Keys.RIGHT -> right = isPressed
            Input.Keys.D -> right = isPressed
            Input.Keys.DOWN -> down = isPressed
            Input.Keys.S -> down = isPressed
        }
    }
}
