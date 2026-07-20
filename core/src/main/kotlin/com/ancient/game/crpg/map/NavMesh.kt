package com.ancient.game.crpg.map

import com.badlogic.gdx.ai.pfa.Connection
import com.badlogic.gdx.ai.pfa.DefaultGraphPath
import com.badlogic.gdx.ai.pfa.Heuristic
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array as GdxArray

/**
 * A visibility-graph nav mesh built from collision polygon outlines.
 *
 * The nav points are the obstacle vertices pushed outward by the moving unit's radius, so a
 * unit's *centre* can sit on any of them without its body overlapping a wall. Two nav points
 * are connected when a unit can walk directly between them, and A* over that graph is already
 * taut — there is no string-pulling pass, because the graph only ever contains straight
 * shortcuts in the first place.
 *
 * Everything here is pure geometry over libGDX math types. It knows nothing about `Level`,
 * `MapManager`, or the ECS, and it holds no GL state — which is why, unlike most of this
 * codebase, it is unit-testable.
 *
 * ## Units
 * Whatever unit the caller passes in. Obstacle coordinates, `radius`, and the returned
 * waypoints are all in the same space; conversion is the caller's problem.
 */

/** One obstacle boundary segment. Derived from the outlines at build time. */
data class NavWall(val a: Vector2, val b: Vector2)

/** A node of the visibility graph. [index] is stable for the lifetime of the graph. */
data class NavNode(val position: Vector2, val index: Int)

/** A traversable straight hop between two nav nodes. Cost is Euclidean distance. */
data class NavConnection(
    val from: NavNode,
    val to: NavNode,
    val weight: Float
) : Connection<NavNode> {
    override fun getFromNode(): NavNode = from
    override fun getToNode(): NavNode = to
    override fun getCost(): Float = weight
}

/**
 * A built visibility graph for one clearance [radius].
 *
 * [connections] is parallel to [nodes] — `connections[i]` are the outgoing hops of
 * `nodes[i]`. [walls] is kept so per-query endpoint visibility can be recomputed.
 */
data class NavGraph(
    val nodes: List<NavNode>,
    val walls: List<NavWall>,
    val radius: Float,
    val connections: List<List<NavConnection>>
) : IndexedGraph<NavNode> {

    override fun getNodeCount(): Int = nodes.size

    override fun getIndex(node: NavNode): Int = node.index

    override fun getConnections(fromNode: NavNode): GdxArray<Connection<NavNode>> =
        connections[fromNode.index].toGdxArray()

    /** True if a unit of this graph's radius can walk the straight line [a] → [b]. */
    fun hasClearance(a: Vector2, b: Vector2): Boolean =
        NavMesh.hasClearance(a, b, walls, radius)
}

object NavMesh {

    /**
     * Miter cap, as a multiple of `radius`. Past this the miter spike is long and thin enough
     * to be useless as a nav point, so we clip it into two points instead.
     */
    const val MITER_LIMIT = 4f

    /**
     * Build the visibility graph.
     *
     * [obstacles] are polygon outlines in world units. Winding may be either direction —
     * each polygon is normalised to counter-clockwise internally. Outlines may be concave;
     * containment uses an even-odd crossing test rather than libGDX's `Polygon.contains`,
     * which is only correct for convex polygons.
     */
    fun build(obstacles: List<List<Vector2>>, radius: Float): NavGraph {
        val walls = wallsOf(obstacles)
        val tol = tolerance(radius)

        val points = obstacles
            .flatMap { offsetPolygon(it, radius) }
            .filter { isStandable(it, obstacles, walls, radius) }
            .let { dedupe(it, tol) }

        val nodes = points.mapIndexed { i, p -> NavNode(p, i) }
        val connections = List(nodes.size) { mutableListOf<NavConnection>() }

        // O(n^2) over pairs. ~70 verts today; an acceleration structure would be noise.
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                if (!hasClearance(nodes[i].position, nodes[j].position, walls, radius)) continue
                val cost = nodes[i].position.dst(nodes[j].position)
                connections[i] += NavConnection(nodes[i], nodes[j], cost)
                connections[j] += NavConnection(nodes[j], nodes[i], cost)
            }
        }

        return NavGraph(nodes, walls, radius, connections.map { it.toList() })
    }

    /** Every outline turned into its boundary segments, dropping zero-length ones. */
    fun wallsOf(obstacles: List<List<Vector2>>): List<NavWall> = obstacles
        .filter { it.size >= 2 }
        .flatMap { poly -> poly.indices.map { NavWall(poly[it], poly[(it + 1) % poly.size]) } }
        .filter { it.a.dst2(it.b) > 0f }

    /**
     * Push every vertex of [polygon] outward along its angle bisector by `radius / sin(θ/2)`,
     * where θ is the interior angle — the standard miter offset. Acute corners land further
     * out, which is exactly the behaviour we want: you have to swing wider around a spike.
     *
     * The formula is implemented via the edge normals rather than θ directly. With `n1`/`n2`
     * the outward normals of the two edges meeting at `v` and `u` their normalised sum,
     * `n1·u == sin(θ/2)` for convex *and* reflex vertices alike, so there is no case split.
     *
     * Vertices past [miterLimit] (× radius) are clipped: instead of one far-flung miter point
     * we emit the two points where the offset edge lines cross the clip line, both sitting at
     * `miterLimit * radius` along the bisector. That keeps the pair walkable *around* the
     * spike tip — a plain bevel at distance `radius` would put both points level with the tip
     * and the hop between them would clip it.
     */
    fun offsetPolygon(
        polygon: List<Vector2>,
        radius: Float,
        miterLimit: Float = MITER_LIMIT
    ): List<Vector2> {
        if (polygon.size < 3) return emptyList()
        val poly = if (signedArea(polygon) < 0f) polygon.reversed() else polygon
        val n = poly.size
        val limit = miterLimit * radius
        val out = mutableListOf<Vector2>()

        for (i in 0 until n) {
            val v = poly[i]
            val d1 = Vector2(v).sub(poly[(i - 1 + n) % n])
            val d2 = Vector2(poly[(i + 1) % n]).sub(v)
            if (d1.isZero || d2.isZero) continue // duplicated vertex; nothing to bisect
            d1.nor()
            d2.nor()

            // Outward normal of a CCW edge running along `d` is (d.y, -d.x).
            val n1 = Vector2(d1.y, -d1.x)
            val n2 = Vector2(d2.y, -d2.x)
            val bisector = Vector2(n1).add(n2)

            if (bisector.len2() < 1e-12f) {
                // Exact 180° turn — a zero-width spur. No bisector exists; cap it flat.
                out += Vector2(v).mulAdd(n1, radius)
                out += Vector2(v).mulAdd(n2, radius)
                continue
            }

            val u = bisector.nor()
            val sinHalfTheta = n1.dot(u)
            val miter = radius / sinHalfTheta

            if (miter <= limit) {
                out += Vector2(v).mulAdd(u, miter)
            } else {
                out += clipToLimit(v, n1, d1, u, radius, limit)
                out += clipToLimit(v, n2, d2, u, radius, limit)
            }
        }
        return out
    }

    /**
     * Where the offset line of one edge (`v + radius*normal + t*dir`) crosses the clip line
     * sitting [limit] out along the bisector [u].
     */
    private fun clipToLimit(
        v: Vector2,
        normal: Vector2,
        dir: Vector2,
        u: Vector2,
        radius: Float,
        limit: Float
    ): Vector2 {
        val along = dir.dot(u)
        // We only clip once the bisector is nearly parallel to the edges, so `along` is far
        // from zero here. The guard is belt-and-braces for degenerate input.
        if (Math.abs(along) < 1e-6f) return Vector2(v).mulAdd(u, limit)
        val t = (limit - radius * normal.dot(u)) / along
        return Vector2(v).mulAdd(normal, radius).mulAdd(dir, t)
    }

    /**
     * Can a unit of [radius] stand centred on [p]? It must be outside every obstacle *and*
     * at least `radius` clear of every wall — being outside is not enough on its own, and
     * neither is clearance (a point deep inside a large obstacle is far from all its walls).
     */
    fun isStandable(
        p: Vector2,
        obstacles: List<List<Vector2>>,
        walls: List<NavWall>,
        radius: Float
    ): Boolean {
        val tol = tolerance(radius)
        if (obstacles.any { pointInPolygon(p, it) }) return false
        return walls.none { distancePointSegment(p, it.a, it.b) < radius - tol }
    }

    /**
     * The traversability test for a straight hop.
     *
     * Note this is segment-to-wall *distance*, not segment-vs-wall intersection. Offsetting
     * vertices by `radius` makes the vertices safe but says nothing about the segments
     * between them: a hop can pass within `radius` of a wall without ever crossing it, and a
     * unit walking that hop would scrape through the wall. The distance test is the correct
     * one; the intersection test is the tempting wrong one.
     */
    fun hasClearance(a: Vector2, b: Vector2, walls: List<NavWall>, radius: Float): Boolean {
        val minDistance = radius - tolerance(radius)
        return walls.none { distanceSegmentSegment(a, b, it.a, it.b) < minDistance }
    }

    // --- primitives -------------------------------------------------------------------

    /** Positive for counter-clockwise winding, negative for clockwise. */
    fun signedArea(polygon: List<Vector2>): Float {
        var sum = 0f
        for (i in polygon.indices) {
            val p = polygon[i]
            val q = polygon[(i + 1) % polygon.size]
            sum += p.x * q.y - q.x * p.y
        }
        return sum * 0.5f
    }

    /**
     * Even-odd crossing test. Correct for concave polygons, unlike `Polygon.contains`.
     * Points exactly on the boundary are unspecified — callers pair this with the clearance
     * test, which decides those cases.
     */
    fun pointInPolygon(p: Vector2, polygon: List<Vector2>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val crossingX = a.x + (p.y - a.y) / (b.y - a.y) * (b.x - a.x)
                if (p.x < crossingX) inside = !inside
            }
            j = i
        }
        return inside
    }

    fun distancePointSegment(p: Vector2, a: Vector2, b: Vector2): Float {
        val abX = b.x - a.x
        val abY = b.y - a.y
        val lengthSquared = abX * abX + abY * abY
        val t = if (lengthSquared <= 0f) 0f else
            (((p.x - a.x) * abX + (p.y - a.y) * abY) / lengthSquared).coerceIn(0f, 1f)
        return Vector2.dst(p.x, p.y, a.x + t * abX, a.y + t * abY)
    }

    /**
     * Minimum distance between two segments. Zero when they cross; otherwise the minimum is
     * always attained at one of the four endpoints, so the endpoint minimum is exact.
     */
    fun distanceSegmentSegment(a: Vector2, b: Vector2, c: Vector2, d: Vector2): Float {
        if (segmentsCross(a, b, c, d)) return 0f
        return minOf(
            distancePointSegment(a, c, d),
            distancePointSegment(b, c, d),
            distancePointSegment(c, a, b),
            distancePointSegment(d, a, b)
        )
    }

    /**
     * Proper (non-degenerate) crossing. Collinear overlaps and endpoint touches report false
     * and are picked up by the endpoint distances in [distanceSegmentSegment], which give ~0
     * for exactly those cases.
     */
    fun segmentsCross(a: Vector2, b: Vector2, c: Vector2, d: Vector2): Boolean {
        val d1 = cross(c, d, a)
        val d2 = cross(c, d, b)
        val d3 = cross(a, b, c)
        val d4 = cross(a, b, d)
        return ((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
            ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
    }

    private fun cross(o: Vector2, a: Vector2, b: Vector2): Float =
        (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

    /**
     * Comparison slack, scaled to the radius so this behaves the same whether the caller
     * works in metres or in pixels. Nav points sit at *exactly* `radius` from their own two
     * walls, so an exact `>=` would reject wall-hugging hops on float rounding alone.
     */
    fun tolerance(radius: Float): Float = maxOf(Math.abs(radius) * 1e-3f, 1e-5f)

    private fun dedupe(points: List<Vector2>, tolerance: Float): List<Vector2> {
        val out = mutableListOf<Vector2>()
        val toleranceSquared = tolerance * tolerance
        points.forEach { p ->
            if (out.none { it.dst2(p) <= toleranceSquared }) out += p
        }
        return out
    }
}

/**
 * A* from [start] to [goal], returning the waypoints in world units — `start` first, `goal`
 * last, obstacle corners in between. Empty when the goal is unreachable.
 *
 * Neither endpoint is a graph node. They are wired into a throwaway copy of the graph for the
 * duration of the query and discarded afterwards, so the built graph stays immutable and
 * queries are safe to run in any order.
 */
fun NavGraph.findPath(start: Vector2, goal: Vector2): List<Vector2> {
    if (hasClearance(start, goal)) return listOf(Vector2(start), Vector2(goal))

    val startNode = NavNode(Vector2(start), nodes.size)
    val goalNode = NavNode(Vector2(goal), nodes.size + 1)

    val fromStart = nodes
        .filter { hasClearance(startNode.position, it.position) }
        .map { NavConnection(startNode, it, startNode.position.dst(it.position)) }

    val toGoal = nodes
        .filter { hasClearance(it.position, goalNode.position) }
        .associate { it.index to NavConnection(it, goalNode, it.position.dst(goalNode.position)) }

    if (fromStart.isEmpty() || toGoal.isEmpty()) return emptyList()

    val query = QueryGraph(this, startNode, goalNode, fromStart, toGoal)
    val heuristic = Heuristic<NavNode> { node, endNode -> node.position.dst(endNode.position) }
    val path = DefaultGraphPath<NavNode>()

    if (!IndexedAStarPathFinder(query).searchNodePath(startNode, goalNode, heuristic, path)) {
        return emptyList()
    }
    return path.map { Vector2(it.position) }
}

/**
 * The built graph plus the two per-query endpoints. Indices `nodeCount` and `nodeCount + 1`
 * are the start and goal, which keeps every index dense and lets gdx-ai size its record array
 * off [getNodeCount] as usual.
 */
private class QueryGraph(
    private val base: NavGraph,
    private val startNode: NavNode,
    private val goalNode: NavNode,
    private val fromStart: List<NavConnection>,
    private val toGoal: Map<Int, NavConnection>
) : IndexedGraph<NavNode> {

    override fun getNodeCount(): Int = base.nodes.size + 2

    override fun getIndex(node: NavNode): Int = node.index

    override fun getConnections(fromNode: NavNode): GdxArray<Connection<NavNode>> =
        when (fromNode.index) {
            startNode.index -> fromStart
            goalNode.index -> emptyList()
            else -> base.connections[fromNode.index] + listOfNotNull(toGoal[fromNode.index])
        }.toGdxArray()
}

/** libGDX's `Array` is invariant in Kotlin, so the upcast has to be spelled out. */
private fun List<NavConnection>.toGdxArray(): GdxArray<Connection<NavNode>> =
    GdxArray(map { it as Connection<NavNode> }.toTypedArray())
