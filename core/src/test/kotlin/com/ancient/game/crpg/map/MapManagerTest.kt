package com.ancient.game.crpg.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Integration cover for the nav mesh against the REAL level geometry.
 *
 * NavMeshTest exercises the algorithm on synthetic polygons; this checks it
 * survives contact with what the editor actually exported — concave outlines,
 * obstacles of wildly different scale, and coordinates that went through the
 * pixel-to-world-unit conversion.
 */
class MapManagerTest {

    private val mapManager: MapManager by lazy {
        MapManager(LevelLoader.parse(File("assets/levels/main.level.json").readText()))
    }

    @Test
    fun `builds a graph for every size class from the real level`() {
        CreatureSize.values().forEach { size ->
            val graph = mapManager.navGraph(size)
            assertTrue(
                "no nav nodes for $size — the offset or discard rule rejected everything",
                graph.nodeCount > 0
            )
        }
    }

    @Test
    fun `larger creatures get a sparser graph`() {
        // More clearance means more offset points land inside obstacles or too
        // close to walls, so they get discarded. If this ever inverts, the
        // radius is not being applied.
        val small = mapManager.navGraph(CreatureSize.SMALL).nodeCount
        val large = mapManager.navGraph(CreatureSize.LARGE).nodeCount
        assertTrue("SMALL=$small LARGE=$large", large <= small)
    }

    @Test
    fun `party can path to the cart across the level`() {
        val from = mapManager.spawns(SpawnKind.PARTY).first()
        val to = mapManager.spawns(SpawnKind.CART).first()

        val path = mapManager.findPath(from, to, CreatureSize.SMALL)
        assertTrue("no path from party spawn to cart spawn", path.isNotEmpty())
        assertEquals("path should end at the goal", to, path.last())
    }

    @Test
    fun `every waypoint on a cross-level path is outside collision`() {
        val from = mapManager.spawns(SpawnKind.PARTY).first()
        val to = mapManager.spawns(SpawnKind.TREASURE).last()

        val path = mapManager.findPath(from, to, CreatureSize.SMALL)
        assertTrue(path.isNotEmpty())
        path.forEach { wp ->
            assertFalse("waypoint $wp is inside an obstacle", mapManager.collidesAt(wp))
        }
    }

    @Test
    fun `pathing into an obstacle yields no path rather than a bogus one`() {
        // NavMesh deliberately does not clamp the goal to a legal position, so a
        // click on a wall must come back empty. Whoever wires click-to-move owns
        // the clamp.
        val from = mapManager.spawns(SpawnKind.PARTY).first()
        val insideAnObstacle = mapManager.collision.first().outline
            .fold(com.badlogic.gdx.math.Vector2()) { acc, v -> acc.add(v) }
            .scl(1f / mapManager.collision.first().outline.size)

        if (mapManager.collidesAt(insideAnObstacle)) {
            assertTrue(
                "expected no path into an obstacle",
                mapManager.findPath(from, insideAnObstacle, CreatureSize.SMALL).isEmpty()
            )
        }
    }
}
