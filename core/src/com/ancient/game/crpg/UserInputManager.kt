package com.ancient.game.crpg

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxInputAdapter


data class MouseInput(val pos: Vector2, val left: MouseButtonAction?, val right: MouseButtonAction?)

sealed class MouseButtonAction
data class MouseDown(
        val screenX: Float,
        val screenY: Float
) : MouseButtonAction()

data class MouseDragged(
        val screenX: Float, val screenY: Float, val originX: Float, val originY: Float
) : MouseButtonAction()

data class MouseUp(
        val screenX: Float,
        val screenY: Float,
        val isDoubleClick: Boolean = false,
        val wasDragging: Boolean = false
) : MouseButtonAction()


sealed class UserInput


// Left Mouse Button Motions
data class LeftUp(
        val screenX: Float,
        val screenY: Float,
        val wasDragging: Boolean = false
) : UserInput()

// Right Mouse Button Motions

data class RightUp(val screenX: Float, val screenY: Float, val wasDragging: Boolean = false) : UserInput()

internal const val START_DRAGGING_DISTANCE = 5f

interface UserInputListener {
    fun onInput(mouseInput: MouseInput)
}

class UserInputManager(
        private val listeners: List<UserInputListener>
) : KtxInputAdapter {

    private var timeSinceLastLeftUp = 0f
    private var timeSinceLastRightUp = 0f


    private var mouseX: Float = 0f
    private var mouseY: Float = 0f

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

        fun isDragging(curX: Float, curY: Float): Boolean =
                (Vector2.dst2(curX, curY, down.x, down.y) >= START_DRAGGING_DISTANCE)

        fun deriveAction(curX: Float, curY: Float): MouseButtonAction {
            val dragging = isDragging(curX, curY)
            return when {
                up != null -> MouseUp(up.x, up.y, false, dragging)
                dragging -> MouseDragged(curX, curY, down.x, down.y)
                else -> MouseDown(down.x, down.y)
            }
        }

    }

    private var leftButton: MouseButton? = null
    private var rightButton: MouseButton? = null


    fun update(dt: Float) {
        // Update relevant systems
        leftButton?.copy(timePassed = leftButton!!.timePassed + dt)
        leftButton?.let { timeSinceLastLeftUp += dt }
        rightButton?.copy(timePassed = rightButton!!.timePassed + dt)
        rightButton?.let { timeSinceLastRightUp += dt }


        listeners.forEach {
            it.onInput(
                    MouseInput(
                            Vector2(mouseX, mouseY),
                            leftButton?.deriveAction(mouseX, mouseY),
                            rightButton?.deriveAction(mouseX, mouseY)
                    )
            )
        }
        rightButton?.let { it.up }?.let { rightButton = null }
        leftButton?.let { it.up }?.let { leftButton = null }
    }


    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> leftButton = MouseButton(screenX, screenY, screenX,
                    screenY)
            Input.Buttons.RIGHT -> rightButton = MouseButton(screenX, screenY, screenX,
                    screenY)
        }
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        mouseX = screenX.toFloat()
        mouseY = screenY.toFloat()
        return false
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        when (button) {
            Input.Buttons.LEFT -> {
                leftButton?.copy(
                        up = Vector2(screenX.toFloat(), screenY.toFloat())
                )
            }
            Input.Buttons.RIGHT -> {
                rightButton?.copy(
                        up = Vector2(screenX.toFloat(), screenY.toFloat())
                )
            }
        }
        return false
    }

}