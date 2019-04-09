package com.ancient.game.crpg.battle

import com.ancient.game.crpg.CTransform
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
    private val actionMapper: ComponentMapper<ActionC> = mapperFor()
    private val entitiesToProcess = mutableListOf<Entity>()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entitiesToProcess.add(entity)
    }

    override fun update(deltaTime: Float) {
        val (players, enemies) =
                entities.partition { it[combatantMapper]!!.combatant is Player }

        if (players.isEmpty()) {
            entitiesToProcess.clear()
            return
        }

        players.firstOrNull()?.let { attackNearby(it, enemies) }
        enemies.firstOrNull()?.let { attackNearby(it, players) }

        entitiesToProcess.clear()
        super.update(deltaTime)
    }

    private fun attackNearby(attacker: Entity, targets: List<Entity>) {
        val target = targets.firstOrNull() ?: return
        val (tx, ty) = target[transformMapper]!!.position
        val (x, y) = attacker[transformMapper]!!.position
        val combatant = attacker[combatantMapper]!!.combatant
        val gear = attacker[combatantMapper]!!.equipment
        val weapon: MeleeWeapon = listOf(gear.rightHand,
                gear.leftHand).first { it is MeleeWeapon } as MeleeWeapon
        Vector2.dst(x, y, tx, ty).let { distance ->
            if (distance <= weapon.range) {
                // Start Attack
                attacker[actionMapper] ?: attacker.add(
                        ActionC(0f, weapon.duration,
                                MeleeEffectC(attacker, target, weapon.range, weapon.staminaDamage)
                        )).also { println("Weapon: ${weapon.name}") }
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
