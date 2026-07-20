package com.ancient.game.crpg.map

import com.ancient.game.crpg.SiUnits
import com.ancient.game.crpg.gameLogger
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * Reads a `.level.json` produced by `tools/convert-h2d-scene.py`.
 *
 * THE UNIT BOUNDARY LIVES HERE. Level files are authored in pixels, because that
 * is what the editor exported and what the art is measured in. Everything the
 * loader returns is in world units. Nothing downstream should multiply by
 * [SiUnits.PIXELS_TO_METER] again — the old code scattered that conversion
 * across about a dozen call sites, which is exactly the kind of thing that
 * silently drifts.
 */
object LevelLoader {

    private val log = gameLogger(LevelLoader::class.java)

    fun load(path: String): Level = parse(Gdx.files.internal(path).readString())

    /**
     * Split from [load] so it can be unit-tested: parsing is pure, whereas
     * `Gdx.files` needs an initialised backend and would drag a GL context into
     * the test suite.
     */
    fun parse(json: String): Level {
        val root = JsonReader().parse(json)
        val scale = SiUnits.PIXELS_TO_METER

        val world = root.get("world")
        val background = root.get("background")

        val level = Level(
            name = root.getString("name"),
            worldWidth = world.getFloat("width") * scale,
            worldHeight = world.getFloat("height") * scale,
            backgroundImage = background.getString("image"),
            backgroundPosition = Vector2(
                background.getFloat("x") * scale,
                background.getFloat("y") * scale
            ),
            collision = root.get("collision").map { poly ->
                CollisionPoly(
                    outline = poly.get("outline").toPoints(scale),
                    convexPieces = poly.get("convex").map { it.toPoints(scale) }
                )
            },
            occluders = root.get("occluders").map { it.get("outline").toPoints(scale) },
            spawns = root.get("spawns").map { spawn ->
                Spawn(
                    kind = SpawnKind.valueOf(spawn.getString("kind")),
                    position = Vector2(
                        spawn.getFloat("x") * scale,
                        spawn.getFloat("y") * scale
                    )
                )
            }
        )

        log.info(
            "Loaded level '${level.name}': ${level.collision.size} collision, " +
                "${level.occluders.size} occluders, ${level.spawns.size} spawns, " +
                "world ${level.worldWidth} x ${level.worldHeight} units"
        )
        return level
    }

    /** `[[x, y], ...]` -> world-unit points. */
    private fun JsonValue.toPoints(scale: Float): List<Vector2> =
        map { pair -> Vector2(pair.getFloat(0) * scale, pair.getFloat(1) * scale) }
}
