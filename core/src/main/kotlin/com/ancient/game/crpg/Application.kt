package com.ancient.game.crpg

import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.ancient.game.crpg.assetManagement.aseprite.AsepriteJson
import com.ancient.game.crpg.assetManagement.aseprite.AsepriteJsonLoader
import com.ancient.game.crpg.assetManagement.aseprite.AsepriteLoader
import com.ancient.game.crpg.battle.BattleScreen
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import games.rednblack.editor.renderer.resources.AsyncResourceManager
import games.rednblack.editor.renderer.resources.ResourceManagerLoader
import ktx.app.KtxGame
import ktx.inject.*
object SiUnits {
    const val UNIT = 64 // minimum resolution of a character.
    const val PIXELS_TO_METER = 1.0f / UNIT.toFloat()
}

class Application : KtxGame<Screen>() {


    private val log = gameLogger(this::class.java)

    val context = Context()

    private val assetManager = AssetManager()
    private var loaded = false
    override fun create() {
        log.debug("""
            --- Application ---
            Resolution:  ${Gdx.graphics.width} x ${Gdx.graphics.height}

        """.trimIndent())


        val camera = OrthographicCamera()

        val viewport = ScreenViewport(camera)
        viewport.apply()

        val viewportManager = ViewportManager(viewport)

        log.info("Loading h2d")
        assetManager.setLoader(AsyncResourceManager::class.java, ResourceManagerLoader(assetManager.fileHandleResolver))
        assetManager.load("project.dt", AsyncResourceManager::class.java)

        log.info("Loading Assets")
        assetManager.setLoader(Aseprite::class.java, AsepriteLoader(InternalFileHandleResolver()))
        assetManager.setLoader(AsepriteJson::class.java, AsepriteJsonLoader(InternalFileHandleResolver()))
        AsepriteAsset.values().forEach {
            log.info("Loading Aesprite Asset: ${it.assetName}")
            assetManager.load(it.assetName, Aseprite::class.java)
        }

        context.register {
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton<Viewport>(viewport)
            bindSingleton(assetManager)
            bindSingleton(viewportManager)
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
