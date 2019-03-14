package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Batch
import ktx.ashley.get
import ktx.ashley.mapperFor


class RenderSystem(val batch: Batch) : IteratingSystem(
        all(Sprite::class.java).get()) {

    private val spriteMapper: ComponentMapper<Sprite> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {

        batch.begin()
        val sprite = entity[spriteMapper]!!
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(sprite.r.texture, 0f, 0f)
        batch.end()
    }

}