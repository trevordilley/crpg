package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.assetManagement.Asset
import com.ancient.game.crpg.equipment.*
import com.ancient.game.crpg.equipment.Nothing
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


class BattleScreen(val assetManager: AssetManager, val batch: Batch, val viewport: Viewport) : KtxScreen {

    private val log = gameLogger(this::class.java)

    private lateinit var engine: PooledEngine
    private lateinit var inputManager: UserInputManager
    override fun show() {
        log.info("Showing at camera pos ${viewport.camera.position}")
        log.info("Input Management")

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


        log.info("Revving Engines")
        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch, viewport))
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem())
        engine.addSystem(HealthSystem())
        engine.addSystem(DeadSystem())
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())
        // Player Character
        val playerCharacterTexture: Texture = assetManager[Asset.SWORD_SHIELD.filePath]
        val playerCharacterSprite = Sprite(playerCharacterTexture)
        val playerCharacterEntity =
                Entity().apply {
                    add(CCombatant(Player,
                            Equipment(
                                    MeleeWeapon(
                                            "Short Sword",
                                            10,
                                            0.5f,
                                            NumberHandsToWield.ONE,
                                            140f),
                                    Shield("Large Shield", 50f),
                                    Armor("Plate Mail", 20, 30f))
                    ))
                    add(CHealth(250,
                            250,
                            1,
                            1))
                    add(CRenderable(playerCharacterSprite))
                    add(CTransform(Vector2(100f, 100f), 0f, playerCharacterSprite.width / 2f))
                    add(CSelectable())
                    add(CPlayerControlled)
                    add(CMovable(320f, null, 8f, null))
                }


        // Orc
        val orcTexture: Texture = assetManager[Asset.ORC.filePath]
        val orcSprite = Sprite(orcTexture)
        val orcEntity = Entity().apply {
            add(CCombatant(Enemy(300f),
                    Equipment(
                            MeleeWeapon("Large Axe",
                                    120,
                                    2f,
                                    NumberHandsToWield.TWO,
                                    140f
                            ),
                            Nothing,
                            Armor("Shirt", 0, 0f))
            ))
            add(CHealth(250,
                    250,
                    1,
                    1))
            add(CRenderable(orcSprite))
            add(CSelectable())
            add(CTransform(Vector2(350f, 350f), 200f, orcSprite.width / 2f))
            add(CMovable(320f, null, 8f, null))
        }


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