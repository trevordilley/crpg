package com.ancient.game.crpg.battle

import com.ancient.game.crpg.RenderSystem
import com.ancient.game.crpg.UserInputManager
import com.ancient.game.crpg.assetManagement.ASSET
import com.ancient.game.crpg.playerCharacter
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.log.info


class BattleScreen(val assetManager: AssetManager, val batch: Batch, val viewport: Viewport) : KtxScreen {

    private lateinit var engine: PooledEngine
    private lateinit var inputManager: UserInputManager
    override fun show() {
        info { "Showing at camera pos ${viewport.camera.position}" }
        info { "Input Management" }

        val battleCommandSystem = BattleCommandSystem(viewport)

        inputManager = UserInputManager(listOf(battleCommandSystem))

        Gdx.input.inputProcessor = object : KtxInputAdapter {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                inputManager.touchDown(screenX, screenY, pointer, button)
                return false
            }
        }


        info { "Revving Engines" }
        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch, viewport))
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem())
        val txr: Texture = assetManager[ASSET.SWORD_SHIELD.filePath]

        val sprite = Sprite(txr)
        engine.addEntity(
                Entity().playerCharacter(
                        sprite
                        , Vector2(0f, 0f)
                        , 320.0f
                        , 90f
                        , 0.5f
                )
        )
    }

    override fun render(delta: Float) {
        // Receive user input first
        inputManager.update(delta)

        // Update systems
        engine.update(delta)
    }
}