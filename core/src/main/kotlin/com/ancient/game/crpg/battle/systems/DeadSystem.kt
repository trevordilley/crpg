package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.*
import com.ancient.game.crpg.systems.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.has
import ktx.ashley.mapperFor
import ktx.log.info

class CDead(
    var deadFor: Double = 0.0,
    var beingHealed: Boolean = false,
    var healedFor: Double = 0.0
) : Component {
    companion object {
        fun m() = mapperFor<CDead>()
    }
}

class DeadSystem(val haulingSystem: HaulableSystem) : IteratingSystem(all(CDead::class.java).get()) {
    private val log = gameLogger(this::class.java)

    private val timeTillPermaDeath = 20.0f
    private val timeTillRessurection = 5.0f


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        val dead = entity[CDead.m()]!!

        // They juuuuuuuuust died
        if (dead.deadFor == 0.0) {
            entity[CSelectable.m()]?.let {
                entity.add(CHaulable()) // They can now be moved
                entity[CSelectable.m()]!!.kind = HaulableSelect
            }
        }
        dead.deadFor += dt

        if (entity[CPlayerControlled.m()] == null) {
            engine.removeEntity(entity).also {
                info { "Removing enemy immediately $entity." }
            }

        }

        if (dead.deadFor >= timeTillPermaDeath && !dead.beingHealed) {
            entity[CHaulable.m()]?.let { haulingSystem.drop(it) }
            engine.removeEntity(entity).also {
                info { "Removing dead critter $entity." }
            }
        }

        if (dead.beingHealed) {
            dead.healedFor += dt
            if (dead.healedFor >= timeTillRessurection) {
                entity.remove(CDead::class.java)
                entity.remove(CHaulable::class.java)
                entity[CAnimated.m()]?.anims?.values?.first()?.setAnimation<IdleAnimation>()
                entity[CSelectable.m()]?.let {
                    it.kind = CharacterSelect(
                        if (entity.has(CPlayerControlled.m())) {
                            Allegiance.PLAYER
                        } else {
                            Allegiance.ENEMY
                        }
                    )
                }
                entity[CHealth.m()]!!.health = entity[CHealth.m()]!!.maxHealth
                entity[CHealth.m()]!!.stamina = (entity[CHealth.m()]!!.maxStamina.toDouble() * 0.25).toInt()
                println("$entity is back in action with ${entity[CHealth.m()]!!.stamina} stamina!")

            }
        }
    }
}
