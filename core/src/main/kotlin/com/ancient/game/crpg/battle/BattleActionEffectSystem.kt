package com.ancient.game.crpg.battle

import com.ancient.game.crpg.CTransform
import com.ancient.game.crpg.UserInputManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.one
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.mapperFor

sealed class CActionEffect : Component
class CMeleeEffect(val attacker: Entity, val target: Entity, val range: Float, val staminaDamage: Int) : CActionEffect()

class BattleActionEffectSystem : IteratingSystem(one(
        CMeleeEffect::class.java).get()) {

    companion object {
        fun applyEffect(engine: Engine, effect: CActionEffect) {
            engine.addEntity(Entity().add(effect))
        }
    }

    private val meleeMapper: ComponentMapper<CMeleeEffect> = mapperFor()
    private val transformMapper: ComponentMapper<CTransform> = mapperFor()
    private val healthMapper: ComponentMapper<CHealth> = mapperFor()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (!UserInputManager.isPaused) {
            entity[meleeMapper]?.let { effect -> applyEffect(effect) }
            engine.removeEntity(entity)
        }
    }

    private fun applyEffect(effect: CMeleeEffect) = effect.let { eff ->
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
                        health.damages.add(Damage(eff.staminaDamage, attackerPos.position))
                    }
                }
            }
        }
    }
}
