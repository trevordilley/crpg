package com.ancient.game.crpg.map

import com.badlogic.gdx.math.Vector2
import java.util.PriorityQueue

data class Edge(val p1: Vector2, val p2: Vector2)

/**
 * Level geometry queries: collision, occlusion, spawns, pathfinding.
 *
 * This replaces the tile-based implementation wholesale. That version derived
 * polygon edges *from* a tile grid — grouping adjacent impassable tiles, counting
 * shared vertices, walking corners to build edge loops — which is exactly what
 * the level editor now hands us directly. About 300 lines of inference deleted.
 */
class MapManager(private val level: Level) {

    val collision: List<CollisionPoly> = level.collision

    /** Occluder edges plus the world-bounds ring, for the field-of-view raycaster. */
    val opaqueEdges: List<Edge> = level.occluderEdges()

    fun spawns(kind: SpawnKind): List<Vector2> = level.spawns(kind)

    fun collidesAt(point: Vector2): Boolean = level.collidesAt(point)

    // ---------------------------------------------------------------------
    // INTERIM PATHFINDING
    //
    // A uniform grid A* over polygon containment. This is deliberately
    // throwaway: the real implementation is a visibility graph built from the
    // collision vertices (NavMesh, Phase 4), which produces taut paths without
    // post-hoc smoothing and scales with wall complexity rather than floor area.
    //
    // It lives here so click-to-move keeps working while that is built in
    // parallel. Delete this whole block when NavMesh lands, and forward
    // findPath() to it.
    // ---------------------------------------------------------------------

    private val cell = 0.5f
    private val cols = Math.ceil((level.worldWidth / cell).toDouble()).toInt()
    private val rows = Math.ceil((level.worldHeight / cell).toDouble()).toInt()

    private val blocked: BooleanArray = BooleanArray(cols * rows).also { grid ->
        val p = Vector2()
        for (cx in 0 until cols) {
            for (cy in 0 until rows) {
                p.set((cx + 0.5f) * cell, (cy + 0.5f) * cell)
                grid[cy * cols + cx] = level.collidesAt(p)
            }
        }
    }

    private fun inBounds(cx: Int, cy: Int) = cx in 0 until cols && cy in 0 until rows
    private fun isBlocked(cx: Int, cy: Int) = !inBounds(cx, cy) || blocked[cy * cols + cx]
    private fun toCell(v: Vector2) = Pair((v.x / cell).toInt(), (v.y / cell).toInt())
    private fun toWorld(cx: Int, cy: Int) = Vector2((cx + 0.5f) * cell, (cy + 0.5f) * cell)

    /**
     * Waypoints from [start] to [goal] in world units, excluding [start].
     * Empty if unreachable — callers should treat that as "don't move".
     */
    fun findPath(start: Vector2, goal: Vector2): List<Vector2> {
        val (sx, sy) = toCell(start)
        val (gx, gy) = toCell(goal)
        if (!inBounds(sx, sy) || !inBounds(gx, gy) || isBlocked(gx, gy)) return emptyList()
        if (sx == gx && sy == gy) return listOf(goal)

        val startIdx = sy * cols + sx
        val goalIdx = gy * cols + gx

        val cameFrom = HashMap<Int, Int>()
        val gScore = HashMap<Int, Float>().apply { put(startIdx, 0f) }
        val heuristic = { idx: Int ->
            val x = idx % cols
            val y = idx / cols
            Vector2.dst(x.toFloat(), y.toFloat(), gx.toFloat(), gy.toFloat())
        }
        val open = PriorityQueue<Int>(compareBy { (gScore[it] ?: Float.MAX_VALUE) + heuristic(it) })
        open.add(startIdx)
        val closed = HashSet<Int>()

        while (open.isNotEmpty()) {
            val current = open.poll()
            if (current == goalIdx) return reconstruct(cameFrom, current, goal)
            if (!closed.add(current)) continue

            val cx = current % cols
            val cy = current / cols
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    val ny = cy + dy
                    if (isBlocked(nx, ny)) continue
                    // Don't cut diagonally between two blocked orthogonals, which
                    // would let units slip through a wall corner.
                    if (dx != 0 && dy != 0 && (isBlocked(cx + dx, cy) || isBlocked(cx, cy + dy))) continue

                    val nIdx = ny * cols + nx
                    if (nIdx in closed) continue
                    val step = if (dx != 0 && dy != 0) 1.41421f else 1f
                    val tentative = (gScore[current] ?: Float.MAX_VALUE) + step
                    if (tentative < (gScore[nIdx] ?: Float.MAX_VALUE)) {
                        cameFrom[nIdx] = current
                        gScore[nIdx] = tentative
                        open.add(nIdx)
                    }
                }
            }
        }
        return emptyList()
    }

    private fun reconstruct(cameFrom: Map<Int, Int>, goalIdx: Int, goal: Vector2): List<Vector2> {
        val out = ArrayList<Vector2>()
        var cur = goalIdx
        while (true) {
            out.add(toWorld(cur % cols, cur / cols))
            cur = cameFrom[cur] ?: break
        }
        out.reverse()
        // Snap the last waypoint to the exact click, so units stop where asked
        // rather than at a cell centre.
        if (out.isNotEmpty()) out[out.size - 1] = goal
        return out
    }
}
