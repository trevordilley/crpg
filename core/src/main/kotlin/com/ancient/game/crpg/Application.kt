package com.ancient.game.crpg

import com.ancient.game.crpg.assetManagement.MAP_FILEPATH
import com.ancient.game.crpg.assetManagement.SpriteAsset
import com.ancient.game.crpg.battle.BattleScreen
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxGame
import ktx.inject.Context

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


        //    val camera = OrthographicCamera()
        val camera = OrthographicCamera(Gdx.graphics.width * SiUnits.PIXELS_TO_METER,
                Gdx.graphics.height * SiUnits.PIXELS_TO_METER)

        val viewport = ScreenViewport(camera)
        viewport.unitsPerPixel = SiUnits.PIXELS_TO_METER
        viewport.apply()

        val viewportManager = ViewportManager(viewport)

        log.info("Loading Assets")
        SpriteAsset.values().forEach {
            log.info("Loading ${it.filePath}")
            assetManager.load(it.filePath, Texture::class.java)
        }
        assetManager.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()))
        assetManager.load(MAP_FILEPATH, TiledMap::class.java)
        log.info("Setting up Context")
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
