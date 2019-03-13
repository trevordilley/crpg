package com.ancient.game.crpg.battle

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.app.KtxScreen
import ktx.log.info


class BattleScreen(val stage: Stage, val batch: Batch) : KtxScreen {

    override fun show() {
        info { "Showing" }
        super.show()
    }

    override fun render(delta: Float) {
        super.render(delta)
    }
}