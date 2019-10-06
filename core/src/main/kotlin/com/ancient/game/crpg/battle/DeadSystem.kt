package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.has
import ktx.ashley.mapperFor
import ktx.log.info

class CDead(var deadFor: Double = 0.0, var beingHealed: Boolean = false, var healedFor: Double = 0.0) : Component
class DeadSystem(val haulingSystem: HaulableSystem) : IteratingSystem(all(CDead::class.java).get()) {
    private val log = gameLogger(this::class.java)

    private val timeTillPermaDeath = 20.0f
    private val timeTillRessurection = 5.0f

    private val deadM: ComponentMapper<CDead> = mapperFor()
    private val haulableM: ComponentMapper<CHaulable> = mapperFor()
    private val healthM: ComponentMapper<CHealth> = mapperFor()
    private val animatedM: ComponentMapper<CAnimated> = mapperFor()
    private val selectionM: ComponentMapper<CSelectable> = mapperFor()
    private val playerControlledM: ComponentMapper<CPlayerControlled> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        val dead = entity[deadM]!!

        // They juuuuuuuuust died
        if (dead.deadFor == 0.0) {
            entity[selectionM]?.let {
                entity.add(CHaulable()) // They can now be moved
                entity[selectionM]!!.kind = HaulableSelect
            }
        }
        dead.deadFor += dt

        if (entity[playerControlledM] == null) {
            engine.removeEntity(entity).also {
                info { "Removing enemy immediately $entity." }
            }

        }

        if (dead.deadFor >= timeTillPermaDeath && !dead.beingHealed) {
            entity[haulableM]?.let { haulingSystem.drop(it) }
            engine.removeEntity(entity).also {
                info { "Removing dead critter $entity." }
            }
        }

        if (dead.beingHealed) {
            dead.healedFor += dt
            if (dead.healedFor >= timeTillRessurection) {
                entity.remove(CDead::class.java)
                entity.remove(CHaulable::class.java)
                entity[animatedM]?.anims?.values?.first()?.setAnimation<IdleAnimation>()
                entity[selectionM]?.let {
                    it.kind = CharacterSelect(if (entity.has(playerControlledM)) {
                        Allegiance.PLAYER
                    } else {
                        Allegiance.ENEMY
                    })
                }
                entity[healthM]!!.health = entity[healthM]!!.maxHealth
                entity[healthM]!!.stamina = (entity[healthM]!!.maxStamina.toDouble() * 0.25).toInt()
                println("$entity is back in action with ${entity[healthM]!!.stamina} stamina!")

            }
        }
    }
}
