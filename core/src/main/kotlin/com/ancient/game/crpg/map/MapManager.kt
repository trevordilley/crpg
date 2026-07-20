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
     * Waypoints from [start] to [goal] in world units, empty if unreachable.
     *
     * Note this does not clamp [goal] to a legal standing position: clicking
     * inside an obstacle yields no path rather than the nearest reachable point.
     */
    fun findPath(start: Vector2, goal: Vector2, size: CreatureSize = CreatureSize.MEDIUM): List<Vector2> =
        graphs.getValue(size).findPath(start, goal)
}
