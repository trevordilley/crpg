package com.ancient.game.crpg.battle

import com.ancient.game.crpg.ASSET
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import ktx.app.KtxScreen
import ktx.log.info

data class Renderable(val texture: Texture) : Component


class BattleScreen(val assetManager: AssetManager, val batch: Batch) : KtxScreen {

    private lateinit var engine: PooledEngine

    override fun show() {
        info { "Showing" }

        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch))
        val txr: Texture = assetManager[ASSET.SWORD_SHIELD.filePath]
        engine.addEntity(
                Entity().add(Renderable(txr))
        )
    }

    override fun render(dt: Float) {
        engine.update(dt)
    }
}