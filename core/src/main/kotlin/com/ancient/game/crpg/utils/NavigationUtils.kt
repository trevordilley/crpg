package com.ancient.game.crpg.utils

import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import ktx.math.div

object NavigationUtils {
    fun angleFromPoints(
        cx: Float, cy: Float,
        px: Float, py: Float,
        nx: Float, ny: Float
        ): Float {
        return (atan2(nx- cx, ny - cy) - atan2(px - cx, py - cy)).let { Math.toDegrees(it.toDouble())}.toFloat()
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


            // Move points to origin for simpler maths
            prev.sub(cur)
            next.sub(cur)

            val halfAng = atan2(prev.x, prev.y) - atan2(next.x, next.y)

//            val halfAng = acos(
//                Vector2.dot(vec1.x,vec1.y, vec2.x,vec2.y)/
//                        Vector2.dst(vec1.x,vec1.y, vec2.x, vec2.y))/2f

             navPoints.add(cur.add(Vector2(cos(halfAng) * radius, sin(halfAng) * radius)))
        }
        return navPoints
    }
}