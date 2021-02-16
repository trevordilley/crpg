package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.systems.CTransform
import com.ancient.game.crpg.map.Edge
import com.ancient.game.crpg.map.MapManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.math.minus

class CFoV(var fovPoly: Polygon?) : Component {
   companion object {
       fun m() = mapperFor<CFoV>()
   }
}

class FieldOfViewSystem(private val mapManager: MapManager)
    : IteratingSystem(
        all(
                CFoV::class.java,
                CTransform::class.java
        ).get()) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val pos = entity[CTransform.m()]!!.position

        val opaqueEdges = mapManager.opaqueEdges

        // line 66 to 89 in sight-and-light.js
        val uniquePoints = opaqueEdges.map { listOf(it.p1, it.p2) }.flatten().toSet()

        // line 91 sight-and-light.js
        val angles = uniquePoints.map { p ->
            //            val a = Math.atan2((p.y - pos.y).toDouble(), (p.x - pos.x).toDouble()).toFloat()
            val a = (p - pos).angleRad()
            val tweak = 0.00001f
            listOf(a, a + tweak, a - tweak)
        }.flatten()

        val fovPoly = calculateFoV(pos.x, pos.y, opaqueEdges, angles)
        entity[CFoV.m()]!!.fovPoly = fovPoly
    }


    data class Ray(val a: Vector2, val b: Vector2)
    data class Segment(val ax: Float, val ay: Float, val bx: Float, val by: Float)
    data class Intersection(val x: Float, val y: Float, val param: Float, val angle: Float)


    fun getIntersection(ray: Ray, segment: Segment, angle: Float): Intersection? {

//            // RAY in parametric: Point + Delta*T1
        val r_px = ray.a.x
        val r_py = ray.a.y
        val r_dx = ray.b.x - ray.a.x
        val r_dy = ray.b.y - ray.a.y

//            // SEGMENT in parametric: Point + Delta*T2
        val s_px = segment.ax
        val s_py = segment.ay
        val s_dx = segment.bx - segment.ax
        val s_dy = segment.by - segment.ay

//            // Are they parallel? If so, no intersect

        val r_mag =
                (r_dx * r_dx + r_dy * r_dy)
                        .toDouble()
                        .let { Math.sqrt(it) }
        val s_mag =
                (s_dx * s_dx + s_dy * s_dy)
                        .toDouble()
                        .let { Math.sqrt(it) }
        if (r_dx / r_mag == s_dx / s_mag && r_dy / r_mag == s_dy / s_mag) {
            // Unit vectors are the same.
            return null
        }

        // SOLVE FOR T1 & T2
        // r_px+r_dx*T1 = s_px+s_dx*T2 && r_py+r_dy*T1 = s_py+s_dy*T2
        // ==> T1 = (s_px+s_dx*T2-r_px)/r_dx = (s_py+s_dy*T2-r_py)/r_dy
        // ==> s_px*r_dy + s_dx*T2*r_dy - r_px*r_dy = s_py*r_dx + s_dy*T2*r_dx - r_py*r_dx
        // ==> T2 = (r_dx*(s_py-r_py) + r_dy*(r_px-s_px))/(s_dx*r_dy - s_dy*r_dx)
        val t2 = (r_dx * (s_py - r_py) + r_dy * (r_px - s_px)) / (s_dx * r_dy - s_dy * r_dx);
        val t1 = (s_px + s_dx * t2 - r_px) / r_dx;

        // Must be within parametric whatevers for RAY/SEGMENT
        if (t1 < 0) return null
        if (t2 < 0 || t2 > 1) return null

        // Return the POINT OF INTERSECTION
        return Intersection(
                x = r_px + r_dx * t1,
                y = r_py + r_dy * t1,
                param = t1,
                angle = angle
        )
    }


    fun calculateFoV(sightX: Float, sightY: Float, walls: List<Edge>, uniqueAngles: List<Float>): Polygon {

        val intersect = { angle: Double ->
            // Calculate dx & dy from angle
            val dx = Math.cos(angle).toFloat()
            val dy = Math.sin(angle).toFloat()

            // Ray to that angle
            val ray = Ray(
                    Vector2(sightX, sightY),
                    Vector2(sightX + dx, sightY + dy)
            )

            // Find CLOSEST intersection
            walls
                    .mapNotNull {
                        getIntersection(
                                ray,
                                Segment(
                                        it.p1.x,
                                        it.p1.y,
                                        it.p2.x,
                                        it.p2.y
                                ),
                                angle.toFloat())
                    }
                    .minBy { it.param }
        }

        return uniqueAngles
                .mapNotNull { intersect(it.toDouble()) }
                .sortedBy { it.angle }
                .map { listOf(it.x, it.y) }
                .flatten()
                .let {
                    Polygon(it.toFloatArray())
                            .apply { setOrigin(sightX, sightY) }
                }
    }


}


