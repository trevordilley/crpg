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


class CCombatant(val combatant: Combatant, val equipment: Equipment) : Component

class CombatantSystem : IteratingSystem(all(CCombatant::class.java).get()) {
    private val transformMapper: ComponentMapper<CTransform> = mapperFor()
    private val movableMapper: ComponentMapper<CMovable> = mapperFor()
    private val combatantMapper: ComponentMapper<CCombatant> = mapperFor()
    private val actionMapper: ComponentMapper<CAction> = mapperFor()
    private val animatedMapper: ComponentMapper<CAnimated> = mapperFor()
    private val entitiesToProcess = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entitiesToProcess.add(entity)
    }

    override fun update(deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)

        val (players, enemies) =
                entities.partition { it[combatantMapper]!!.combatant is Player }

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
        val (x, y) = attacker[transformMapper]!!.position
        val target = targets.sortedBy {
            val (tx, ty) = it[transformMapper]!!.position
            Vector2.dst(x, y, tx, ty)
        }.firstOrNull() ?: return
        val (tx, ty) = target[transformMapper]!!.position
        val combatant = attacker[combatantMapper]!!.combatant
        val gear = attacker[combatantMapper]!!.equipment
        val weapon: MeleeWeapon = listOf(gear.rightHand,
                gear.leftHand).first { it is MeleeWeapon } as MeleeWeapon
        Vector2.dst(x, y, tx, ty).let { distance ->
            if (distance <= weapon.range) {

                attacker[animatedMapper]?.setAnimation<AttackAnimation>()
                attacker[animatedMapper]?.addAction(9) {
                    BattleActionEffectSystem.applyEffect(engine,
                            CMeleeEffect(attacker, target, weapon.range, weapon.staminaDamage))

                    attacker[animatedMapper]?.setAnimation<IdleAnimation>()
                }
//                attacker[actionMapper] ?: attacker.add(
//
//                        // This is where we can figure out how to animate/make pretty
//                        // melee attacks. Moving the character around is proving to be a bad idea
//                        // cause it offsets their position and more importantly THEIR ROTATION (you
//                        // can get hit in the back!!! insta-kill!!)
//                        //
//                        // So we should look for a more mature solution, but this is a step closer to
//                        // solving how to animate + damage an entity.
//                        CAction { dt, duration ->
//
//
//
//
//                            val passed = duration / weapon.duration
//                            if (passed >= 1.0f) {
//                                true
//                            } else {
//
//
//                                val t = when (passed) {
//                                    in 0.2f..0.5f -> (passed / 1.0f) * 1.2f
//                                    in 0.5f..1f -> (passed / 1.0f) * 1.1f
//                                    else -> 0f
//                                }
//                                attacker[transformMapper]!!.position = quadraticBezier(Vector2(x, y), Vector2(tx, ty),
//                                        Vector2(x, y), t)
//
//                                if (passed >= 0.5 && !effectApplied) {
//                                    BattleActionEffectSystem.applyEffect(engine,
//                                            CMeleeEffect(attacker, target, weapon.range, weapon.staminaDamage))
//                                    effectApplied = true
//                                }
//                                false
//                            }
//                        }
//                )
                attacker[movableMapper]!!.destination = null
            } else {
                when (combatant) {
                    is Enemy -> {
                        if (distance <= combatant.aggroRange) {
                            attacker[movableMapper]!!.destination = Vector2(tx, ty)
                        }
                    }
                }
            }

        }

    }

}
