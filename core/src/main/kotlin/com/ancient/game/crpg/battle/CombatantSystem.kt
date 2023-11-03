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

class CCombatant(val combatant: Combatant, val equipment: Equipment) : Component {
    companion object {
        fun m() = mapperFor<CCombatant>()
    }
}

class CombatantSystem : IteratingSystem(all(CCombatant::class.java).exclude(CDead::class.java).get()) {
    private val entitiesToProcess = mutableListOf<Entity>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entitiesToProcess.add(entity)
    }


    override fun update(deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        val (players, enemies) =
                entities.partition { it[CCombatant.m()]!!.combatant is Player }

        if (players.isEmpty()) {
            entitiesToProcess.clear()
            return
        }

        players.forEach { attackNearby(it, enemies) }
        enemies.forEach { attackNearby(it, players) }

        entitiesToProcess.clear()
        super.update(dt)
    }


    private fun attackNearby(attacker: Entity, targets: List<Entity>) {
        val (x, y) = attacker[CTransform.m()]!!.position
        val target =
                targets.minByOrNull {
                    val (tx, ty) = it[CTransform.m()]!!.position
                    Vector2.dst(x, y, tx, ty)
                } ?: return

        val (tx, ty) = target[CTransform.m()]!!.position
        val combatant = attacker[CCombatant.m()]!!.combatant
        val gear = attacker[CCombatant.m()]!!.equipment
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
                                attacker[CAnimated.m()]!!.anims.values.first()
                        if (atk.currentAnimationState !is AttackAnimation) {
                            atk.setAnimation<AttackAnimation>(
                                    OnTag("Damage") to {
                                        target[CTransform.m()]?.let { tarPos ->
                                            target[CHealth.m()]?.let { health ->
                                                attacker[CTransform.m()]?.let { attackerPos ->
                                                    val dist =
                                                            Vector2.dst(
                                                                    attackerPos.position.x,
                                                                    attackerPos.position.y,
                                                                    tarPos.position.x,
                                                                    tarPos.position.y
                                                            )
                                                    if (dist <= weapon.range) {
                                                        health.damages.add(Damage(weapon.staminaDamage, attackerPos.position))
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    OnAnimationEnd to {
                                        atk.setAnimation<IdleAnimation>()
                                    }
                            )
                        }
                        attacker[CMovable.m()]!!.destination = null
                    } else {
                        when (combatant) {
                            is Enemy -> {
                                if (distance <= combatant.aggroRange) {
                                    attacker[CMovable.m()]!!.destination = Vector2(tx, ty)
                                }
                            }
                        }
                    }
                }
    }
}
