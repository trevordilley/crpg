package com.ancient.game.crpg.map

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

enum class SpawnKind { PARTY, ENEMY, TREASURE, CART, HEALER }

/**
 * A collision obstacle.
 *
 * [outline] is the shape as authored, and may be concave — use it for rendering
 * and as the source of nav-mesh vertices.
 *
 * [convex] is the same shape decomposed into convex pieces. Containment tests
 * must go through these, because libGDX's [Polygon.contains] is only correct for
 * convex polygons. The old code tested the raw outline and was silently wrong on
 * every concave obstacle.
 */
class CollisionPoly(val outline: List<Vector2>, convexPieces: List<List<Vector2>>) {

    private val pieces: List<Polygon> = convexPieces.map { piece ->
        Polygon(FloatArray(piece.size * 2).also { arr ->
            piece.forEachIndexed { i, v ->
                arr[i * 2] = v.x
                arr[i * 2 + 1] = v.y
            }
        })
    }

    /** Cheap reject before the per-piece test. */
    private val bounds: Rectangle = Rectangle().also { r ->
        val xs = outline.map { it.x }
        val ys = outline.map { it.y }
        r.set(xs.min(), ys.min(), xs.max() - xs.min(), ys.max() - ys.min())
    }

    fun contains(point: Vector2): Boolean =
        bounds.contains(point) && pieces.any { it.contains(point.x, point.y) }

    /** Closed loop of edges, last vertex joined back to the first. */
    fun edges(): List<Edge> = outline.mapIndexed { i, v ->
        Edge(v, outline[(i + 1) % outline.size])
    }
}

class Spawn(val kind: SpawnKind, val position: Vector2)

/**
 * A level's geometry, already converted to world units by [LevelLoader].
 *
 * World bounds are a declared property rather than derived from the background
 * art: the art is sized to fit under the 2048px texture ceiling and different
 * levels may pair different art with the same bounds. The playable area can
 * therefore be larger than the visible background.
 */
class Level(
    val name: String,
    val worldWidth: Float,
    val worldHeight: Float,
    val backgroundImage: String,
    val backgroundPosition: Vector2,
    val collision: List<CollisionPoly>,
    val occluders: List<List<Vector2>>,
    val spawns: List<Spawn>
) {

    fun spawns(kind: SpawnKind): List<Vector2> =
        spawns.filter { it.kind == kind }.map { it.position }

    fun collidesAt(point: Vector2): Boolean = collision.any { it.contains(point) }

    /**
     * Occluder edges for the field-of-view raycaster, plus a ring around the
     * world bounds.
     *
     * The ring is load-bearing: without it, rays that escape between obstacles
     * never hit anything and the visibility polygon comes out malformed. This is
     * what commit 49bb765 on the old branch was fixing.
     */
    fun occluderEdges(): List<Edge> {
        val fromOccluders = occluders.flatMap { poly ->
            poly.mapIndexed { i, v -> Edge(v, poly[(i + 1) % poly.size]) }
        }
        val bl = Vector2(0f, 0f)
        val br = Vector2(worldWidth, 0f)
        val tr = Vector2(worldWidth, worldHeight)
        val tl = Vector2(0f, worldHeight)
        return fromOccluders + listOf(
            Edge(bl, br), Edge(br, tr), Edge(tr, tl), Edge(tl, bl)
        )
    }
}

/**
 * Creature size classes, as radii in world units.
 *
 * SiUnits.UNIT (64px) is the minimum resolution of a character, so these are
 * half the 64 / 128 / 256px sprite diameters. The nav mesh builds one graph per
 * size, since a wider creature needs more clearance to round a corner.
 */
enum class CreatureSize(val radius: Float) {
    SMALL(0.5f),
    MEDIUM(1.0f),
    LARGE(2.0f)
}
