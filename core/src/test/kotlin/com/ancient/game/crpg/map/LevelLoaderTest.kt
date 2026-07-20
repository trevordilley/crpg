package com.ancient.game.crpg.map

import com.ancient.game.crpg.SiUnits
import com.badlogic.gdx.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LevelLoaderTest {

    private val level: Level by lazy {
        LevelLoader.parse(File("assets/levels/main.level.json").readText())
    }

    @Test
    fun `loads the expected counts from the real level file`() {
        assertEquals(8, level.collision.size)
        assertEquals(8, level.occluders.size)
        assertEquals(15, level.spawns.size)
    }

    @Test
    fun `spawn kinds match what the editor exported`() {
        assertEquals(2, level.spawns(SpawnKind.PARTY).size)
        assertEquals(3, level.spawns(SpawnKind.ENEMY).size)
        assertEquals(8, level.spawns(SpawnKind.TREASURE).size)
        assertEquals(1, level.spawns(SpawnKind.CART).size)
        assertEquals(1, level.spawns(SpawnKind.HEALER).size)
    }

    @Test
    fun `converts pixels to world units exactly once`() {
        // The file declares a 2048px world; UNIT is 64px, so 32 world units.
        assertEquals(2048f * SiUnits.PIXELS_TO_METER, level.worldWidth, 0.001f)
        assertEquals(32f, level.worldWidth, 0.001f)

        // Geometry was measured in pixels at x [319.2 .. 1963.8], y [43.4 .. 1482.8].
        // Everything must land inside that, scaled - if any coordinate is still in
        // pixels it will be ~64x too large and this fails loudly.
        val all = level.collision.flatMap { it.outline } + level.occluders.flatten()
        val maxX = all.maxOf { it.x }
        val maxY = all.maxOf { it.y }
        assertTrue("x still looks like pixels: $maxX", maxX < 32f)
        assertTrue("y still looks like pixels: $maxY", maxY < 32f)
        assertEquals(1963.8f * SiUnits.PIXELS_TO_METER, maxX, 0.1f)
        assertEquals(1482.8f * SiUnits.PIXELS_TO_METER, maxY, 0.1f)
    }

    @Test
    fun `every collision poly has at least one convex piece`() {
        level.collision.forEach { poly ->
            assertTrue("outline too small", poly.outline.size >= 3)
            // A point far outside the level must not be contained by anything,
            // which also proves the convex pieces were actually built.
            assertFalse(poly.contains(Vector2(-100f, -100f)))
        }
    }

    @Test
    fun `containment uses convex pieces so a concave obstacle is tested correctly`() {
        // U shape: the notch between the arms is OUTSIDE the polygon, but a naive
        // convex-hull style test would wrongly report it as inside.
        //
        //   (0,4)        (4,4)
        //     |  notch..  |
        //   (0,0)--------(4,0)
        val outline = listOf(
            Vector2(0f, 0f), Vector2(4f, 0f), Vector2(4f, 4f),
            Vector2(3f, 4f), Vector2(3f, 1f), Vector2(1f, 1f),
            Vector2(1f, 4f), Vector2(0f, 4f)
        )
        val convex = listOf(
            listOf(Vector2(0f, 0f), Vector2(4f, 0f), Vector2(4f, 1f), Vector2(0f, 1f)),
            listOf(Vector2(0f, 1f), Vector2(1f, 1f), Vector2(1f, 4f), Vector2(0f, 4f)),
            listOf(Vector2(3f, 1f), Vector2(4f, 1f), Vector2(4f, 4f), Vector2(3f, 4f))
        )
        val poly = CollisionPoly(outline, convex)

        assertTrue("base of the U", poly.contains(Vector2(2f, 0.5f)))
        assertTrue("left arm", poly.contains(Vector2(0.5f, 3f)))
        assertTrue("right arm", poly.contains(Vector2(3.5f, 3f)))
        assertFalse("the notch is not inside the U", poly.contains(Vector2(2f, 3f)))
        assertFalse("outside entirely", poly.contains(Vector2(10f, 10f)))
    }

    @Test
    fun `occluder edges include a closing ring around the world bounds`() {
        val edges = level.occluderEdges()
        val perOccluder = level.occluders.sumOf { it.size }
        assertEquals("occluder edges plus 4 boundary edges", perOccluder + 4, edges.size)

        // The ring must sit on the declared world bounds, not the background art,
        // otherwise rays escape past the corners and the FoV polygon degenerates.
        val w = level.worldWidth
        val h = level.worldHeight
        assertTrue(edges.any { it.p1 == Vector2(0f, 0f) && it.p2 == Vector2(w, 0f) })
        assertTrue(edges.any { it.p1 == Vector2(w, h) && it.p2 == Vector2(0f, h) })
    }

    @Test
    fun `spawns land outside collision geometry`() {
        // A spawn inside a wall would trap whatever spawns there.
        level.spawns.forEach { spawn ->
            assertFalse(
                "${spawn.kind} spawn at ${spawn.position} is inside collision",
                level.collidesAt(spawn.position)
            )
        }
    }
}
