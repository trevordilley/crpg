package com.ancient.game.crpg

import com.badlogic.gdx.utils.viewport.Viewport


class ViewportManager(val viewport: Viewport) : UserInputListener {

    private var up: Boolean = false
    private var down: Boolean = false
    private var left: Boolean = false
    private var right: Boolean = false

    override fun onInput(
            mouseInput: MouseInput,
            left: Boolean,
            up: Boolean,
            right: Boolean,
            down: Boolean
    ) {
        this.left = left
        this.right = right
        this.up = up
        this.down = down
    }


    fun update(dt: Float) {
        viewport.camera.update()

        val cameraMoveSpeed = 400 * dt
        if (left) viewport.camera.position.x -= cameraMoveSpeed
        if (right) viewport.camera.position.x += cameraMoveSpeed
        if (up) viewport.camera.position.y += cameraMoveSpeed
        if (down) viewport.camera.position.y -= cameraMoveSpeed
    }
}
