package com.ancient.game.crpg

import com.ancient.game.crpg.battle.BattleScreen
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxGame
import ktx.inject.Context
import ktx.log.info

class Application : KtxGame<Screen>() {
    val context = Context()

    private val assetManager = AssetManager()

    override fun create() {
        info { "Loading Assets" }
        assetManager.load(ASSET.SWORD_SHIELD.filePath, Texture::class.java)

        info { "Setting up Context" }
        context.register {
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(assetManager)
            bindSingleton<Viewport>(ScreenViewport())
            bindSingleton(Stage(inject(), inject()))
            bindSingleton(this@Application)
            bindSingleton(BattleScreen(inject(), inject()))
        }
        addScreen(context.inject<BattleScreen>())
        setScreen<BattleScreen>()
    }


    override fun dispose() {
        val assetManager = context.inject<AssetManager>()
        val batch = context.inject<Batch>()
        batch.dispose()
        assetManager.dispose()
    }
}
