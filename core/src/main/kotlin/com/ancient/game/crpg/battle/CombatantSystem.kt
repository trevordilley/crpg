package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.equipment.Equipment
import com.ancient.game.crpg.equipment.MeleeWeapon
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.math.component1
import ktx.math.component2

sealed class Combatant
object Player : Combatant()
data class Enemy(val aggroRange: Float) : Combatant()

class CCombatant(val combatant: Combatant, val equipment: Equipment, var curCooldown: Float = 0f, val maxCooldown: Float = 1f) : Component

class CombatantSystem : IteratingSystem(all(CCombatant::class.java).get()) {
    private val transformM: ComponentMapper<CTransform> = mapperFor()
    private val movableM: ComponentMapper<CMovable> = mapperFor()
    private val combatantM: ComponentMapper<CCombatant> = mapperFor()
    private val animatedM: ComponentMapper<CAnimated> = mapperFor()
    private val healthM: ComponentMapper<CHealth> = mapperFor()
    private val entitiesToProcess = mutableListOf<Entity>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entitiesToProcess.add(entity)
    }


    override fun update(deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        val (players, enemies) =
                entities.partition { it[combatantM]!!.combatant is Player }

        if (players.isEmpty()) {
            entitiesToProcess.clear()
            return
        }

        entitiesToProcess.forEach { it[combatantM]!!.curCooldown += dt }

        players.forEach { attackNearby(it, enemies) }
        enemies.forEach { attackNearby(it, players) }

        entitiesToProcess.clear()
        super.update(dt)
    }


    private fun attackNearby(attacker: Entity, targets: List<Entity>) {
        println(attacker[combatantM]!!.curCooldown)
        val (x, y) = attacker[transformM]!!.position
        val target =
                targets.minBy {
                    val (tx, ty) = it[transformM]!!.position
                    Vector2.dst(x, y, tx, ty)
                } ?: return

        val (tx, ty) = target[transformM]!!.position
        val combatant = attacker[combatantM]!!.combatant
        val gear = attacker[combatantM]!!.equipment
        val weapon: MeleeWeapon =
                listOf(
                        gear.rightHand,
                        gear.leftHand
                ).first { it is MeleeWeapon } as MeleeWeapon

        Vector2
                .dst(x, y, tx, ty)
                .let { distance ->
                    if (distance <= weapon.range) {
                        val atk =
                                attacker[animatedM]!!.anims.values.first()
                        if (atk.currentAnimationState !is AttackAnimation) {
                            atk.setAnimation<AttackAnimation>(
                                    OnTag("Damage") to {
                                        target[transformM]?.let { tarPos ->
                                            target[healthM]?.let { health ->
                                                attacker[transformM]?.let { attackerPos ->
                                                    val dist =
                                                            Vector2.dst(
                                                                    attackerPos.position.x,
                                                                    attackerPos.position.y,
                                                                    tarPos.position.x,
                                                                    tarPos.position.y
                                                            )
                                                    if (dist <= weapon.range) {
                                                        health.damages.add(Damage(weapon.staminaDamage, attackerPos.position))
                                                        println("Applying ${health.damages.size} damages for a total of ${health.damages.sumBy { it.stamina }} damage against target with health of ${health.stamina}")
                                                    }
                                                }
                                            }
                                        }
                                        attacker[combatantM]!!.curCooldown = 0f
                                    },
                                    OnAnimationEnd to {
                                        atk.setAnimation<IdleAnimation>()
                                    }
                            )
                        }
                        attacker[movableM]!!.destination = null
                    } else {
                        when (combatant) {
                            is Enemy -> {
                                if (distance <= combatant.aggroRange) {
                                    attacker[movableM]!!.destination = Vector2(tx, ty)
                                }
                            }
                        }
                    }
                }
    }
}
