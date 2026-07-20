package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.LEVEL_FILEPATH
import com.ancient.game.crpg.assetManagement.LEVEL_BACKGROUND_FILEPATH
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.ancient.game.crpg.battle.systems.*
import com.ancient.game.crpg.equipment.*
import com.ancient.game.crpg.equipment.Nothing
import com.ancient.game.crpg.map.CreatureSize
import com.ancient.game.crpg.map.Level
import com.ancient.game.crpg.map.LevelLoader
import com.ancient.game.crpg.map.MapManager
import com.ancient.game.crpg.map.SpawnKind
import com.ancient.game.crpg.systems.*
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.ashley.get
import java.util.Stack


class BattleScreen(private val assetManager: AssetManager, private val batch: Batch,
                   private val viewportManager: ViewportManager) : KtxScreen {

    private val log = gameLogger(this::class.java)

    private lateinit var engine: PooledEngine
    private lateinit var inputManager: UserInputManager

    override fun show() {
        log.info("Showing at camera pos ${viewportManager.viewport.camera.position}")
        log.info("Input Management")

        log.info("Building Map")
        val level: Level = LevelLoader.load(LEVEL_FILEPATH)
        val background: Texture = assetManager[LEVEL_BACKGROUND_FILEPATH]
        val mapManager = MapManager(level)

        val selectionCircleAnim: Aseprite = assetManager[AsepriteAsset.SELECTION_CIRCLE.assetName]
        val selectionSystem = SelectionSystem()


        val haulableSystem = HaulableSystem()

        val battleCommandSystem = BattleCommandSystem(viewportManager.viewport, mapManager, selectionSystem, haulableSystem)

        inputManager = UserInputManager(listOf(battleCommandSystem, viewportManager))

        Gdx.input.inputProcessor = inputManager

        Screenshot.logRenderState("show", viewportManager.viewport)
        log.info("Revving Engines")
        engine = PooledEngine()
        // Render order is load-bearing: FoV writes the depth mask, then the map
        // and sprites draw through it. See MapRenderSystem.
        engine.addSystem(FovRenderSystem(viewportManager.viewport, showDebug = false))
        engine.addSystem(MapRenderSystem(viewportManager.viewport, batch, background, level))
        engine.addSystem(
                RenderSystem(
                        batch,
                        viewportManager.viewport,
                        mapManager,
                        showDebug = true,
                        // Flip to true to inspect the visibility graph.
                        showNavMesh = false,
                        navMeshSize = CreatureSize.SMALL
                )
        )
        engine.addSystem(BattleHealthUiRendererSystem(viewportManager.viewport))
        engine.addSystem(haulableSystem)
        engine.addSystem(battleCommandSystem)
        engine.addSystem(BattleMovementSystem(mapManager::collidesAt, mapManager::findPath))
        engine.addSystem(HealthSystem(selectionSystem))
        engine.addSystem(DeadSystem(haulableSystem))
        engine.addSystem(BattleActionSystem())
        engine.addSystem(BattleActionEffectSystem())
        engine.addSystem(CombatantSystem())
        engine.addSystem(FieldOfViewSystem(mapManager.opaqueEdges))
        engine.addSystem(AnimationSystem())
        engine.addSystem(DropZoneSystem(selectionSystem))
        engine.addSystem(selectionSystem)
        // Player Character
        val playerCharacterAnim: Aseprite = assetManager[AsepriteAsset.SWORD_SHIELD.assetName]

        val createPc = { pos: Vector2 ->

            Entity().apply {
                val spriteRadius = (playerCharacterAnim.width * SiUnits.PIXELS_TO_METER) / 2f
                val rotation = 90f
                add(
                    CCombatant(
                        Player,
                        Equipment(
                                MeleeWeapon(
                                        "Short Sword",
                                        30,
                                        3f,
                                        NumberHandsToWield.ONE,
                                        1f),
                                Shield("Large Shield", 0.7f),
                                Armor("Plate Mail", 20, 30f))
                )
                )
                add(
                    CHealth(250,
                        250,
                        1,
                        3, 3)
                )
                add(CTransform(pos, rotation, spriteRadius))
                add(CSelectable(kind = CharacterSelect(Allegiance.PLAYER)))
                add(CFoV(null))
                add(CPlayerControlled)
                add(CMovable(2f, null, Stack(), 600f, null, size = CreatureSize.forRadius(spriteRadius)))
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
                add(
                    CCombatant(
                        Enemy(3f),
                        Equipment(
                                MeleeWeapon("Large Axe",
                                        300,
                                        4f,
                                        NumberHandsToWield.TWO,
                                        1f
                                ),
                                Nothing,
                                Armor("Shirt", 0, 0f))
                )
                )
                add(
                    CHealth(
                        250,
                        250,
                        1,
                        3,
                        3)
                )
                add(CTransform(pos, 270f, orcAnim.width / 2f))
                add(CMovable(2f, null, Stack(), 8f, null, size = CreatureSize.forRadius(orcAnim.width / 2f * SiUnits.PIXELS_TO_METER)))
                add(
                    CAnimated(
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
                )
                )
            }
        }

        // Fall back to a sensible spot if a level omits one of these, rather
        // than crashing on an empty list.
        val cartSpawn = mapManager.spawns(SpawnKind.CART).firstOrNull() ?: Vector2(2f, 2f)
        val healerSpawn = mapManager.spawns(SpawnKind.HEALER).firstOrNull() ?: Vector2(4f, 2f)

        // DropZones have to come before other entities in render order!
        val lootDropZoneAnim: Aseprite = assetManager[AsepriteAsset.LOOT_DROP_ZONE.assetName]
        // OOps, I mixed up the way the cart if facing, so it's width and height are mixed up
        val height = lootDropZoneAnim.frame(0).regionWidth.toFloat()
        val normedH = height * SiUnits.PIXELS_TO_METER
        val width = lootDropZoneAnim.frame(0).regionHeight.toFloat()
        val normedW = width * SiUnits.PIXELS_TO_METER
        engine.addEntity(Entity().apply {
            val transform = CTransform(cartSpawn, 270f, 1f)
            add(transform)
            val dropZoneRect =
                    Rectangle(
                            cartSpawn.x - 1f,
                            cartSpawn.y - 2f,
                            2f,
                            4f
                    )
            add(
                    CDropZone(TreasureKind, dropZoneRect)
            )
            add(
                CAnimated(
                    mapOf(
                            AsepriteAsset.LOOT_DROP_ZONE to AnimationData(
                                    IdleAnimation(lootDropZoneAnim),
                                    listOf(
                                        IdleAnimation(lootDropZoneAnim),
                                            OnDropAnimation(lootDropZoneAnim)
                                    )
                            )
                    )
            )
            )

        })

        // Healing DropZone
        val healingDropZoneAnim: Aseprite = assetManager[AsepriteAsset.HEALING_DROP_ZONE.assetName]
        val healingWidth = healingDropZoneAnim.frame(0).regionWidth.toFloat()
        val healingNormedW = healingWidth * SiUnits.PIXELS_TO_METER
        val healingHeight = healingDropZoneAnim.frame(0).regionHeight.toFloat()
        val healingNormedH = healingHeight * SiUnits.PIXELS_TO_METER
        engine.addEntity(Entity().apply {
            val transform = CTransform(healerSpawn, 0f, 1f)
            add(transform)
            val dropZoneRect =
                    Rectangle(
                            healerSpawn.x - 1f,
                            healerSpawn.y - 2f,
                            2f,
                            4f
                    )
            add(
                    CDropZone(HealingKind, dropZoneRect)
            )
            add(
                CAnimated(
                    mapOf(
                            AsepriteAsset.HEALING_DROP_ZONE to AnimationData(
                                    IdleAnimation(healingDropZoneAnim),
                                    listOf(
                                        IdleAnimation(healingDropZoneAnim),
                                            OnDropAnimation(healingDropZoneAnim)
                                    )
                            )
                    )
            )
            )

        })

        val treasureAnim: Aseprite = assetManager[AsepriteAsset.TREASURE.assetName]

        // Treasure - one per authored spawn
        mapManager.spawns(SpawnKind.TREASURE).forEach { treasurePos ->
        engine.addEntity(Entity().apply {

            val spriteRadius = (treasureAnim.width * SiUnits.PIXELS_TO_METER) / 2f
            add(CTransform(treasurePos, 0f, spriteRadius))
            add(CHaulable())
            add(CDiscovery("An impressive pile of gold coin. A cumbersome load to carry, but certainly worthwhile!"))
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
        })
        }


        // Spawn positions now come from the level rather than being hardcoded.
        mapManager.spawns(SpawnKind.PARTY).forEach { engine.addEntity(createPc(it)) }

        mapManager.spawns(SpawnKind.ENEMY).forEach { engine.addEntity(createOrc(it)) }

        scriptedMove(mapManager)
    }

    /**
     * Issues a real move order at startup, so pathfinding can be *seen* rather
     * than only asserted.
     *
     * I cannot play the game to check that units route sensibly around
     * obstacles, so this drives it: every player character is ordered to the far
     * corner of the level, which forces a path around several tree canopies.
     * Combined with the debug path overlay and a framebuffer capture, that turns
     * an untestable claim into a picture.
     *
     *   ./gradlew :desktop:run -Dcrpg.demo=move -Dcrpg.capture=240
     *
     * Off unless -Dcrpg.demo=move is set.
     */
    private fun scriptedMove(mapManager: MapManager) {
        if (System.getProperty("crpg.demo") !in listOf("move", "fight")) return

        val party = mapManager.spawns(SpawnKind.PARTY).firstOrNull() ?: return

        // "fight" walks the party onto the enemies so combat actually triggers;
        // "move" sends them to the furthest treasure, which forces a route
        // around several obstacles.
        val target = if (System.getProperty("crpg.demo") == "fight") {
            mapManager.spawns(SpawnKind.ENEMY).minByOrNull { it.dst2(party) } ?: return
        } else {
            mapManager.spawns(SpawnKind.TREASURE).maxByOrNull { it.dst2(party) } ?: return
        }

        var ordered = 0
        val fightMode = System.getProperty("crpg.demo") == "fight"
        engine.getEntitiesFor(
            com.badlogic.ashley.core.Family.all(CMovable::class.java, CTransform::class.java).get()
        ).forEach { entity ->
            // In fight mode leave the orcs where they are, so the party has
            // something to close on rather than everyone converging on a point.
            if (fightMode && entity[CCombatant.m()]?.combatant !is Player) return@forEach
            val movable = entity[CMovable.m()]!!
            val from = entity[CTransform.m()]!!.position
            val path = mapManager.findPath(from, target, movable.size)
            if (path.isNotEmpty()) {
                movable.destination = path.last()
                movable.path = Stack<Vector2>().apply { path.reversed().forEach { push(it) } }
                ordered++
                log.info("DEMO: ${from} -> ${target} via ${path.size} waypoints (${movable.size})")
            } else {
                log.info("DEMO: no path from ${from} to ${target} for ${movable.size}")
            }
        }
        log.info("DEMO: ordered $ordered entities to move")

        // Point the camera at the action. Without this the demo can walk units
        // clean out of the visible region — the viewport only covers part of the
        // 32x32 world — and the capture shows an empty field.
        viewportManager.viewport.camera.position.set(target.x, target.y, 0f)
        viewportManager.viewport.camera.update()
        log.info("DEMO: camera centred on $target")
    }


    // No screen overrode resize(), so the viewport was never updated when the
    // window changed size.
    //
    // centerCamera = false deliberately: libGDX calls resize() after show(), so
    // re-centring here would snap the camera back to the middle of the map and
    // throw away wherever the player had scrolled to.
    override fun resize(width: Int, height: Int) {
        viewportManager.viewport.update(width, height, false)
        Screenshot.logRenderState("resize $width x $height", viewportManager.viewport)
    }

    override fun render(delta: Float) {
        // Receive user input first
        inputManager.update()

        // Update camera
        viewportManager.update(delta)

        // The render loop never cleared the colour buffer at all, leaving the
        // back buffer undefined between frames.
        com.badlogic.gdx.Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        com.badlogic.gdx.Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)

        // The map is drawn by MapRenderSystem inside the engine, so that it
        // lands after FovRenderSystem has written the visibility mask.

        // Update systems
        engine.update(delta)
    }
}
