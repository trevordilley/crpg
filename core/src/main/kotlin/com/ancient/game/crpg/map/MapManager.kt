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
        println("Node: ${node?.pos}")
        return node!!.index
    }

    override fun getNodeCount(): Int {
        return cellsCount
    }

    private fun getOpaqueEdges(tiles: List<TileCell>) =
            tiles
                    .filter { isOpaqueCell(it) }
                    .let { getTilesInAdjacentGroups(it) }
                    .let { it.values.flatten() } // dummy line
                    .map { t -> t.edges() }
                    .flatten()


    // TODO OPTIMIZE>>>>
    fun findPath(startPos: Vector2, endPos: Vector2): GraphPath<TileCell> {
        println("vec2 pos: $startPos, $endPos")
        val flooredVec = { vec: Vector2 -> Vector2(vec.x.toInt().toFloat(), vec.y.toInt().toFloat()) }
        val start = allCells.find { it.pos == flooredVec(startPos) }
        val end = allCells.find { it.pos == flooredVec(endPos) }
        println("cell pos: $start, $end")
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


        fun getTilesInAdjacentGroups(tiles: List<TileCell>): Map<Int, List<TileCell>> {
            val posToTile =
                    tiles
                            .map { Pair(it.pos.x.toInt(), it.pos.y.toInt()) to it }
                            .toMap()
            val maxX = tiles.maxBy { it.pos.x }!!.pos.x.toInt()
            val maxY = tiles.maxBy { it.pos.y }!!.pos.y.toInt()

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
            }


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
