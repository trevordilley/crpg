package com.ancient.game.crpg.battle

import com.ancient.game.crpg.RenderSystem
import com.ancient.game.crpg.Renderable
import com.ancient.game.crpg.TransformC
import com.ancient.game.crpg.UserInputManager
import com.ancient.game.crpg.assetManagement.Asset
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

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                inputManager.touchUp(screenX, screenY, pointer, button)
                return false
            }
        }


        info { "Revving Engines" }
        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch, viewport))
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem())
        engine.addSystem(HealthSystem())
        engine.addSystem(DeadSystem())
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())

        // Player Character
        val playerCharacterTexture: Texture = assetManager[Asset.SWORD_SHIELD.filePath]
        val playerCharacterSprite = Sprite(playerCharacterTexture)
        val playerCharacterEntity =
                Entity().apply {
                    add(HealthC(250,
                            250,
                            1,
                            1))
                    add(Renderable(playerCharacterSprite))
                    add(TransformC(Vector2(100f, 100f), 0f))
                    add(Selectable)
                    add(PlayerControlled)
                    add(Movable(320f, null, 8f, null))
                }


        // Orc
        val orcTexture: Texture = assetManager[Asset.ORC.filePath]
        val orcSprite = Sprite(orcTexture)
        val orcEntity = Entity().apply {
            add(HealthC(250,
                    250,
                    1,
                    1))
            add(Renderable(orcSprite))
            add(TransformC(Vector2(350f, 350f), 200f))
            add(Movable(320f, null, 8f, null))
        }

        playerCharacterEntity
                .add(
                        ActionC(0f,
                                10,
                                3f,
                                MeleeEffect(playerCharacterEntity, orcEntity, 1000f, 200)))

        engine.addEntity(playerCharacterEntity)
        engine.addEntity(orcEntity)


    }


    override fun render(delta: Float) {
        // Receive user input first
        inputManager.update(delta)

        // Update systems
        engine.update(delta)
    }
}