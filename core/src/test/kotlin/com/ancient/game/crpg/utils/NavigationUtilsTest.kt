package com.ancient.game.crpg.utils

import com.badlogic.gdx.math.Vector2
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitQuickcheck::class)
internal class NavigationUtilsTest {

    val squarePoly = com.badlogic.gdx.math.Polygon(floatArrayOf(
        0f, 0f, // bl
        0f, 10f, // tl
        10f, 10f, // tr
        10f, 0f // br
    ))


    /*
    Should look something like this:

    ..........
    ..........
    ..
    ..
    ..
    .........
    .........

    The hypotenuse of the triangle created by the bottom
    right vertex will pass through a lot of
    empty space.
     */
    val impactedSquare = com.badlogic.gdx.math.Polygon(floatArrayOf(
        0f, 0f, // bl
        0f, 10f, // tl
        10f, 10f, // tr
        10f, 9.5f,
        1f, 9.5f, // Vert of first convex Tri
        1f, 0.5f,
        10f, 0.5f,
        10f, 0f // br
    ))

    @Test
    fun `Simple square polygon has correct nav verts`() {
        val navPoints = NavigationUtils.deriveNavPointsFromPolygon(squarePoly, 32f)
        assert(navPoints.size == 4)
    }

    @Test
    fun `opposing edge in triangle intersects square poly`() {
        val verts = squarePoly.vertices

        val start =
            Vector2(verts[2], verts[3])
        val end =
            Vector2(verts[6], verts[7])

        val shouldIntersect = NavigationUtils.polygonContainsLineOpenStartAndEnd(Line(start, end), squarePoly)

        assert(shouldIntersect)
    }

    @Test
    fun `Impacted Square has concave tri, opposing edge should NOT intersect the poly`() {
        val verts = impactedSquare.vertices
        val start = Vector2(verts[6], verts[7])
        val end = Vector2(verts[10], verts[11])

        val shouldIntersect = NavigationUtils.polygonContainsLineOpenStartAndEnd(Line(start, end), impactedSquare)

        assert(!shouldIntersect)

    }
}