package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.MAP_FILEPATH
import com.ancient.game.crpg.assetManagement.SpriteAsset
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.ancient.game.crpg.equipment.*
import com.ancient.game.crpg.equipment.Nothing
import com.ancient.game.crpg.map.MapManager
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import java.util.Stack


class BattleScreen(private val assetManager: AssetManager, private val batch: Batch,
                   private val viewportManager: ViewportManager) : KtxScreen {

    private val log = gameLogger(this::class.java)

    private lateinit var engine: PooledEngine
    private lateinit var inputManager: UserInputManager
    private lateinit var mapRenderer: OrthogonalTiledMapRenderer

    override fun show() {
        log.info("Showing at camera pos ${viewportManager.viewport.camera.position}")
        log.info("Input Management")

        log.info("Building Map")
        val map: TiledMap = assetManager[MAP_FILEPATH]
        val mapManager = MapManager(map)
        val collisionPoints = mapManager.impassableCellPositions()
        mapRenderer = OrthogonalTiledMapRenderer(map, SiUnits.PIXELS_TO_METER, batch)

        val battleCommandSystem = BattleCommandSystem(viewportManager.viewport, mapManager)

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

        // Map


        log.info("Revving Engines")
        engine = PooledEngine()
        engine.addSystem(RenderSystem(batch, viewportManager.viewport, collisionPoints, mapManager))
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem(collisionPoints))
        engine.addSystem(HealthSystem())
        engine.addSystem(DeadSystem())
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())
        engine.addSystem(FieldOfViewSystem(mapManager))
        engine.addSystem(AnimationSystem())

        // Player Character
        val playerCharacterTexture: Texture = assetManager[SpriteAsset.SWORD_SHIELD.filePath]
        val playerCharacterSprite = Sprite(playerCharacterTexture)
        val playerCharacterAnim: Aseprite = assetManager[AsepriteAsset.SWORD_SHIELD.assetName]
        val createPc = { pos: Vector2 ->
            Entity().apply {
                add(CCombatant(Player,
                        Equipment(
                                MeleeWeapon(
                                        "Short Sword",
                                        10,
                                        1f,
                                        NumberHandsToWield.ONE,
                                        1f),
                                Shield("Large Shield", 0.7f),
                                Armor("Plate Mail", 20, 30f))
                ))
                add(CHealth(250,
                        250,
                        1,
                        1))
                add(CRenderableSprite(Sprite(playerCharacterAnim.frame(0))))
                add(CTransform(pos, 90f, (playerCharacterSprite.width * SiUnits.PIXELS_TO_METER) / 2f))
                add(CSelectable())
                add(CFoV(null))
                add(CPlayerControlled)
                add(CMovable(2f, null, Stack(), 600f, null))
                add(CAnimated(IdleAnimation(playerCharacterAnim), listOf(
                        IdleAnimation(playerCharacterAnim),
                        AttackAnimation(playerCharacterAnim),
                        MovingAnimation(playerCharacterAnim)
                )))
            }
        }


        // Orc
        val orcTexture: Texture = assetManager[SpriteAsset.ORC.filePath]
        val orcSprite = Sprite(orcTexture)
        val createOrc = { pos: Vector2 ->
            Entity().apply {
                add(CCombatant(Enemy(3f),
                        Equipment(
                                MeleeWeapon("Large Axe",
                                        120,
                                        2f,
                                        NumberHandsToWield.TWO,
                                        1f
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
                add(CTransform(pos, 270f, orcSprite.width / 2f))
                add(CMovable(2f, null, Stack(), 8f, null))
            }
        }


        engine.addEntity(createPc(Vector2(1.5f, 1f)))
        engine.addEntity(createPc(Vector2(1.5f, 2f)))
//        engine.addEntity(createPc(Vector2(2.5f, 1f)))
//        engine.addEntity(createPc(Vector2(2.5f, 2f)))
        engine.addEntity(createOrc(Vector2(14.5f, 17f)))
        engine.addEntity(createOrc(Vector2(15.5f, 18f)))
//
//        engine.addEntity(createOrc(Vector2(22.5f, 21f)))
//        engine.addEntity(createOrc(Vector2(20.5f, 21f)))
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
