package com.ancient.game.crpg

import com.badlogic.gdx.utils.viewport.Viewport


class ViewportManager(val viewport: Viewport) : UserInputListener {

    private var up: Boolean = false
    private var down: Boolean = false
    private var left: Boolean = false
    private var right: Boolean = false

    override fun onInput(mouseInput: MouseInput, left: Boolean, up: Boolean, right: Boolean, down: Boolean) {
        this.left = left
        this.right = right
        this.up = up
        this.down = down
    }


    fun update(dt: Float) {
        viewport.camera.update()

        val cameraMoveSpeed = 20 * dt

        //log.debug("$left, $right, $up, $down")
        when {
            left -> viewport.camera.position.x -= cameraMoveSpeed
            right -> viewport.camera.position.x += cameraMoveSpeed
            up -> viewport.camera.position.y += cameraMoveSpeed
            down -> viewport.camera.position.y -= cameraMoveSpeed
        }

    }
}