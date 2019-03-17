package com.ancient.game.crpg.battle

import com.ancient.game.crpg.Position
import com.ancient.game.crpg.RenderSystem
import com.ancient.game.crpg.assetManagement.ASSET
import com.ancient.game.crpg.playerCharacter
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.log.info


class BattleScreen(val assetManager: AssetManager, val batch: Batch) : KtxScreen {

    private lateinit var engine: PooledEngine

    override fun show() {
        info { "Showing" }

        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch))
        val txr: Texture = assetManager[ASSET.SWORD_SHIELD.filePath]
        val sprite = Sprite(txr)
        engine.addEntity(
                Entity().playerCharacter(
                        sprite
                        , Position(100f, 100f)
                        , 1.0f
                        , 90f
                        , Vector2(100f, 100f)
                        , 180f)
        )
    }

    override fun render(dt: Float) {
        engine.update(dt)
    }
}