package com.ancient.game.crpg.map

import com.badlogic.gdx.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure geometry, no GL context, no libGDX lifecycle — the whole point of keeping [NavMesh]
 * decoupled from `Level` and the ECS.
 */
class NavMeshTest {

    // --- fixtures ---------------------------------------------------------------------

    /** Counter-clockwise unit-ish square, 10 across. */
    private val square = poly(0f to 0f, 10f to 0f, 10f to 10f, 0f to 10f)

    /** Equilateral, side 10 — every interior angle 60°. */
    private val equilateralTriangle = poly(0f to 0f, 10f to 0f, 5f to 8.660254f)

    /** Regular hexagon, circumradius 10 — every interior angle 120°. */
    private val hexagon = poly(
        10f to 0f, 5f to 8.660254f, -5f to 8.660254f,
        -10f to 0f, -5f to -8.660254f, 5f to -8.660254f
    )

    /** L-shape. The vertex at (1,1) is reflex: interior angle 270°. */
    private val lShape = poly(0f to 0f, 2f to 0f, 2f to 1f, 1f to 1f, 1f to 2f, 0f to 2f)

    /** A U opening upward: 2-thick walls, cavity x in [2,4], y in [2,10]. */
    private val uShape = poly(
        0f to 0f, 6f to 0f, 6f to 10f, 4f to 10f,
        4f to 2f, 2f to 2f, 2f to 10f, 0f to 10f
    )

    // --- 1. miter offset distance ------------------------------------------------------

    @Test
    fun `miter offset at an acute vertex pushes out by radius over sin half theta`() {
        val radius = 1f
        val expected = miterDistance(radius, 60.0)   // 2.0 — twice as far as a right angle

        val offsets = NavMesh.offsetPolygon(equilateralTriangle, radius)

        assertEquals(3, offsets.size)
        offsets.forEachIndexed { i, p ->
            assertEquals(expected, p.dst(equilateralTriangle[i]), 1e-4f)
        }
    }

    @Test
    fun `miter offset at a right-angle vertex pushes out by radius over sin half theta`() {
        val radius = 1f
        val expected = miterDistance(radius, 90.0)   // 1.41421

        val offsets = NavMesh.offsetPolygon(square, radius)

        assertEquals(4, offsets.size)
        offsets.forEachIndexed { i, p ->
            assertEquals(expected, p.dst(square[i]), 1e-4f)
        }
        // and outward, not inward: the offset square strictly contains the original
        assertVec(Vector2(-1f, -1f), offsets[0])
        assertVec(Vector2(11f, 11f), offsets[2])
    }

    @Test
    fun `miter offset at an obtuse vertex pushes out by radius over sin half theta`() {
        val radius = 1f
        val expected = miterDistance(radius, 120.0)  // 1.15470

        val offsets = NavMesh.offsetPolygon(hexagon, radius)

        assertEquals(6, offsets.size)
        offsets.forEachIndexed { i, p ->
            assertEquals(expected, p.dst(hexagon[i]), 1e-4f)
        }
    }

    @Test
    fun `miter offset at a reflex vertex pushes into the notch by radius over sin half theta`() {
        val radius = 0.2f
        val expected = miterDistance(radius, 270.0)  // 0.28284

        val offsets = NavMesh.offsetPolygon(lShape, radius)

        assertEquals(6, offsets.size)
        val reflex = offsets[3] // parallel to lShape[3] == (1,1)
        assertEquals(expected, reflex.dst(Vector2(1f, 1f)), 1e-4f)
        // The notch of this L is the quadrant x>1, y>1, so the point lands there.
        assertVec(Vector2(1.2f, 1.2f), reflex)
    }

    @Test
    fun `winding direction does not change the offset`() {
        val ccw = NavMesh.offsetPolygon(square, 1f)
        val cw = NavMesh.offsetPolygon(square.reversed(), 1f)

        assertEquals(ccw.size, cw.size)
        // Same set of points; the clockwise input is normalised before offsetting.
        ccw.forEach { p -> assertTrue("$p missing from $cw", cw.any { it.dst(p) < 1e-4f }) }
    }

    // --- 2. miter limit ----------------------------------------------------------------

    @Test
    fun `a spike past the miter limit is clipped into two points`() {
        val radius = 1f
        val limit = NavMesh.MITER_LIMIT * radius
        // Apex angle ~5.7 degrees, so the raw miter would sit 20 * radius out.
        val spike = poly(0f to 0f, 10f to -0.5f, 10f to 0.5f)
        assertTrue(miterDistance(radius, 5.7247) > limit)

        val offsets = NavMesh.offsetPolygon(spike, radius)

        // 2 points for the clipped apex, 1 each for the two blunt vertices.
        assertEquals(4, offsets.size)
        val apexPoints = offsets.filter { it.x < 0f }
        assertEquals(2, apexPoints.size)

        // Both sit on the clip line, exactly `limit` out along the outward bisector (-1, 0)...
        apexPoints.forEach { assertEquals(limit, -it.x, 1e-3f) }
        // ...which means the hop between them clears the spike tip by the full limit, where a
        // plain bevel at `radius` would have put both points level with the tip.
        assertEquals(
            limit,
            NavMesh.distancePointSegment(Vector2(0f, 0f), apexPoints[0], apexPoints[1]),
            1e-3f
        )
    }

    // --- 3. discarding unusable offset points ------------------------------------------

    @Test
    fun `offset points falling inside another obstacle are discarded`() {
        val radius = 1f
        val corner = Vector2(11f, 11f) // the miter point of square's (10,10) vertex
        // A second obstacle sitting right on top of that miter point.
        val blocker = poly(10.5f to 10.5f, 13f to 10.5f, 13f to 13f, 10.5f to 13f)

        val alone = NavMesh.build(listOf(square), radius)
        assertTrue(
            "expected a nav node at $corner when the square stands alone",
            alone.nodes.any { it.position.dst(corner) < 1e-3f }
        )

        val crowded = NavMesh.build(listOf(square, blocker), radius)
        assertFalse(
            "the miter point is buried inside the blocker and must be dropped",
            crowded.nodes.any { it.position.dst(corner) < 0.25f }
        )
    }

    @Test
    fun `offset points with less than radius of clearance are discarded`() {
        val radius = 1f
        // Gap of 1.2 between the two squares — under 2 * radius, so nothing fits between
        // them and every offset point aimed into the gap is unusable.
        val right = poly(11.2f to 0f, 21.2f to 0f, 21.2f to 10f, 11.2f to 10f)

        val graph = NavMesh.build(listOf(square, right), radius)

        assertTrue(graph.nodes.isNotEmpty())
        // Only where the squares actually face each other (y in 0..10) is the gap pinched.
        // Above and below them there is open floor, and nodes there are legitimate.
        assertFalse(
            "no node may sit in the 1.2-wide slot between the squares",
            graph.nodes.any {
                it.position.x > 10f && it.position.x < 11.2f &&
                    it.position.y > 0f && it.position.y < 10f
            }
        )
    }

    @Test
    fun `point in polygon is correct for a concave outline`() {
        // The notch quadrant is outside the L even though it is inside its bounding box.
        assertFalse(NavMesh.pointInPolygon(Vector2(1.5f, 1.5f), lShape))
        assertTrue(NavMesh.pointInPolygon(Vector2(0.5f, 1.5f), lShape))
        assertTrue(NavMesh.pointInPolygon(Vector2(1.5f, 0.5f), lShape))
        assertFalse(NavMesh.pointInPolygon(Vector2(3f, 3f), lShape))
    }

    // --- 4. visibility -----------------------------------------------------------------

    @Test
    fun `visibility is rejected through a wall`() {
        val walls = NavMesh.wallsOf(listOf(square))

        assertFalse(NavMesh.hasClearance(Vector2(-5f, 5f), Vector2(15f, 5f), walls, 1f))
        assertTrue(NavMesh.hasClearance(Vector2(-5f, 5f), Vector2(-5f, 20f), walls, 1f))
    }

    @Test
    fun `a segment clearing a corner by less than radius is rejected and by more is accepted`() {
        val walls = NavMesh.wallsOf(listOf(square))
        // Cuts the corner (10,10) diagonally. It never touches the square; its closest
        // approach is the midpoint (11,11), i.e. sqrt(2) ~ 1.414 away.
        val a = Vector2(12f, 10f)
        val b = Vector2(10f, 12f)
        assertEquals(1.41421f, NavMesh.distanceSegmentSegment(a, b, Vector2(10f, 10f), Vector2(10f, 0f)), 1e-4f)

        // A bare intersection test would accept this for any radius. The distance test does not.
        assertTrue("clearance 1.414 > radius 1.0", NavMesh.hasClearance(a, b, walls, 1.0f))
        assertFalse("clearance 1.414 < radius 2.0", NavMesh.hasClearance(a, b, walls, 2.0f))
    }

    @Test
    fun `hops that hug a wall at exactly radius are accepted`() {
        // Two adjacent miter points of the same square run parallel to their shared edge at
        // exactly `radius`. If the tolerance were wrong these would drop out and the graph
        // would fall apart.
        val graph = NavMesh.build(listOf(square), 1f)
        assertEquals(4, graph.nodes.size)
        assertTrue(graph.hasClearance(Vector2(-1f, -1f), Vector2(11f, -1f)))
    }

    // --- 5. pathfinding ----------------------------------------------------------------

    @Test
    fun `an unobstructed query short-circuits to a straight segment`() {
        val graph = NavMesh.build(listOf(square), 1f)

        val path = graph.findPath(Vector2(-5f, -5f), Vector2(-5f, 20f))

        assertEquals(2, path.size)
        assertVec(Vector2(-5f, -5f), path[0])
        assertVec(Vector2(-5f, 20f), path[1])
    }

    @Test
    fun `a path around a convex obstacle is taut`() {
        val radius = 1f
        val graph = NavMesh.build(listOf(square), radius)
        val start = Vector2(-5f, 5f)
        val goal = Vector2(15f, 5f)

        val path = graph.findPath(start, goal)

        // start, two corners of one side of the square, goal — nothing redundant.
        assertEquals(4, path.size)
        assertVec(start, path[0])
        assertVec(goal, path[3])

        // Both interior waypoints are miter points of the square, on the same side of it.
        listOf(path[1], path[2]).forEach { waypoint ->
            assertTrue(
                "$waypoint is not one of the graph's nav nodes",
                graph.nodes.any { it.position.dst(waypoint) < 1e-3f }
            )
        }
        assertEquals(path[1].y, path[2].y, 1e-3f)

        // Taut: 2 * dst((-5,5),(-1,-1)) + 12
        assertEquals(26.4222f, pathLength(path), 1e-2f)
        assertClearThroughout(path, graph, radius)
    }

    @Test
    fun `a path out of a concave obstacle does not cut the corner`() {
        val radius = 0.5f
        val graph = NavMesh.build(listOf(uShape), radius)
        val start = Vector2(3f, 3f)   // down inside the cavity
        val goal = Vector2(10f, 5f)   // outside, to the right of the U

        val path = graph.findPath(start, goal)

        assertTrue("expected a route out of the U", path.isNotEmpty())
        assertVec(start, path[0])
        assertVec(goal, path[path.size - 1])

        // The only way out is over the top of the U, so the path has to climb past the
        // mouth at y = 10 rather than cutting through a prong.
        assertTrue(
            "path never rises above the mouth of the U: $path",
            path.any { it.y > 10f }
        )
        assertTrue("path must detour", pathLength(path) > start.dst(goal) + 1f)

        // The real assertion: every leg keeps a full radius off every wall.
        assertClearThroughout(path, graph, radius)
    }

    @Test
    fun `an unreachable goal returns an empty path`() {
        val radius = 1f
        // A sealed room built from four overlapping 2-thick walls. No door.
        val room = listOf(
            poly(0f to 0f, 20f to 0f, 20f to 2f, 0f to 2f),
            poly(0f to 18f, 20f to 18f, 20f to 20f, 0f to 20f),
            poly(0f to 0f, 2f to 0f, 2f to 20f, 0f to 20f),
            poly(18f to 0f, 20f to 0f, 20f to 20f, 18f to 20f)
        )
        val graph = NavMesh.build(room, radius)

        assertTrue(graph.findPath(Vector2(-5f, 10f), Vector2(10f, 10f)).isEmpty())
    }

    @Test
    fun `node indices are stable and dense`() {
        val graph = NavMesh.build(listOf(square, lShape), 0.2f)

        assertEquals(graph.nodes.size, graph.nodeCount)
        graph.nodes.forEachIndexed { i, node ->
            assertEquals(i, node.index)
            assertEquals(i, graph.getIndex(node))
        }
        // Connections are symmetric — the graph is undirected.
        graph.connections.flatten().forEach { c ->
            assertTrue(
                "missing reverse of ${c.from.index}->${c.to.index}",
                graph.connections[c.to.index].any { it.to.index == c.from.index }
            )
        }
    }

    // --- helpers -----------------------------------------------------------------------

    private fun poly(vararg xy: Pair<Float, Float>) = xy.map { Vector2(it.first, it.second) }

    /** The spec'd offset: `d = radius / sin(theta / 2)`, theta being the interior angle. */
    private fun miterDistance(radius: Float, interiorAngleDegrees: Double) =
        (radius / Math.sin(Math.toRadians(interiorAngleDegrees / 2.0))).toFloat()

    private fun pathLength(path: List<Vector2>) =
        (0 until path.size - 1).fold(0f) { acc, i -> acc + path[i].dst(path[i + 1]) }

    private fun assertClearThroughout(path: List<Vector2>, graph: NavGraph, radius: Float) {
        for (i in 0 until path.size - 1) {
            val gap = graph.walls.minOf {
                NavMesh.distanceSegmentSegment(path[i], path[i + 1], it.a, it.b)
            }
            assertTrue(
                "leg ${path[i]} -> ${path[i + 1]} clears walls by only $gap, needs $radius",
                gap >= radius - NavMesh.tolerance(radius)
            )
        }
    }

    private fun assertVec(expected: Vector2, actual: Vector2, epsilon: Float = 1e-3f) {
        assertEquals("x of $actual vs $expected", expected.x, actual.x, epsilon)
        assertEquals("y of $actual vs $expected", expected.y, actual.y, epsilon)
    }
}
