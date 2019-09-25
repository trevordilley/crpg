package com.ancient.game.crpg.map

import com.ancient.game.crpg.map.MapManager.Companion.dedupeEdges
import com.ancient.game.crpg.map.MapManager.Companion.getCornersFromVertCounts
import com.ancient.game.crpg.map.MapManager.Companion.getEdgesFromCornersAndVerts
import com.ancient.game.crpg.map.MapManager.Companion.getVertCounts
import com.ancient.game.crpg.map.MapManager.Companion.getVertsForTilePos
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.math.Vector2
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitQuickcheck::class)
internal class MapManagerTest {

    val newTile = { x: Float, y: Float, i: Int ->
        TileCell(TiledMapTileLayer.Cell(), Vector2(x, y), i)
    }


    @Test
    fun `3 groups containing the right cells`() {
        val tiles = mapOf(
                1 to listOf(newTile(0f, 0f, 0),
                        newTile(0f, 1f, 1)),

                2 to listOf(
                        newTile(10f, 0f, 0),
                        newTile(10f, 1f, 1),
                        newTile(11f, 0f, 0),
                        newTile(11f, 1f, 1),
                        newTile(12f, 0f, 0),
                        newTile(12f, 1f, 0)),

                3 to listOf(
                        newTile(2f, 0f, 0),
                        newTile(2f, 1f, 1),
                        newTile(3f, 0f, 0),
                        newTile(3f, 1f, 1),
                        newTile(4f, 0f, 0),
                        newTile(4f, 1f, 1))
        )

        MapManager
                .getTilesInAdjacentGroups(tiles.values.flatten())
                .map { it.map { t -> Pair(t.pos.x, t.pos.y) } }
                .forEach { println(it) }
    }

    @Test
    fun `vert overlap counts from tiles`() {
        val tiles = listOf(
                Vector2(0f, 0f),
                Vector2(1f, 0f),
                Vector2(0f, 1f),
                Vector2(0f, 2f),
                Vector2(1f, 1f)
        )
        tiles
                .map { getVertsForTilePos(it) }
                .flatten()
                .let { verts -> getVertCounts(verts) }
                .also { println(it) }
    }

    @Test
    fun `can dedupe bidirectional edges`() {
        val edges = listOf(
                Edge(Vector2(0f, 0f), Vector2(2f, 2f)),
                Edge(Vector2(2f, 2f), Vector2(0f, 0f))
        )
        println(dedupeEdges(edges))
    }


    @Test
    fun `walking corners in direction`() {
        val tiles = listOf(
                Vector2(0f, 0f)
        )
        tiles
                .map { getVertsForTilePos(it) }
                .flatten()
                .let { overlappedVerts ->
                    val counts = getVertCounts(overlappedVerts)
                    val corners = getCornersFromVertCounts(counts)
                    val verts = counts.keys
                    getEdgesFromCornersAndVerts(corners, verts).also { println(it) }
                }
    }


}
