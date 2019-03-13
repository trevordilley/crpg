package com.ancient.game.crpg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.inject.Context
import ktx.log.info

class CRPG : KtxGame<Screen>() {
    val context = Context()


    override fun create() {
        info { "Setting up Context" }
        context.register {
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(AssetManager())
        }

        context.inject<AssetManager>().load("rpg_sword_shield.png", Texture::class.java)

    }

    override fun render() {
        val assetManager = context.inject<AssetManager>()
        val batch = context.inject<Batch>()
        if (assetManager.update()) {
            Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            batch.begin()
            val txr: Texture = assetManager["rpg_sword_shield.png"]
            batch.draw(txr, 0f, 0f)
            batch.end()

        }
    }

    override fun dispose() {
        val assetManager = context.inject<AssetManager>()
        val batch = context.inject<Batch>()
        batch.dispose()
        assetManager.dispose()
    }
}
