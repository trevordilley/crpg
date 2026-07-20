package com.ancient.game.crpg.battle.systems


import com.ancient.game.crpg.*
import com.ancient.game.crpg.systems.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.one
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Rectangle
import ktx.ashley.get
import ktx.ashley.mapperFor

sealed class DropZoneKind
object TreasureKind : DropZoneKind()
object HealingKind : DropZoneKind()

class CDropZone(
    val kind: DropZoneKind,
    val bounds: Rectangle
) : Component {
    companion object {
        fun m() = mapperFor<CDropZone>()
    }
}

class DropZoneSystem(private val selectionSystem: SelectionSystem) : IteratingSystem(one(
        CDropZone::class.java, CHaulable::class.java).get()) {

    private val log = gameLogger(this::class.java)

    /** Running total of loot delivered to a treasure drop zone. */
    var claimedValue: Int = 0
        private set

    private val dropZones = mutableListOf<Entity>()
    private val haulables = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[CDropZone.m()]?.let { dropZones.add(entity) }
        entity[CHaulable.m()]?.let { haulables.add(entity) }
    }

    override fun update(deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        super.update(dt)
        haulables.forEach { h ->
            val pos = h[CTransform.m()]!!.position
            dropZones.forEach { dzEnt ->
                val dz = dzEnt[CDropZone.m()]!!
                if (dz.bounds.contains(pos)) {
                    when (dz.kind) {
                        is TreasureKind -> {
                            h[CTreasure.m()]?.let { treasure ->
                                // Null-safe: claiming loot must not depend on the
                                // drop zone having an animation to play. The !!
                                // here would NPE on any zone without CAnimated.
                                dzEnt[CAnimated.m()]?.anims?.values?.firstOrNull()?.let { anim ->
                                    anim.setAnimation<OnDropAnimation>(OnAnimationEnd to {
                                        anim.setAnimation<IdleAnimation>()
                                    })
                                }

                                selectionSystem.deselect(h)
                                this.engine.removeEntity(h)
                                claimedValue += treasure.value
                                log.info("Claimed some loot worth ${treasure.value}")
                            }
                        }
                        is HealingKind -> {
                            h[CDead.m()]?.let {
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
