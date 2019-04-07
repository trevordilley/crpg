package com.ancient.game.crpg.battle

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor
import kotlin.math.max

// Tenth of a second, recharges 10 points per second at rate of 1
const val STAMINA_RECHARGE_INTERVAL = 0.10f

class CHealth(
        var stamina: Int,
        var maxStamina: Int,
        var staminaRechargeRate: Int,
        var health: Int,
        val damages: MutableList<Int> = mutableListOf(),
        var staminaNotRechargingForSeconds: Float = 0f) : Component

class CDead : Component
class HealthSystem : IteratingSystem(
        all(CHealth::class.java).exclude(CDead::class.java)
                .get()) {

    private var curTimeTillRecharge = 0f
    private val healthMapper: ComponentMapper<CHealth> = mapperFor()
    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[healthMapper]!!.let { health ->
            if (health.staminaNotRechargingForSeconds > 0f) {
                health.staminaNotRechargingForSeconds -= deltaTime
            }

            val shouldRechargeStamina =
                    health.stamina < health.maxStamina &&
                            curTimeTillRecharge > STAMINA_RECHARGE_INTERVAL &&
                            health.staminaNotRechargingForSeconds <= 0f
            if (shouldRechargeStamina) {
                health.stamina += health.staminaRechargeRate
            }

            health.damages.forEach { damage ->
                if (health.stamina == 0) {
                    if (health.health <= 0) {
                        entity.add(CDead())
                    } else {
                        health.health--
                    }
                } else {
                    health.stamina = max(health.stamina - damage, 0)
                    if (health.stamina == 0) {
                        health.staminaNotRechargingForSeconds = 4f
                    }
                }
            }
            health.damages.clear()
        }
    }

    override fun update(deltaTime: Float) {
        curTimeTillRecharge += deltaTime
        super.update(deltaTime)
        if (curTimeTillRecharge > STAMINA_RECHARGE_INTERVAL) {
            curTimeTillRecharge = 0f
        }
    }
}
