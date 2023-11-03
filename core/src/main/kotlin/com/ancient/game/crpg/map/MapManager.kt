package com.ancient.game.crpg.map

import com.badlogic.gdx.ai.pfa.Connection
import com.badlogic.gdx.ai.pfa.DefaultGraphPath
import com.badlogic.gdx.ai.pfa.GraphPath
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import ktx.math.component1
import ktx.math.component2

data class TileCell(val cell: Cell, val pos: Vector2, val index: Int) {
    fun bl() = pos
    fun tl() = Vector2(pos.x, pos.y + 1f)
    fun tr() = Vector2(pos.x + 1, pos.y + 1)
    fun br() = Vector2(pos.x + 1, pos.y)
    fun edges() = listOf(
            Edge(bl(), tl()),
            Edge(tl(), tr()),
            Edge(tr(), br()),
            Edge(br(), bl())
    )
}

data class TileCellConnection(val source: TileCell,
                              val sink: TileCell) : Connection<TileCell> {
    override fun getToNode() = sink

    override fun getCost() = 1f

    override fun getFromNode() = source
}

data class Edge(val p1: Vector2, val p2: Vector2)

class MapManager(val map: TiledMap) : IndexedGraph<TileCell> {

    private val allCells: List<TileCell>
    private val adjacencyList: Map<TileCell, List<TileCellConnection>>
    private val cellsCount: Int
    val opaqueEdges: List<Edge>

    init {
        allCells = getCells()
        adjacencyList = allCells
                .let { cells -> cellConnections(cells) }
                .let { conns -> cellAdjacencyList(conns) }
        cellsCount = allCells.size
        opaqueEdges = getOpaqueEdges(allCells)
    }


    override fun getConnections(fromNode: TileCell): Array<Connection<TileCell>> {
        return adjacencyList[fromNode]
                ?.let { Array<Connection<TileCell>>(it.toTypedArray()) }
                ?: Array(0)
    }

    override fun getIndex(node: TileCell?): Int {
        return node!!.index
    }

    override fun getNodeCount(): Int {
        return cellsCount
    }

    private fun getOpaqueEdges(tiles: List<TileCell>) =
            tiles
                    .filter { isOpaqueCell(it) }
                    .let { getTilesInAdjacentGroups(it) }
                    .map { getEdgesFromTileGroup(it) }
                    .flatten()


    // TODO OPTIMIZE>>>>
    fun findPath(startPos: Vector2, endPos: Vector2): GraphPath<TileCell> {
        val flooredVec = { vec: Vector2 -> Vector2(vec.x.toInt().toFloat(), vec.y.toInt().toFloat()) }
        val start = allCells.find { it.pos == flooredVec(startPos) }
        val end = allCells.find { it.pos == flooredVec(endPos) }
        val graph = DefaultGraphPath<TileCell>()
        IndexedAStarPathFinder(this).searchNodePath(start, end,
                { a, b ->
                    Vector2.dst(a.pos.x + 0.5f, a.pos.y + 0.5f, b.pos.x + 0.5f, b.pos.y + 0.5f)
                }
                , graph)
        return graph
    }

    fun cellAdjacencyList(connections: List<TileCellConnection>): Map<TileCell, List<TileCellConnection>> {
        return mutableMapOf<TileCell, MutableList<TileCellConnection>>().apply {
            connections.forEach { conn ->
                if (!containsKey(conn.source)) {
                    put(conn.source, mutableListOf())
                }
                get(conn.source)!!.add(conn)
            }
        }
    }

    fun cellConnections(
            cells: List<TileCell>): List<TileCellConnection> {
        var idx = 0
        val positions = cells
                .filter { !isImpassableCell(it) }
                .map { it.pos to it }
                .toMap()
        return mutableListOf<TileCellConnection>().apply {
            val neighbors = { cell: TileCell ->
                val (x, y) = cell.pos
                listOf(
                        positions[Vector2(x + 1, y)],
                        positions[Vector2(x + 1, y + 1)],
                        positions[Vector2(x + 1, y - 1)],
                        positions[Vector2(x, y + 1)],
                        positions[Vector2(x - 1, y)],
                        positions[Vector2(x - 1, y + 1)],
                        positions[Vector2(x - 1, y - 1)],
                        positions[Vector2(x, y - 1)]
                )
                        .filterNotNull()
                        .filter { c ->
                            !isImpassableCell(c)
                        }
                        .map {
                            TileCellConnection(cell, it).also {
                                idx++
                            }
                        }
            }

            cells.map { addAll(neighbors(it)) }
        }
    }

    fun getCells(): List<TileCell> {
        val collisionLayer = map.layers.first() as TiledMapTileLayer
        return collisionLayer
                .let {
                    val width = it.width
                    val height = it.height
                    mutableListOf<TileCell>().apply {
                        var index = 0
                        for (i in 0 until width) {
                            for (j in 0 until height) {
                                val c: Cell? = it.getCell(i, j)
                                if (c != null) {
                                    add(TileCell(c, Vector2(i.toFloat(), j.toFloat()), index)).also { index++ }
                                }
                            }
                        }
                    }
                }
    }


    // TODO: !!!! cell.tile.properties.containsKey just throws nullpointerexceptions
    // for some reason. We need to iterate over these cells and build our own
    // data structure mapping a cells position to it's data
    //
    // It's a future thing cause we currently don't need it NOW to implement path
    // finding. But once we get an MVP of pathfinding and stuff in, we should
    // remedy how we treat map data.

    companion object {
        val impassablePropertyName = "impassable"

        // TODO: change to opaque when I update the tileset
        val opaquePropertyName = "impassable"

        fun isImpassableCell(cell: TileCell) = hasProperty(cell.cell, impassablePropertyName)
        fun isOpaqueCell(cell: TileCell) = hasProperty(cell.cell, opaquePropertyName)
        fun hasProperty(cell: TiledMapTileLayer.Cell, property: String) =
                cell.tile.properties.keys.let {
                    mutableSetOf<String>().apply {
                        it.forEach {
                            add(it)
                        }
                    }
                }.contains(property)


        fun getVertCounts(tiles: List<Pair<Float, Float>>): Map<Pair<Float, Float>, Int> {
            return mutableMapOf<Pair<Float, Float>, Int>().let { vertToCount ->
                tiles.map {
                    if (!vertToCount.contains(it)) {
                        vertToCount.put(it, 1)
                    } else {
                        vertToCount[it] = vertToCount[it]!! + 1
                    }
                }
                vertToCount
            }
        }

        fun getCornersFromVertCounts(vertsToCounts: Map<Pair<Float, Float>, Int>): Set<Pair<Float, Float>> =
                vertsToCounts
                        .map {
                            when (it.value) {
                                1 -> it.key // 1 tile had this vert, so it's an exterior corner
                                2 -> null // 2 tiles had this vert, so it's the side of an edge
                                3 -> it.key // 3 tiles had this vert, so it's a convex corner
                                4 -> null // 4 tiles had this vert, so it's an interior vert and not part of an edge
                                else -> throw Error("Unexpected number of overlaps ${it.value}")
                            }
                        }
                        .filterNotNull()
                        .toSet()


        fun getVertsForTilePos(pos: Vector2): List<Pair<Float, Float>> {
            return listOf(
                    pos.x to pos.y, // Bottom Left
                    pos.x + 1 to pos.y, // Bottom Right
                    pos.x to pos.y + 1, // Top Left
                    pos.x + 1 to pos.y + 1 // Top Right
            )
        }

        fun dedupeEdges(edges: List<Edge>): List<Edge> {
            val posToEdge =
                    mutableMapOf<String, Edge>()
            val edgeStr =
                    { edge: Edge -> "${edge.p1.x}-${edge.p1.y}-${edge.p2.x}-${edge.p2.y}" }
            edges.forEach { e ->
                val key = edgeStr(e)
                val inverseKey = edgeStr(Edge(e.p2, e.p1))
                if (!(posToEdge.containsKey(key) || posToEdge.containsKey(inverseKey))) {
                    posToEdge[key] = e
                }
            }
            return posToEdge.values.toList()
        }

        fun getEdgesFromCornersAndVerts(corners: Set<Pair<Float, Float>>,
                                        verts: Set<Pair<Float, Float>>): List<Edge> {

            val up = Pair(0f, 1f)
            val right = Pair(1f, 0f)
            val down = Pair(0f, -1f)
            val left = (Pair(-1f, 0f))
            return corners
                    .map {
                        listOf(
                                walkDirection(it, up, corners, verts),
                                walkDirection(it, right, corners, verts),
                                walkDirection(it, down, corners, verts),
                                walkDirection(it, left, corners, verts)
                        )
                                .filterNotNull()
                    }
                    .flatten()
                    .let { dedupeEdges(it) }

        }

        fun walkDirection(start: Pair<Float, Float>, direction: Pair<Float, Float>, corners: Set<Pair<Float, Float>>,
                          verts: Set<Pair<Float, Float>>): Edge? {

            val nextVert = { cur: Pair<Float, Float> ->
                Pair(cur.first + direction.first, cur.second + direction.second)
            }
            var curVert = start
            while (true) {
                val next = nextVert(curVert)
                // Sanity Check
                if (next == start) throw Error("How did the start and next become equal?")
                if (corners.contains(next)) {
                    return Edge(Vector2(start.first, start.second), Vector2(next.first, next.second))
                } else if (!verts.contains(next)) {
                    return null // Hit a terminating side
                } else {
                    curVert = next
                }
            }

        }

        fun getEdgesFromTileGroup(tiles: List<TileCell>): List<Edge> =
                tiles
                        .map { getVertsForTilePos(it.pos) }
                        .flatten()
                        .let { getVertCounts(it) }
                        .let { vertsToCounts ->
                            val corners = getCornersFromVertCounts(vertsToCounts)
                            val verts = vertsToCounts.keys
                            getEdgesFromCornersAndVerts(corners, verts)
                        }


        fun getTilesInAdjacentGroups(tiles: List<TileCell>): List<List<TileCell>> {
            val posToTile =
                    tiles
                            .map { Pair(it.pos.x.toInt(), it.pos.y.toInt()) to it }
                            .toMap()
            val maxX = tiles.maxByOrNull { it.pos.x }!!.pos.x.toInt()
            val maxY = tiles.maxByOrNull { it.pos.y }!!.pos.y.toInt()

            var curGroup = 0

            val posToGroup = mutableMapOf<Pair<Int, Int>, Int>()

            for (i in 0..maxX) {
                for (j in 0..maxY) {

                    val curPos = Pair(i, j)
                    if (!posToTile.containsKey(curPos)) {
                        continue
                    }

                    val group = if (posToGroup.containsKey(curPos)) {
                        posToGroup[curPos]!!
                    } else {
                        curGroup++
                        posToGroup[curPos] = curGroup
                        curGroup
                    }

                    val bottom = Pair(curPos.first, curPos.second + 1)
                    if (posToTile.contains(bottom) && !posToGroup.containsKey(bottom)) {
                        posToGroup[bottom] = group
                    }

                    val right = Pair(curPos.first + 1, curPos.second)
                    if (posToTile.contains(right) && !posToGroup.containsKey(right)) {
                        posToGroup[right] = group
                    }
                }
            }

            return mutableMapOf<Int, MutableList<TileCell>>().apply {
                posToGroup.values.forEach { group ->
                    this[group] = mutableListOf()
                }
                posToGroup.forEach { pos, group ->
                    posToTile[pos]?.let { tile ->
                        this[group]!!.add(tile)
                    }
                }
            }.values.toList()


        }

    }


    fun getImpassableCells() = getCells()
            .filter {
                isImpassableCell(it)
            }

    fun impassableCellPositions(): Set<Vector2> {
        return getImpassableCells()
                .map { (_, pos) ->
                    val (x, y) = pos
                    // In meters, not pixels
                    Vector2(x, y)

                }.toSet()

    }

}
