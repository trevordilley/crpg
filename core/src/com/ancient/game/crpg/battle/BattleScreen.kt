package com.ancient.game.crpg.battle

import com.ancient.game.crpg.ASSET
import com.ancient.game.crpg.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import ktx.app.KtxScreen
import ktx.log.info


class BattleScreen(val stage: Application, val batch: Batch) : KtxScreen {

    override fun show() {
        info { "Showing" }
        super.show()
    }

    override fun render(delta: Float) {
        super.render(delta)
        val assetM = stage.context.inject<AssetManager>()
        val batch = stage.context.inject<Batch>()
        if (assetM.update()) {
            Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            batch.begin()
            val txr: Texture = assetM[ASSET.SWORD_SHIELD.filePath]
            batch.draw(txr, 0f, 0f)
            batch.end()
        }
    }
}