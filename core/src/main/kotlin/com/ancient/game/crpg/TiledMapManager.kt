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
                    Vector2(x, y)

                }.toSet()

    }

}