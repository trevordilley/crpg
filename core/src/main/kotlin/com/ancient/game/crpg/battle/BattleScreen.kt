package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.MAP_FILEPATH
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.ancient.game.crpg.equipment.*
import com.ancient.game.crpg.equipment.Nothing
import com.ancient.game.crpg.map.Edge
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
import ktx.ashley.add
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

        log.info("Before loadScene")
        sceneLoader.loadScene("MainScene", viewportManager.viewport)

        // TODO: Enumify all this! Pull into a lambda!
        val spawnPoints = sceneLoader.sceneVO.composite.content.get("games.rednblack.editor.renderer.data.LabelVO")
        val partySpawns = spawnPoints.filter {it.itemIdentifier == "PARTY_SPAWN"}
        val treasureSpawns = spawnPoints.filter { it.itemIdentifier == "TREASURE_SPAWN"}
        val cartSpawn = spawnPoints.filter {it.itemIdentifier == "CART_SPAWN" }
        val healerSpawn = spawnPoints.filter {it.itemIdentifier == "HEALER_SPAWN"}
        val enemySpawns = spawnPoints.filter {it.itemIdentifier == "ENEMY_SPAWN"}
        val backgroundImage = sceneLoader.sceneVO.composite.content.get("games.rednblack.editor.renderer.data.SimpleImageVO").filter { it.itemIdentifier == "BACKGROUND"}.first()
        val backgroundPosition = Vector2(backgroundImage.x, backgroundImage.y)
        val occluders = sceneLoader.sceneVO.composite.content.get("games.rednblack.editor.renderer.data.ColorPrimitiveVO")
            .filter {it.itemIdentifier == "OCCLUDER"}
            .let { it }
            .map { it.shape.vertices.map { v -> Vector2(v.x + it.x, v.y + it.y) }  }
            .map { poly ->
                poly.withIndex().map { (i, v) ->

                    // Perhaps a bit to clever, if i+1 is out of bounds then
                    // we've circled back, so connect the last vert with the first vert.
                    //
                    // I suppose I could put some kind of assert here? Like... if i is
                    // out of bounds by more than 1? Meh... _it'll be fine..._
                    val nxt = poly.getOrElse(i + 1) { poly[0] }
                    Edge(v, nxt)
                }
            }.toMutableList().apply {

                val bl = Vector2(backgroundPosition.x, backgroundPosition.y)
                val br = Vector2(1920f - backgroundPosition.x,backgroundPosition.y)
                val tr = Vector2(1920f - backgroundPosition.x,1080f - backgroundPosition.y)
                val tl = Vector2(backgroundPosition.x,1080f - backgroundPosition.y)
                add(listOf(
                    Edge(bl, br ),
                    Edge(br, tr ),
                    Edge(tr, tl ),
                    Edge(tl, bl ),
                ))
            }.toList()


        log.info("After loadScene")

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
        engine.addSystem(
                RenderSystem(
                        sceneLoader.batch,
                        viewportManager.viewport,
                        collisionPoints,
                        mapManager,
                        occluders,
                        showDebug = false
                )
        )
        engine.addSystem(haulableSystem)
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem(collisionPoints))
        engine.addSystem(HealthSystem(selectionSystem))
        engine.addSystem(DeadSystem(haulableSystem))
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())
        engine.addSystem(FieldOfViewSystem(occluders))
        engine.addSystem(FovRenderSystem(viewportManager.viewport, sceneLoader.batch))
//        engine.addSystem(BattleHealthUiRenderer(viewportManager.viewport))
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
                add(CMovable(2f, Vector2(1000f, 1000f), Stack(), 600f, null))
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
        partySpawns.map { engine.addEntity(createPc(Vector2(it.x, it.y)))}
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
        enemySpawns.map { engine.addEntity(createOrc(Vector2(it.x, it.y)))}

        // DropZones have to come before other entities in render order!
        val lootDropZoneAnim: Aseprite = assetManager[AsepriteAsset.LOOT_DROP_ZONE.assetName]
        engine.addEntity(Entity().apply {
            val transform = CTransform(Vector2(cartSpawn[0].x, cartSpawn[0].y), 270f, 1f)
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
        engine.addEntity(Entity().apply {
            val transform = CTransform(Vector2(healerSpawn[0].x, healerSpawn[0].y), 0f, 1f)
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
        val createTreasure = { pos: Vector2 -> Entity().apply {

            val spriteRadius = (treasureAnim.width * SiUnits.PIXELS_TO_METER) / 2f
            add(CTransform(pos, 0f, spriteRadius))
            add(CHaulable())
            add(CDiscovery("An impressive pile of gold coin. A heavy load to carry, but certainly worthwhile!"))
            add(CTreasure(100))
            add(CSelectable(kind = HaulableSelect))
            add(
                CAnimated(
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
                )
            )
        }}
        treasureSpawns.forEach { engine.addEntity(createTreasure(Vector2(it.x, it.y)))}
    }


    override fun render(delta: Float) {
        // Receive user input first
        inputManager.update()

        viewportManager.viewport.camera.update()
        //Clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1.0f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
        // Update camera
//        viewportManager.update(delta)


//        // Render Map
//        mapRenderer.setView(viewportManager.viewport.camera as OrthographicCamera)
//        mapRenderer.render()

        // Update systems
        // Render h2d
        viewportManager.update(delta)
        sceneLoader.engine.process()
        engine.update(delta)

    }
}
