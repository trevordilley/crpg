package com.ancient.game.crpg.battle.hauling


import com.ancient.game.crpg.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.one
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Rectangle
import ktx.ashley.get
import ktx.ashley.mapperFor

class CDropZone(val bounds: Rectangle,
                val onDrop: (Pair<Entity, CHaulable>) -> Unit) : Component

class DropZoneSystem() : IteratingSystem(one(CDropZone::class.java, CHaulable::class.java).get()) {
    private val dropZoneM = mapperFor<CDropZone>()
    private val animatedM = mapperFor<CAnimated>()
    private val haulableM = mapperFor<CHaulable>()
    private val transformM = mapperFor<CTransform>()
    private val dropZones = mutableListOf<Entity>()
    private val haulables = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[dropZoneM]?.let { dropZones.add(entity) }
        entity[haulableM]?.let { haulables.add(entity) }
    }

    override fun update(deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        super.update(dt)
        haulables.forEach { h ->
            val pos = h[transformM]!!.position
            dropZones.forEach { dzEnt ->
                val dz = dzEnt[dropZoneM]!!
                if (dz.bounds.contains(pos)) {
                    val anim = dzEnt[animatedM]!!.anims.values.first()
                    anim.setAnimation<OnDropAnimation>(OnAnimationEnd to {
                        anim.setAnimation<IdleAnimation>()
                    })
                    dz.onDrop(h to h[haulableM]!!)
                }
            }
        }
        // Get DropZone bounds
        // Check if any haulables intersect
        // Act on intersection

        // TODO: DropZones accept different kinds of haulables with
        // different actions based on Kind (Loot, NeedsHealing, etc)
        dropZones.clear()
        haulables.clear()
    }

}
