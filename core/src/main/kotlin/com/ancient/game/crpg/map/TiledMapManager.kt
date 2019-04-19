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

data class TileCell(val cell: Cell, val pos: Vector2, val index: Int)

data class TileCellConnection(val source: TileCell,
                              val sink: TileCell) : Connection<TileCell> {
    override fun getToNode() = sink

    override fun getCost() = 1f

    override fun getFromNode() = source
}

class TiledMapManager(val map: TiledMap, val unitScale: Float) : IndexedGraph<TileCell> {

    private val allCells: List<TileCell>
    private val adjacencyList: Map<TileCell, List<TileCellConnection>>
    private val cellsCount: Int

    init {
        allCells = getCells()
        adjacencyList = allCells
                .let { cells -> cellConnections(cells) }
                .let { conns -> cellAdjacencyList(conns) }
        cellsCount = allCells.size
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


    // TODO OPTIMIZE>>>>
    fun findPath(start: Vector2, end: Vector2): GraphPath<TileCell> {
        val start = allCells.find { it.pos == start }
        val end = allCells.find { it.pos == end }
        val graph = DefaultGraphPath<TileCell>()
        IndexedAStarPathFinder(this).searchNodePath(start, end,
                { a, b -> Vector2.dst(a.pos.x, a.pos.y, b.pos.x, b.pos.y) }, graph)
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
                .filter { !isImpassableCell(it.cell) }
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
                            !isImpassableCell(c.cell)
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
        fun isImpassableCell(cell: TiledMapTileLayer.Cell) =
                cell.tile.properties.keys.let {
                    mutableSetOf<String>().apply {
                        it.forEach {
                            add(it)
                        }
                    }
                }.contains(impassablePropertyName)

    }


    fun getImpassableCells() = getCells()
            .filter {
                isImpassableCell(it.cell)
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