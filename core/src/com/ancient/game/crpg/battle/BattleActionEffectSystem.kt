package com.ancient.game.crpg.battle

import com.ancient.game.crpg.TransformC
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor


sealed class ActionEffectC : Component
class MeleeEffect(val attacker: Entity, val target: Entity, val range: Float, val staminaDamage: Int) : ActionEffectC()
class AreaEffect(val position: Vector2, val staminaDamage: Int) : ActionEffectC()

class BattleActionEffectSystem : IteratingSystem(all(ActionEffectC::class.java).get()) {
    private val effectMapper: ComponentMapper<ActionEffectC> = mapperFor()
    private val transformMapper: ComponentMapper<TransformC> = mapperFor()
    private val healthMapper: ComponentMapper<HealthC> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[effectMapper]?.let { effect ->
            when (effect) {
                is MeleeEffect -> applyEffect(effect)
                is AreaEffect -> applyEffect(effect)
            }
        }
        engine.removeEntity(entity)
    }

    private fun applyEffect(effect: MeleeEffect) = effect.let { eff ->
        eff.target[transformMapper]?.let { tarPos ->
            eff.target[healthMapper]?.let { health ->
                eff.attacker[transformMapper]?.let { attackerPos ->
                    val distance =
                            Vector2.dst(
                                    attackerPos.position.x,
                                    attackerPos.position.y,
                                    tarPos.position.x,
                                    tarPos.position.y
                            )
                    if (distance <= eff.range) {
                        health.damages.add(eff.staminaDamage)
                    }
                }
            }
        }

    }

    private fun applyEffect(effect: AreaEffect) = effect


}