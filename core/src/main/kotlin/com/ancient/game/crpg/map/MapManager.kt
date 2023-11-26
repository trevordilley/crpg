package com.ancient.game.crpg.map

import com.badlogic.gdx.ai.pfa.*
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntArray
import games.rednblack.editor.renderer.SceneLoader
import games.rednblack.editor.renderer.data.MainItemVO
import games.rednblack.editor.renderer.data.SimpleImageVO
import ktx.collections.gdxArrayOf
import ktx.collections.gdxIntArrayOf
import ktx.math.component1
import ktx.math.component2


data class PathNode(val x: Int, val y: Int)
data class PathNodeConnection(
    val source: PathNode,
    val sink: PathNode,
) : Connection<PathNode> {
    override fun getToNode() = sink

    override fun getCost() =  1f

    override fun getFromNode() = source
}

data class Edge(val p1: Vector2, val p2: Vector2)

class MapManager(private val sceneLoader: SceneLoader, private val worldWidth: Int, private val worldHeight: Int) : IndexedGraph<PathNode> {
    val spawnPoints = sceneLoader.sceneVO.composite.content.get("games.rednblack.editor.renderer.data.LabelVO")
    val partySpawns = spawnPoints.filter {it.itemIdentifier == "PARTY_SPAWN"}
    val treasureSpawns = spawnPoints.filter { it.itemIdentifier == "TREASURE_SPAWN"}
    val cartSpawn = spawnPoints.filter {it.itemIdentifier == "CART_SPAWN" }
    val healerSpawn = spawnPoints.filter {it.itemIdentifier == "HEALER_SPAWN"}
    val enemySpawns = spawnPoints.filter {it.itemIdentifier == "ENEMY_SPAWN"}

    private val spacingBetweenPathNodes = 10 // in px
    private val backgroundImage: MainItemVO =
        sceneLoader.sceneVO.composite.content.get("games.rednblack.editor.renderer.data.SimpleImageVO")
            .first { it.itemIdentifier == "BACKGROUND" }
    private val backgroundPosition = Vector2(backgroundImage.x, backgroundImage.y)
    private val polys =
        sceneLoader.sceneVO.composite.content.get("games.rednblack.editor.renderer.data.ColorPrimitiveVO")
    val collision = polys.filter { it.itemIdentifier == "COLLISION" }.map {
        it.shape.vertices.map { v -> listOf(v.x + it.x, v.y + it.y) }.flatten()
    }
        .map {
            Polygon(it.toFloatArray())
        }
    val navMesh = collision.let { colliders ->
        val deriveNavPoint = { target: Vector2, before: Vector2, after: Vector2, center: Vector2, radiusToFit: Float ->
           println("todo")
           Vector2(0f,0f)
        }

        for(collider in colliders) {
            // TODO: need to pair these verts up
            for(i in 0 until collider.vertexCount) {
            }
        }
    }

    val occluders = polys
        .filter { it.itemIdentifier == "OCCLUDER" }
        .map { it.shape.vertices.map { v -> Vector2(v.x + it.x, v.y + it.y) } }
        .map { poly ->
            poly.withIndex().map { (i, v) ->

                // Perhaps a bit to clever, if i+1 is out of bounds then
                // we've circled back, so connect the last vert with the first vert.
                //
                // I suppose I could put some kind of assert here? Like... if i is
                // out of bounds by more than 1? Meh... _it'll be fine..._
                val nxt = poly.getOrElse(i + 1) { poly[0] }
                Edge(v, nxt)
            }
        }.toMutableList().apply {

            val bl = Vector2(backgroundPosition.x, backgroundPosition.y)
            val br = Vector2(worldWidth.toFloat() - backgroundPosition.x, backgroundPosition.y)
            val tr = Vector2(worldWidth.toFloat() - backgroundPosition.x, worldHeight.toFloat() - backgroundPosition.y)
            val tl = Vector2(backgroundPosition.x, worldHeight.toFloat() - backgroundPosition.y)
            add(
                listOf(
                    Edge(bl, br),
                    Edge(br, tr),
                    Edge(tr, tl),
                    Edge(tl, bl),
                )
            )
        }.toList()


    // NEW APPROACH: Use collision polygon verts as nav nodes
    // At level load, grab each vert from collision
    // offset those verts by the radius of the various size creatures we'll support
    // (64px, 128px, 256px)
    // The offset is a function of the angle (if acute, needs to be further out) and
    // the creature radius
    //
    // Once we've calc'd those offsets, for each vert connect them if they have
    // unobstructed line-of-site
    //
    // That's our A* graph

    private val numRows = worldHeight / spacingBetweenPathNodes
    private val numCols = worldWidth / spacingBetweenPathNodes
    val pathNodes =
        gdxArrayOf<IntArray>(true, numRows).apply{
            var idx = 0
            for(x in 0 ..<numCols) {
                val arr = gdxIntArrayOf()
                for (y in 0..<numRows) {
                    val pos = Vector2(x * spacingBetweenPathNodes * 1f, y * spacingBetweenPathNodes * 1f)
                    val collides = collision.firstOrNull() { it.contains(pos)}
                    idx++
                    // Less than 0 means collision, packing to bits of data in one
                    // Not writing A* ourself means using the IndexedAStarPathFinder, which expects
                    // some kind of index function and this is the easiest way to save the index
                    // in an array AND communicate the path cost
                    arr.add(if(collides == null) idx else idx * -1)
                }
                add(arr)
            }
    }


    // Built from the LoS connections between collision verts
    override fun getConnections(fromNode: PathNode): Array<Connection<PathNode>> {
        val (x,y) = fromNode
        val t = PathNode(x, y + 1)
        val tl = PathNode(x - 1,y + 1)
        val l = PathNode(x - 1, y)
        val bl = PathNode(x - 1, y - 1)
        val b = PathNode(x, y - 1)
        val br = PathNode(x + 1, y - 1)
        val r = PathNode(x + 1, y)
        val tr = PathNode(x + 1, y + 1)



        return Array<Connection<PathNode>>().apply {
            val connect = { node: PathNode ->
                // Only connect nodes that aren't collision
                if(pathNodes[node.x/10][node.y/10] >= 0) {
                    add(PathNodeConnection(fromNode, node))
                }
            }

            connect(t)
            connect(tl)
            connect(l)
            connect(bl)
            connect(b)
            connect(br)
            connect(r)
            connect(tr)
        }
    }





    // TODO OPTIMIZE>>>>
    fun findPath(startPos: Vector2, endPos: Vector2): GraphPath<PathNode> {
        val graph = DefaultGraphPath<PathNode>()
        val i = IndexedAStarPathFinder(this, true)
            i.searchNodePath(PathNode(startPos.x.toInt()/10,startPos.y.toInt()/10), PathNode(endPos.x.toInt()/10, endPos.y.toInt()/10),
            { a, b ->
                Vector2.dst(a.x.toFloat() , a.y.toFloat() , endPos.x.toFloat() , endPos.y.toFloat())
            }, graph)
        i.metrics
        return graph
    }

    override fun getIndex(node: PathNode): Int {
        val x = node.x/10
        val y = node.y/10
        return pathNodes[x][y]
    }
    override fun getNodeCount(): Int = numRows * numCols

}
