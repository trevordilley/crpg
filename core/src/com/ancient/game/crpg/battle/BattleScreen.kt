package com.ancient.game.crpg.battle

import com.ancient.game.crpg.RenderSystem
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

    override fun show() {
        info { "Showing at camera pos ${viewport.camera.position}" }




        info { "Input Management" }

        val battleCommandSystem = BattleCommandSystem(viewport)

        Gdx.input.inputProcessor = object : KtxInputAdapter {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                battleCommandSystem.onTouchDown(screenX, screenY, pointer, button)
                return false
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                battleCommandSystem.onTouchUp(screenX, screenY, pointer, button)
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
                        , 80.0f
                        , 90f
                )
        )
    }

    override fun render(dt: Float) {
        engine.update(dt)
    }
}