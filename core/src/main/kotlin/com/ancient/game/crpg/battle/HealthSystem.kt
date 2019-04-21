package com.ancient.game.crpg.battle

import com.ancient.game.crpg.CTransform
import com.ancient.game.crpg.UserInputManager
import com.ancient.game.crpg.angleWithinArc
import com.ancient.game.crpg.equipment.Shield
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.math.minus
import kotlin.math.max

// Tenth of a second, recharges 10 points per second at rate of 1
const val STAMINA_RECHARGE_INTERVAL = 0.10f


data class Damage(val stamina: Int, val originPosition: Vector2, val health: Int = 1)

class CHealth(
        var stamina: Int,
        var maxStamina: Int,
        var staminaRechargeRate: Int,
        var health: Int,
        val damages: MutableList<Damage> = mutableListOf(),
        var staminaNotRechargingForSeconds: Float = 0f) : Component

class CDead : Component
class HealthSystem : IteratingSystem(
        all(CHealth::class.java, CTransform::class.java, CCombatant::class.java).exclude(CDead::class.java)
                .get()) {

    private var curTimeTillRecharge = 0f
    private val healthMapper: ComponentMapper<CHealth> = mapperFor()
    private val transformMapper: ComponentMapper<CTransform> = mapperFor()
    private val combatantMapper: ComponentMapper<CCombatant> = mapperFor()


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        entity[healthMapper]!!.let { health ->
            if (health.staminaNotRechargingForSeconds > 0f) {
                health.staminaNotRechargingForSeconds -= dt
            }

            val shouldRechargeStamina =
                    health.stamina < health.maxStamina &&
                            curTimeTillRecharge > STAMINA_RECHARGE_INTERVAL &&
                            health.staminaNotRechargingForSeconds <= 0f
            if (shouldRechargeStamina) {
                health.stamina += health.staminaRechargeRate
            }

            val (armor, shield) = entity[combatantMapper]!!.equipment
                    .let { it ->
                        Pair(it.armor, listOf(it.leftHand, it.rightHand).firstOrNull { it is Shield } as Shield?)
                    }

            health.damages.forEach { damage ->
                val defenderRotation = entity[transformMapper]!!.rotation
                val defenderPosition = entity[transformMapper]!!.position
                val damageFromAngle = (damage.originPosition - defenderPosition).angle()
                val shieldAdjustedDamage = shield?.let {
                    val inShieldArc = angleWithinArc(defenderRotation, damageFromAngle, it.protectionArc)
                    if (inShieldArc) {
                        damage.copy(stamina = (damage.stamina * (1f - it.damagePercentReduction)).toInt()).also {
                        }
                    } else {
                        damage
                    }
                } ?: damage

                val armorAdjustedDamage = armor?.let {
                    shieldAdjustedDamage.copy(stamina = shieldAdjustedDamage.stamina - it.damageReduction)
                } ?: shieldAdjustedDamage


                val frontArc = 180f
                val damageFromFront = angleWithinArc(defenderRotation, damageFromAngle, frontArc)

                if (health.stamina == 0 || !damageFromFront) {
                    if (health.health <= 0) {
                        entity.add(CDead())
                    } else {
                        health.health -= damage.health
                    }
                } else {
                    health.stamina = max(health.stamina - armorAdjustedDamage.stamina, 0)
                    if (health.stamina == 0) {
                        health.staminaNotRechargingForSeconds = 4f
                    }
                }
            }
            health.damages.clear()
        }
    }

    override fun update(deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)

        curTimeTillRecharge += dt
        super.update(dt)
        if (curTimeTillRecharge > STAMINA_RECHARGE_INTERVAL) {
            curTimeTillRecharge = 0f
        }
    }
}
