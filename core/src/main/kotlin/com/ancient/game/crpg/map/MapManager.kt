package com.ancient.game.crpg.map

import com.badlogic.gdx.math.Vector2

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
    // PATHFINDING
    //
    // A visibility graph built from the collision outlines: nav nodes are the
    // obstacle vertices pushed outward by the creature radius, connected where
    // the segment between them keeps that much clearance from every wall.
    //
    // Paths come out taut, so there is no smoothing pass, and the node count
    // scales with wall complexity rather than floor area - the grid this
    // replaced needed ~4k cells to cover the same level with 8 obstacles.
    //
    // One graph per size class. A larger creature's graph is genuinely sparser,
    // so a big unit can legitimately have no path where a small one does, e.g.
    // through a gap narrower than its diameter. Treat an empty path as a normal
    // outcome, not an error.
    // ---------------------------------------------------------------------

    private val outlines: List<List<Vector2>> = level.collision.map { it.outline }

    private val graphs: Map<CreatureSize, NavGraph> =
        CreatureSize.values().associateWith { NavMesh.build(outlines, it.radius) }

    fun navGraph(size: CreatureSize): NavGraph = graphs.getValue(size)

    /**
     * Can a unit of this size class stand centred here? Outside every obstacle
     * *and* at least its radius clear of every wall.
     *
     * `hasClearance(p, p)` is a degenerate zero-length segment, which reduces to
     * point-to-wall distance — exactly the clearance test we want.
     */
    fun isStandable(point: Vector2, size: CreatureSize = CreatureSize.MEDIUM): Boolean =
        !level.collidesAt(point) && graphs.getValue(size).hasClearance(point, point)

    /**
     * The legal standing position closest to [point], or null if none exists.
     *
     * NavMesh deliberately refuses illegal goals rather than guessing, which is
     * correct for a pathfinder but wrong for a mouse click: players click walls
     * constantly and expect the unit to walk as close as it can. So the clamp
     * lives here, on the game-facing side.
     *
     * Candidates are the projections of [point] onto every wall, pushed clear by
     * the creature radius, plus the nav nodes themselves as a backstop for a
     * click deep inside a large obstacle.
     */
    fun nearestStandable(point: Vector2, size: CreatureSize = CreatureSize.MEDIUM): Vector2? {
        if (isStandable(point, size)) return point

        val graph = graphs.getValue(size)
        val inside = level.collidesAt(point)
        // Overshoot slightly: landing exactly at `radius` is on the boundary and
        // float rounding can put it back on the wrong side.
        val push = graph.radius * 1.02f

        val projected = graph.walls.map { wall ->
            val closest = NavMesh.closestPointOnSegment(point, wall.a, wall.b)
            // Inside an obstacle we push outward along (closest - point); outside
            // but too close, we push away along (point - closest).
            val dir = if (inside) closest.cpy().sub(point) else point.cpy().sub(closest)
            if (dir.len2() < 1e-6f) closest.cpy() else closest.cpy().add(dir.nor().scl(push))
        }

        return (projected + graph.nodes.map { it.position })
            .filter { isStandable(it, size) }
            .minByOrNull { it.dst2(point) }
    }

    /**
     * Waypoints from [start] to [goal] in world units, empty if unreachable.
     *
     * [goal] is clamped to the nearest legal standing position first, so clicking
     * a wall walks up to it rather than doing nothing.
     */
    fun findPath(start: Vector2, goal: Vector2, size: CreatureSize = CreatureSize.MEDIUM): List<Vector2> {
        val target = nearestStandable(goal, size) ?: return emptyList()
        return graphs.getValue(size).findPath(start, target)
    }
}
