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

        val selectionCircleAnim: Aseprite = assetManager[AsepriteAsset.SELECTION_CIRCLE.assetName]
        val selectionSystem = SelectionSystem()

        val battleCommandSystem = BattleCommandSystem(viewportManager.viewport, mapManager, selectionSystem)

        inputManager = UserInputManager(listOf(battleCommandSystem, viewportManager))

        Gdx.input.inputProcessor = inputManager

        // Map


        log.info("Revving Engines")
        engine = PooledEngine()
        engine.addSystem(FovRenderSystem(viewportManager.viewport))
        engine.addSystem(
                RenderSystem(
                        batch,
                        viewportManager.viewport,
                        collisionPoints,
                        mapManager, showDebug = true
                )
        )
        engine.addSystem(BattleHealthUiRenderer(viewportManager.viewport))
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem(collisionPoints))
        engine.addSystem(HealthSystem())
        engine.addSystem(DeadSystem())
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())
        engine.addSystem(FieldOfViewSystem(mapManager))
        engine.addSystem(AnimationSystem())
        engine.addSystem(selectionSystem)
        // Player Character
        val playerCharacterTexture: Texture = assetManager[SpriteAsset.SWORD_SHIELD.filePath]
        val playerCharacterSprite = Sprite(playerCharacterTexture)
        val playerCharacterAnim: Aseprite = assetManager[AsepriteAsset.SWORD_SHIELD.assetName]


        val createPc = { pos: Vector2 ->

            Entity().apply {
                val spriteRadius = (playerCharacterSprite.width * SiUnits.PIXELS_TO_METER) / 2f
                val rotation = 90f
                add(CCombatant(Player,
                        Equipment(
                                MeleeWeapon(
                                        "Short Sword",
                                        30,
                                        3f,
                                        NumberHandsToWield.ONE,
                                        1f),
                                Shield("Large Shield", 0.7f),
                                Armor("Plate Mail", 20, 30f))
                ))
                add(CHealth(250,
                        250,
                        1,
                        3, 3))
                add(CTransform(pos, rotation, spriteRadius))
                add(CSelectable())
                add(CFoV(null))
                add(CPlayerControlled)
                add(CMovable(2f, null, Stack(), 600f, null))
                add(
                        CAnimated(
                                mapOf(
                                        AsepriteAsset.SWORD_SHIELD to AnimationData(
                                                IdleAnimation(playerCharacterAnim),
                                                listOf(
                                                        IdleAnimation(playerCharacterAnim),
                                                        AttackAnimation(playerCharacterAnim),
                                                        MovingAnimation(playerCharacterAnim)
                                                ),
                                                true
                                        ),
                                        AsepriteAsset.SELECTION_CIRCLE to AnimationData(
                                                OnSelectAnimation(selectionCircleAnim),
                                                listOf(
                                                        OnSelectAnimation(selectionCircleAnim),
                                                        SelectedAnimation(selectionCircleAnim)
                                                ),
                                                false
                                        )
                                )
                        )
                )
            }
        }

        // Orc
        val orcTexture: Texture = assetManager[SpriteAsset.ORC.filePath]
        val orcSprite = Sprite(orcTexture)
        val orcAnim: Aseprite = assetManager[AsepriteAsset.ORC.assetName]
        val createOrc = { pos: Vector2 ->
            Entity().apply {
                add(CCombatant(Enemy(3f),
                        Equipment(
                                MeleeWeapon("Large Axe",
                                        20,
                                        4f,
                                        NumberHandsToWield.TWO,
                                        1f
                                ),
                                Nothing,
                                Armor("Shirt", 0, 0f))
                ))
                add(CHealth(250,
                        250,
                        1,
                        3, 3))
                add(CTransform(pos, 270f, orcSprite.width / 2f))
                add(CMovable(2f, null, Stack(), 8f, null))
                add(CAnimated(
                        mapOf(
                                AsepriteAsset.ORC to AnimationData(
                                        IdleAnimation(orcAnim),
                                        listOf(
                                                IdleAnimation(orcAnim),
                                                AttackAnimation(orcAnim),
                                                MovingAnimation(orcAnim)
                                        ),
                                        true
                                )
                        )
                ))
            }
        }



        engine.addEntity(createPc(Vector2(1.5f, 1f)))
        engine.addEntity(createPc(Vector2(1.5f, 2f)))
        engine.addEntity(createPc(Vector2(2.5f, 1f)))
        engine.addEntity(createPc(Vector2(2.5f, 2f)))


        engine.addEntity(createOrc(Vector2(3.5f, 4.5f)))

        engine.addEntity(createOrc(Vector2(4.5f, 9f)))
        engine.addEntity(createOrc(Vector2(12.5f, 1.5f)))
        engine.addEntity(createOrc(Vector2(5.5f, 12f)))
        engine.addEntity(createOrc(Vector2(6.5f, 12f)))
        engine.addEntity(createOrc(Vector2(10.5f, 9.5f)))
        engine.addEntity(createOrc(Vector2(11.5f, 9.5f)))
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
