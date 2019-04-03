package com.ancient.game.crpg

import com.ancient.game.crpg.assetManagement.Asset
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
    private var loaded = false
    override fun create() {
        info { "Loading Assets" }
        Asset.values().forEach {
            info { "Loading ${it.filePath}" }
            assetManager.load(it.filePath, Texture::class.java)
        }



        info { "Setting up Context" }
        context.register {
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton<Viewport>(ScreenViewport())
            //  context.inject<Batch>().projectionMatrix = context.inject<Viewport>().camera.combined
            bindSingleton(assetManager)
            bindSingleton(Stage(inject(), inject()))
            bindSingleton(this@Application)
            bindSingleton(BattleScreen(inject(), inject(), inject()))
        }
        addScreen(context.inject<BattleScreen>())
    }

    override fun render() {
        super.render()
        if (!loaded) {
            if (assetManager.update()) {
                info { "loaded!" }
                loaded = true
                setScreen<BattleScreen>()
            }
        }
    }

    override fun dispose() {
        val assetManager = context.inject<AssetManager>()
        val batch = context.inject<Batch>()
        batch.dispose()
        assetManager.dispose()
    }
}
