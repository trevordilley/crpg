package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.assetManagement.MAP_FILEPATH
import com.ancient.game.crpg.assetManagement.SpriteAsset
import com.ancient.game.crpg.equipment.*
import com.ancient.game.crpg.equipment.Nothing
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen


class BattleScreen(private val assetManager: AssetManager, private val batch: Batch,
                   private val viewportManager: ViewportManager) : KtxScreen {

    private val log = gameLogger(this::class.java)

    private lateinit var engine: PooledEngine
    private lateinit var inputManager: UserInputManager
    private lateinit var mapRenderer: BatchTiledMapRenderer
    override fun show() {
        log.info("Showing at camera pos ${viewportManager.viewport.camera.position}")
        log.info("Input Management")

        val battleCommandSystem = BattleCommandSystem(viewportManager.viewport)

        inputManager = UserInputManager(listOf(battleCommandSystem, viewportManager))

        Gdx.input.inputProcessor = object : KtxInputAdapter {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                inputManager.touchDown(screenX, screenY, pointer, button)
                return false
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                inputManager.touchUp(screenX, screenY, pointer, button)
                return false
            }

            override fun keyDown(keycode: Int): Boolean {
                inputManager.keyDown(keycode)
                return false
            }

            override fun keyUp(keycode: Int): Boolean {
                inputManager.keyUp(keycode)
                return false
            }
        }


        log.info("Revving Engines")
        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch, viewportManager.viewport))
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem())
        engine.addSystem(HealthSystem())
        engine.addSystem(DeadSystem())
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())


        // Player Character
        val playerCharacterTexture: Texture = assetManager[SpriteAsset.SWORD_SHIELD.filePath]
        val playerCharacterSprite = Sprite(playerCharacterTexture)

        val createPc = { pos: Vector2 ->
            Entity().apply {
                add(CCombatant(Player,
                        Equipment(
                                MeleeWeapon(
                                        "Short Sword",
                                        10,
                                        0.5f,
                                        NumberHandsToWield.ONE,
                                        5f),
                                Shield("Large Shield", 0.7f),
                                Armor("Plate Mail", 20, 30f))
                ))
                add(CHealth(250,
                        250,
                        1,
                        1))
                add(CRenderableSprite(playerCharacterSprite))
                add(CTransform(pos, 0f, (playerCharacterSprite.width * SiUnits.PIXELS_TO_METER) / 2f))
                add(CSelectable())
                add(CPlayerControlled)
                add(CMovable(20f, null, 8f, null))
            }
        }


        // Orc
        val orcTexture: Texture = assetManager[SpriteAsset.ORC.filePath]
        val orcSprite = Sprite(orcTexture)
        val createOrc = { pos: Vector2 ->
            Entity().apply {
                add(CCombatant(Enemy(30f),
                        Equipment(
                                MeleeWeapon("Large Axe",
                                        120,
                                        2f,
                                        NumberHandsToWield.TWO,
                                        5f
                                ),
                                Nothing,
                                Armor("Shirt", 0, 0f))
                ))
                add(CHealth(250,
                        250,
                        1,
                        1))
                add(CRenderableSprite(orcSprite))
                //    add(CSelectable()) causes strange selection errors...
                add(CTransform(pos, 180f, orcSprite.width / 2f))
                add(CMovable(10f, null, 8f, null))
            }
        }


        engine.addEntity(createPc(Vector2(10f, 10f)))
        engine.addEntity(createPc(Vector2(10f, 20f)))
        engine.addEntity(createPc(Vector2(10f, 30f)))
        engine.addEntity(createPc(Vector2(10f, 40f)))
        engine.addEntity(createOrc(Vector2(60f, 10f)))
        engine.addEntity(createOrc(Vector2(60f, 20f)))
        engine.addEntity(createOrc(Vector2(60f, 30f)))
        engine.addEntity(createOrc(Vector2(60f, 40f)))

        // Map
        val map: TiledMap = assetManager[MAP_FILEPATH]

        val unitScale = SiUnits.PIXELS_TO_METER
        mapRenderer = OrthogonalTiledMapRenderer(map, unitScale, batch)
    }


    override fun render(delta: Float) {
        // Receive user input first
        inputManager.update(delta)

        // Update camera
        viewportManager.update(delta)

        // Render Map
        mapRenderer.setView(viewportManager.viewport.camera as OrthographicCamera)

        mapRenderer.render()

        // Update systems
        engine.update(delta)


    }
}