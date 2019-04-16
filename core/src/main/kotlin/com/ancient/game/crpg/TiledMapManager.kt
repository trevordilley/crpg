package com.ancient.game.crpg

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.math.Vector2
import ktx.math.component1
import ktx.math.component2

class TiledMapManager(val map: TiledMap, val unitScale: Float) {

    val impassablePropertyName = "impassable"

    fun collisionPoints(): Set<Vector2> {


        val collisionLayer = map.layers.first() as TiledMapTileLayer
        return collisionLayer
                .let {
                    val width = it.width
                    val height = it.height
                    mutableListOf<Pair<TiledMapTileLayer.Cell, Vector2>>().apply {
                        for (i in 0 until width) {
                            for (j in 0 until height) {
                                val c: TiledMapTileLayer.Cell? = it.getCell(i, j)
                                if (c != null) {
                                    add(c to Vector2(i.toFloat(), j.toFloat()))
                                }
                            }
                        }
                    }
                }
                .filter {
                    it.first.tile.properties?.containsKey(impassablePropertyName) ?: false
                }
                .map { (_, pos) ->
                    val (x, y) = pos
                    // In meters, not pixels
                    listOf(
                            Vector2(x, y),
                            Vector2(x, y + 1),
                            Vector2(x + 1, y + 1),
                            Vector2(x + 1, y)
                    )

                }
                .flatten()
                .let { verts ->
                    // Probably easier kotlin magic I could do to group these by their
                    // counts
                    mutableMapOf<String, MutableList<Vector2>>()
                            .apply {
                                verts.forEach { v ->
                                    val (x, y) = v
                                    val key = "$x$y"
                                    if (!containsKey(key)) {
                                        put(key, mutableListOf())
                                    }
                                    get(key)!!.add(v)
                                }
                            }
                            .let { it.values }
                            .map { Pair(it.first(), it.size) }
                }
                .filter { (_, count) ->
                    count != 4
                }
                .map { it.first }
                .toSet()

    }

}