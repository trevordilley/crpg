package com.ancient.game.crpg.battle


import com.ancient.game.crpg.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.one
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Rectangle
import ktx.ashley.get
import ktx.ashley.mapperFor

sealed class DropZoneKind()
object TreasureKind : DropZoneKind()
object HealingKind : DropZoneKind()

class CDropZone(
        val kind: DropZoneKind,
        val bounds: Rectangle
) : Component

class DropZoneSystem(private val selectionSystem: SelectionSystem) : IteratingSystem(one(
        CDropZone::class.java, CHaulable::class.java).get()) {
    private val dropZoneM = mapperFor<CDropZone>()
    private val animatedM = mapperFor<CAnimated>()
    private val haulableM = mapperFor<CHaulable>()
    private val transformM = mapperFor<CTransform>()
    private val treasureM = mapperFor<CTreasure>()
    private val deadM = mapperFor<CDead>()

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
                    when (dz.kind) {
                        is TreasureKind -> {
                            h[treasureM]?.let {
                                val anim = dzEnt[animatedM]!!.anims.values.first()
                                anim.setAnimation<OnDropAnimation>(OnAnimationEnd to {
                                    anim.setAnimation<IdleAnimation>()
                                })

                                selectionSystem.deselect(h)
                                this.engine.removeEntity(h)
                                println("Claimed some loot worth ${it.value}!!!")
                            }
                        }
                        is HealingKind -> {
                            h[deadM]?.let {
                                it.beingHealed = true
                            }
                        }
                    }
                }
            }
        }

        // TODO: DropZones accept different kinds of haulables with
        // different actions based on Kind (Loot, NeedsHealing, etc)
        dropZones.clear()
        haulables.clear()
    }

}
