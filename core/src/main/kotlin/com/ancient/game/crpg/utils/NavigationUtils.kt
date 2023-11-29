package com.ancient.game.crpg.utils

import com.badlogic.gdx.math.Bezier
import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import games.rednblack.editor.renderer.utils.poly.earclipping.ewjordan.Triangle

data class Line(val start: Vector2, val end: Vector2)
object NavigationUtils {
    fun angleFromPoints(
        cx: Float, cy: Float,
        px: Float, py: Float,
        nx: Float, ny: Float
        ): Float {
        return (atan2(nx- cx, ny - cy) - atan2(px - cx, py - cy)).let { Math.toDegrees(it.toDouble())}.toFloat()
    }


    // Given a start and end point (a line), make it _slightly_ shorter so you
    // can essentially test and intersect the original line, but not include the
    // actual start and end of the line (similar to an open comparison vs a closed
    // comparison in a set)
    fun triviallyShortenLine(line: Line): Line {
        val start = line.start
        val end = line.end
        val tmp = Vector2(0f,0f)

        val shortenedStart = Vector2.Zero
        val shortenedEnd = Vector2.Zero
        Bezier.linear(shortenedStart, 0.001f, start, end, tmp)
        Bezier.linear(shortenedEnd, 0.999f, start, end, tmp)
        return Line(shortenedStart, shortenedEnd)
    }


    fun midPointOfLine(line: Line): Vector2 {
        val start = line.start
        val end = line.end
        val tmp = Vector2(0f,0f)

        val midPoint = Vector2.Zero
        Bezier.linear(midPoint, 0.5f, start, end, tmp)
        return  midPoint
    }


    // This tests if the lines start and end are within a given polygon, but doesn't
    // include the actual start and end, but points just _slightly_ inside the start
    // and end of the line.
    //
    // Essentially, if you want to test if a line in a triangle (in which two sides of that
    // triangle are connected edges of the polygon, and the third edge is a line between the two points
    // which are not connected) is inside the polygon or not, you can use this function
    // to do that.
    fun polygonContainsLineOpenStartAndEnd(line: Line, polygon: Polygon): Boolean {
        val shorter = triviallyShortenLine(line)
        return polygon.contains(shorter.start) and polygon.contains(shorter.end)
    }


    fun deriveNavPointsFromPolygon(polygon: Polygon, radius: Float): List<Vector2> {
        val verts = mutableListOf<Pair<Float, Float>>()


        for(i in (polygon.vertices.indices)) {
            if(i % 2 != 0) {
                continue
            }
            verts.add(Pair(polygon.vertices[i], polygon.vertices[i + 1]))
        }

        val navPoints = mutableListOf<Vector2>()

        for (i in verts.indices) {
            val cur = verts[i].let { (x, y) -> Vector2(x, y)}
            val next = if (i == verts.size - 1) {
                // Loop back to first vert
                verts[0]
            } else {
                // Choose the next vert
                verts[i + 1]
            }.let { (x,y) -> Vector2(x,y)}

            val prev = if (i == 0) {
                verts[verts.size - 1]
            } else {
                verts[i - 1]
            }.let { (x,y) -> Vector2(x,y)}



            val ang = angleFromPoints(cur.x, cur.y, prev.x, prev.y, next.x, next.y)

            val opposingLine = Line(prev, next)
            val polyContainsOpposingLine = polygonContainsLineOpenStartAndEnd(opposingLine, polygon)
            val navPoint = midPointOfLine(opposingLine)
                .sub(cur.x, cur.y)
                .nor()
                .scl(radius)
                .let { p ->

                    // This is the case where the cur vert is a protuding "point" on the
                    // polygon. So we know the angle is derived _within_ the polygon.
                    // So when we want to find the direction we'll place the nav point,
                    // we need to actually be on the opposite side
                    if(polyContainsOpposingLine) {
                        p.scl(-1f, -1f)
                    }

                    // This is the case where the cur vert is NOT protruding (concave?) on the
                    // polygon. This means we can simply use the half angle of the `ang` value,
                    // as the angle is computed facing outwards from the polygon
                    else {
                        p
                    }
                }

             navPoints.add(cur.add(navPoint))
        }
        return navPoints
    }
}