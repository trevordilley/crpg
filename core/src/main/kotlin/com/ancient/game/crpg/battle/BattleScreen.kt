package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.MAP_FILEPATH
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.ancient.game.crpg.equipment.*
import com.ancient.game.crpg.equipment.Nothing
import com.ancient.game.crpg.map.MapManager
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import games.rednblack.editor.renderer.SceneConfiguration
import games.rednblack.editor.renderer.SceneLoader
import games.rednblack.editor.renderer.resources.AsyncResourceManager
import ktx.app.KtxScreen
import java.util.Stack


class BattleScreen(private val assetManager: AssetManager, private val batch: Batch,
                   private val viewportManager: ViewportManager) : KtxScreen {

    private val log = gameLogger(this::class.java)

    private lateinit var engine: PooledEngine
    private lateinit var inputManager: UserInputManager
    private lateinit var mapRenderer: OrthogonalTiledMapRenderer

    private lateinit var sceneLoader: SceneLoader

    override fun show() {
        log.info("Setting up h2d")
        val config = SceneConfiguration()
        config.setResourceRetriever(assetManager.get("project.dt", AsyncResourceManager::class.java))
        sceneLoader = SceneLoader(config)

        log.info("Showing at camera pos ${viewportManager.viewport.camera.position}")
        log.info("Input Management")

        log.info("Building Map")
        val map: TiledMap = assetManager[MAP_FILEPATH]
        val mapManager = MapManager(map)
        val collisionPoints = mapManager.impassableCellPositions()
        mapRenderer = OrthogonalTiledMapRenderer(map, SiUnits.PIXELS_TO_METER, batch)

        val selectionCircleAnim: Aseprite = assetManager[AsepriteAsset.SELECTION_CIRCLE.assetName]
        val selectionSystem = SelectionSystem()


        val haulableSystem = HaulableSystem()

        val battleCommandSystem = BattleCommandSystem(viewportManager.viewport, mapManager, selectionSystem, haulableSystem)

        inputManager = UserInputManager(listOf(battleCommandSystem, viewportManager))

        Gdx.input.inputProcessor = inputManager

        log.info("Revving Engines")
        engine = PooledEngine()
        engine.addSystem(FovRenderSystem(viewportManager.viewport))
        engine.addSystem(
                RenderSystem(
                        batch,
                        viewportManager.viewport,
                        collisionPoints,
                        mapManager,
                        showDebug = true
                )
        )
        engine.addSystem(BattleHealthUiRenderer(viewportManager.viewport))
        engine.addSystem(haulableSystem)
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem(collisionPoints))
        engine.addSystem(HealthSystem(selectionSystem))
        engine.addSystem(DeadSystem(haulableSystem))
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())
        engine.addSystem(FieldOfViewSystem(mapManager))
        engine.addSystem(AnimationSystem())
        engine.addSystem(DropZoneSystem(selectionSystem))
        engine.addSystem(selectionSystem)
        // Player Character
        val playerCharacterAnim: Aseprite = assetManager[AsepriteAsset.SWORD_SHIELD.assetName]

        val createPc = { pos: Vector2 ->

            Entity().apply {
                val spriteRadius = (playerCharacterAnim.width * SiUnits.PIXELS_TO_METER) / 2f
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
                add(CSelectable(kind = CharacterSelect(Allegiance.PLAYER)))
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
                                                        MovingAnimation(playerCharacterAnim),
                                                        DieingAnimation(playerCharacterAnim)
                                                )

                                        ),
                                        AsepriteAsset.SELECTION_CIRCLE to AnimationData(
                                                OnSelectAnimation(selectionCircleAnim),
                                                listOf(
                                                        OnSelectAnimation(selectionCircleAnim),
                                                        SelectedAnimation(selectionCircleAnim)
                                                ), false

                                        )
                                )
                        )
                )
            }
        }

        // Orc
        val orcAnim: Aseprite = assetManager[AsepriteAsset.ORC.assetName]
        val createOrc = { pos: Vector2 ->
            Entity().apply {
                add(CCombatant(Enemy(3f),
                        Equipment(
                                MeleeWeapon("Large Axe",
                                        300,
                                        4f,
                                        NumberHandsToWield.TWO,
                                        1f
                                ),
                                Nothing,
                                Armor("Shirt", 0, 0f))
                ))
                add(CHealth(
                        250,
                        250,
                        1,
                        3,
                        3)
                )
                add(CTransform(pos, 270f, orcAnim.width / 2f))
                add(CMovable(2f, null, Stack(), 8f, null))
                add(CAnimated(
                        mapOf(
                                AsepriteAsset.ORC to AnimationData(
                                        IdleAnimation(orcAnim),
                                        listOf(
                                                IdleAnimation(orcAnim),
                                                AttackAnimation(orcAnim),
                                                MovingAnimation(orcAnim)
                                        )
                                )
                        )
                ))
            }
        }

        // DropZones have to come before other entities in render order!
        val lootDropZoneAnim: Aseprite = assetManager[AsepriteAsset.LOOT_DROP_ZONE.assetName]
        // OOps, I mixed up the way the cart if facing, so it's width and height are mixed up
        val height = lootDropZoneAnim.frame(0).regionWidth.toFloat()
        val normedH = height * SiUnits.PIXELS_TO_METER
        val width = lootDropZoneAnim.frame(0).regionHeight.toFloat()
        val normedW = width * SiUnits.PIXELS_TO_METER
        engine.addEntity(Entity().apply {
            val transform = CTransform(Vector2(2f, 2f), 270f, 1f)
            add(transform)
            val dropZoneRect =
                    Rectangle(
                            0f,
                            0f,
                            2f,
                            4f
                    )
            add(
                    CDropZone(TreasureKind, dropZoneRect)
            )
            add(CAnimated(
                    mapOf(
                            AsepriteAsset.LOOT_DROP_ZONE to AnimationData(
                                    IdleAnimation(lootDropZoneAnim),
                                    listOf(IdleAnimation(lootDropZoneAnim),
                                            OnDropAnimation(lootDropZoneAnim)
                                    )
                            )
                    )
            ))

        })

        // Healing DropZone
        val healingDropZoneAnim: Aseprite = assetManager[AsepriteAsset.HEALING_DROP_ZONE.assetName]
        val healingWidth = healingDropZoneAnim.frame(0).regionWidth.toFloat()
        val healingNormedW = healingWidth * SiUnits.PIXELS_TO_METER
        val healingHeight = healingDropZoneAnim.frame(0).regionHeight.toFloat()
        val healingNormedH = healingHeight * SiUnits.PIXELS_TO_METER
        engine.addEntity(Entity().apply {
            val transform = CTransform(Vector2(4f, 2f), 0f, 1f)
            add(transform)
            val dropZoneRect =
                    Rectangle(
                            2f,
                            0f,
                            2f,
                            4f
                    )
            add(
                    CDropZone(HealingKind, dropZoneRect)
            )
            add(CAnimated(
                    mapOf(
                            AsepriteAsset.HEALING_DROP_ZONE to AnimationData(
                                    IdleAnimation(healingDropZoneAnim),
                                    listOf(IdleAnimation(healingDropZoneAnim),
                                            OnDropAnimation(healingDropZoneAnim)
                                    )
                            )
                    )
            ))

        })

        val treasureAnim: Aseprite = assetManager[AsepriteAsset.TREASURE.assetName]

        // Treasure
        engine.addEntity(Entity().apply {

            val spriteRadius = (treasureAnim.width * SiUnits.PIXELS_TO_METER) / 2f
            add(CTransform(Vector2(1.5f, 7f), 0f, spriteRadius))
            add(CHaulable())
            add(CDiscovery("An impressive pile of gold coin. A cumbersome load to carry, but certainly worthwhile!"))
            add(CTreasure(100))
            add(CSelectable(kind = HaulableSelect))
            add(CAnimated(
                    mapOf(
                            AsepriteAsset.TREASURE to AnimationData(
                                    IdleAnimation(treasureAnim),
                                    listOf(
                                            IdleAnimation(treasureAnim),
                                            OnHaulAnimation(treasureAnim)
                                    )
                            ),
                            AsepriteAsset.SELECTION_CIRCLE to AnimationData(
                                    OnSelectAnimation(selectionCircleAnim),
                                    listOf(
                                            OnSelectAnimation(selectionCircleAnim),
                                            SelectedAnimation(selectionCircleAnim)
                                    ), false

                            )
                    )
            ))
        })


        engine.addEntity(createPc(Vector2(1.5f, 1f)))
        engine.addEntity(createPc(Vector2(1.5f, 2f)))
        engine.addEntity(createPc(Vector2(2.5f, 1f)))
        engine.addEntity(createPc(Vector2(2.5f, 2f)))

        engine.addEntity(createOrc(Vector2(4.5f, 9f)))
        engine.addEntity(createOrc(Vector2(5.5f, 12f)))
        engine.addEntity(createOrc(Vector2(10.5f, 9.5f)))
        engine.addEntity(createOrc(Vector2(11.5f, 9.5f)))
        engine.addEntity(createOrc(Vector2(22.5f, 21f)))
        engine.addEntity(createOrc(Vector2(20.5f, 21f)))

        log.info("Before loadScene")
        sceneLoader.loadScene("MainScene", viewportManager.viewport)
        log.info("After loadScene")

    }

    private var oneRun = false

    override fun render(delta: Float) {
        // Receive user input first
        inputManager.update()

        viewportManager.viewport.camera.update()
        //Clear screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
        // Update camera
//        viewportManager.update(delta)


//        // Render Map
//        mapRenderer.setView(viewportManager.viewport.camera as OrthographicCamera)
//        mapRenderer.render()


        // Update systems
//        engine.update(delta)
        // Render h2d
        viewportManager.viewport.apply()
        if(!oneRun) {
            println("Beforgggge process")
        }
        sceneLoader.engine.process()
        if(!oneRun) {
            println("after process")
            oneRun = true
        }
    }
}
