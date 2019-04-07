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

class Application : KtxGame<Screen>() {

    private val log = gameLogger(this::class.java)

    val context = Context()

    private val assetManager = AssetManager()
    private var loaded = false
    override fun create() {
        log.info("Loading Assets")
        Asset.values().forEach {
            log.info("Loading ${it.filePath}")
            assetManager.load(it.filePath, Texture::class.java)
        }



        log.info("Setting up Context")
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
                log.info("loaded!")
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
