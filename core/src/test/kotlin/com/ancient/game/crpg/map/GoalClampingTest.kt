package com.ancient.game.crpg.map

import com.badlogic.gdx.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Clicking a wall must walk the unit up to it, not do nothing.
 *
 * NavMesh deliberately refuses illegal goals — correct for a pathfinder, wrong
 * for a mouse click — so MapManager clamps first. These pin that behaviour.
 */
class GoalClampingTest {

    private val mapManager: MapManager by lazy {
        MapManager(LevelLoader.parse(File("assets/levels/main.level.json").readText()))
    }

    private fun centroidOf(poly: CollisionPoly): Vector2 =
        poly.outline.fold(Vector2()) { acc, v -> acc.add(v) }.scl(1f / poly.outline.size)

    @Test
    fun `a legal point clamps to itself`() {
        val open = mapManager.spawns(SpawnKind.PARTY).first()
        assertTrue(mapManager.isStandable(open, CreatureSize.SMALL))
        assertEquals(open, mapManager.nearestStandable(open, CreatureSize.SMALL))
    }

    @Test
    fun `a point inside an obstacle clamps out of it`() {
        val poly = mapManager.collision.first { mapManager.collidesAt(centroidOf(it)) }
        val inside = centroidOf(poly)
        assertTrue("test premise: point should be inside", mapManager.collidesAt(inside))

        val clamped = mapManager.nearestStandable(inside, CreatureSize.SMALL)
        assertNotNull("expected a legal point near an obstacle centre", clamped)
        assertFalse("clamped point is still inside collision", mapManager.collidesAt(clamped!!))
        assertTrue("clamped point is not standable", mapManager.isStandable(clamped, CreatureSize.SMALL))
    }

    @Test
    fun `clamping keeps the creature radius clear of walls`() {
        // The whole point of clamping to `radius` rather than to the wall itself:
        // a unit standing exactly on the boundary would be half inside it.
        CreatureSize.values().forEach { size ->
            val poly = mapManager.collision.first { mapManager.collidesAt(centroidOf(it)) }
            val clamped = mapManager.nearestStandable(centroidOf(poly), size)
            if (clamped != null) {
                val graph = mapManager.navGraph(size)
                assertTrue(
                    "$size clamped to a point with less than radius clearance",
                    graph.hasClearance(clamped, clamped)
                )
            }
        }
    }

    @Test
    fun `pathing to a point inside a wall now succeeds instead of returning empty`() {
        val from = mapManager.spawns(SpawnKind.PARTY).first()
        val poly = mapManager.collision.first { mapManager.collidesAt(centroidOf(it)) }
        val intoTheWall = centroidOf(poly)

        val path = mapManager.findPath(from, intoTheWall, CreatureSize.SMALL)
        assertTrue("clicking a wall should walk up to it, not do nothing", path.isNotEmpty())
        path.forEach { wp ->
            assertFalse("waypoint $wp is inside an obstacle", mapManager.collidesAt(wp))
        }
    }

    @Test
    fun `clamped goal is closer to the click than an arbitrary nav node`() {
        // Guards against the clamp degenerating into "just pick any nav node",
        // which would send units to a random corner of the level.
        val poly = mapManager.collision.first { mapManager.collidesAt(centroidOf(it)) }
        val click = centroidOf(poly)
        val clamped = mapManager.nearestStandable(click, CreatureSize.SMALL)!!

        val graph = mapManager.navGraph(CreatureSize.SMALL)
        val furthestNode = graph.nodes.maxOf { it.position.dst(click) }
        assertTrue(
            "clamp landed as far away as the level's furthest nav node",
            clamped.dst(click) < furthestNode
        )
    }

    @Test
    fun `a point outside the world clamps to something legal or nothing at all`() {
        // Must not throw, and must not return a position inside geometry.
        val outside = Vector2(-50f, -50f)
        val clamped = mapManager.nearestStandable(outside, CreatureSize.SMALL)
        if (clamped != null) {
            assertFalse(mapManager.collidesAt(clamped))
        } else {
            assertNull(clamped)
        }
    }

    @Test
    fun `size class is chosen from radius without ever under-selecting`() {
        // Under-selecting is the dangerous direction: it would let a wide
        // creature path through a gap only a narrow one fits through.
        assertEquals(CreatureSize.SMALL, CreatureSize.forRadius(0.4f))
        assertEquals(CreatureSize.SMALL, CreatureSize.forRadius(0.5f))
        assertEquals(CreatureSize.MEDIUM, CreatureSize.forRadius(0.51f))
        assertEquals(CreatureSize.MEDIUM, CreatureSize.forRadius(1.0f))
        assertEquals(CreatureSize.LARGE, CreatureSize.forRadius(1.01f))
        assertEquals(CreatureSize.LARGE, CreatureSize.forRadius(99f))

        CreatureSize.values().forEach { size ->
            assertTrue(
                "forRadius(${size.radius}) must not pick something smaller",
                CreatureSize.forRadius(size.radius).radius >= size.radius
            )
        }
    }
}
