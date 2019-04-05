package com.ancient.game.crpg.battle

import com.ancient.game.crpg.TransformC
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.one
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor

interface ActionEffectC : Component
class MeleeEffectC(val attacker: Entity, val target: Entity, val range: Float, val staminaDamage: Int) : ActionEffectC
class AreaEffectC(val position: Vector2, val staminaDamage: Int) : ActionEffectC

class BattleActionEffectSystem : IteratingSystem(one(
        ActionEffectC::class.java,
        MeleeEffectC::class.java, AreaEffectC::class.java).get()) {
    private val effectMapper: ComponentMapper<MeleeEffectC> = mapperFor()
    private val transformMapper: ComponentMapper<TransformC> = mapperFor()
    private val healthMapper: ComponentMapper<HealthC> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        println("BattleActionEffectSystem.processEntity EXECUTIN EFFECT")
        entity[effectMapper]?.let { effect ->
            applyEffect(effect)
        }
        println("Before engine.systems.size() = ${engine.systems.size()}")
        engine.removeEntity(entity)
        println("After engine.systems.size() = ${engine.systems.size()}")
    }

    private fun applyEffect(effect: MeleeEffectC) = effect.let { eff ->
        println("BattleActionEffectSystem.applyEffect")
        eff.target[transformMapper]?.let { tarPos ->
            println("tarPos")
            eff.target[healthMapper]?.let { health ->
                println("tarHelath")
                eff.attacker[transformMapper]?.let { attackerPos ->
                    println("attackTrans")
                    val distance =
                            Vector2.dst(
                                    attackerPos.position.x,
                                    attackerPos.position.y,
                                    tarPos.position.x,
                                    tarPos.position.y
                            )
                    println("{distance} = $distance")
                    if (distance <= eff.range) {
                        println("eff.staminaDamage = ${eff.staminaDamage}")
                        health.damages.add(eff.staminaDamage)
                    }
                }
            }
        }

    }


}