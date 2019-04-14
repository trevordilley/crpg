package com.ancient.game.crpg

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxInputAdapter

data class MouseInput(val left: MouseButtonAction?,
                      val right: MouseButtonAction?)

sealed class MouseButtonAction
data class MouseDown(
        val screenX: Float,
        val screenY: Float
) : MouseButtonAction()

data class MouseUp(
        val screenX: Float,
        val screenY: Float,
        val isDoubleClick: Boolean = false,
        val wasDragging: Boolean = false
) : MouseButtonAction()


internal const val START_DRAGGING_DISTANCE = 5f


interface UserInputListener {
    fun onInput(mouseInput: MouseInput, left: Boolean, up: Boolean,
                right: Boolean, down: Boolean)
}


class UserInputManager(
        private val listeners: List<UserInputListener>
) : KtxInputAdapter {

    /*******
     *
     *  HEY
     *
     *  DONT FORGET TO UPDATE THE Gdx.input.inputProcessor in BattleScreen.kt, etc!
     *
     */

    private val logger = gameLogger(this::class.java)

    private var timeSinceLastLeftUp = 0f
    private var timeSinceLastRightUp = 0f

    private data class MouseButton(
            val down: Vector2,
            val up: Vector2?,
            val timePassed: Float = 0f,
            val isCtrl: Boolean = false
    ) {
        constructor(downX: Int, downY: Int, upX: Int? = null, upY: Int? = null,
                    timePassed: Float = 0f, isCtrl: Boolean = false)
                : this(Vector2(downX.toFloat(), downY.toFloat()),
                if (upX != null && upY != null) {
                    Vector2(upX.toFloat(), upY.toFloat())
                } else {
                    null
                }, timePassed, isCtrl
        )

        // Unable to maintain the dragging state because mouse moved wasn't being detected
        // on touchpad for OSX
        fun wasDragged(): Boolean =
                up?.let {
                    Vector2.dst(up.x, up.y, down.x, down.y) >= START_DRAGGING_DISTANCE
                } ?: false

        fun deriveAction(): MouseButtonAction {
            return when {
                up != null -> MouseUp(up.x, up.y, false, wasDragged())
                else -> MouseDown(down.x, down.y)
            }
        }

    }

    private var leftButton: MouseButton? = null
    private var rightButton: MouseButton? = null


    fun update(dt: Float) {
        // Update relevant systems
        leftButton = leftButton?.copy(timePassed = leftButton!!.timePassed + dt)
        leftButton?.let { timeSinceLastLeftUp += dt }
        rightButton = rightButton?.copy(timePassed = rightButton!!.timePassed + dt)
        rightButton?.let { timeSinceLastRightUp += dt }

        listeners.forEach { listener ->
            listener.onInput(
                    MouseInput(
                            leftButton?.deriveAction(),
                            rightButton?.deriveAction()
                    ),
                    left,
                    up,
                    right,
                    down
            )
        }

        rightButton?.up?.let { rightButton = null }
        leftButton?.up?.let { leftButton = null }

    }


    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> leftButton = MouseButton(screenX, screenY)
            Input.Buttons.RIGHT -> rightButton = MouseButton(screenX, screenY)
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> {
                leftButton = leftButton?.copy(
                        up = Vector2(screenX.toFloat(), screenY.toFloat())
                )
            }
            Input.Buttons.RIGHT -> {
                rightButton = rightButton?.copy(
                        up = Vector2(screenX.toFloat(), screenY.toFloat())
                )
            }
        }
        return false
    }

    private var up: Boolean = false
    private var down: Boolean = false
    private var left: Boolean = false
    private var right: Boolean = false

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


    override fun keyDown(keycode: Int): Boolean {
        setArrowKeys(keycode, true)
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        setArrowKeys(keycode, false)
        return false
    }

}