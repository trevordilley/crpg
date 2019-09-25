package com.ancient.game.crpg


import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.ashley.get
import ktx.ashley.mapperFor
import org.slf4j.LoggerFactory

enum class CombatantAnimationNames(val animName: String) {
    IDLE("Idle"),
    MOVING("walking"),
    ATTACK("Attack"),
}

enum class SelectionCircleAnimationNames(val animName: String) {
    SELECTED("Selected"),
    ON_SELECT("OnSelect")
}


sealed class AnimationState(val animation: Animation<TextureRegion>, val looping: Boolean, val timeDilation: Float = 1f)
class IdleAnimation(animation: Aseprite) :
        AnimationState(animation[CombatantAnimationNames.IDLE.animName], true)

class AttackAnimation(animation: Aseprite) : AnimationState(
        animation[CombatantAnimationNames.ATTACK.animName], false)

class MovingAnimation(animation: Aseprite) : AnimationState(animation[CombatantAnimationNames.MOVING.animName], true)

class SelectedAnimation(animation: Aseprite) : AnimationState(
        animation[SelectionCircleAnimationNames.SELECTED.animName], true)

class OnSelectAnimation(animation: Aseprite) : AnimationState(
        animation[SelectionCircleAnimationNames.ON_SELECT.animName], false, 2.5f)

class AnimationData(var currentAnimationState: AnimationState, val animations: List<AnimationState>,
                    private var active: Boolean, private var timePassed: Float = 0f) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var sprite = Sprite(currentAnimationState.animation.getKeyFrame(0f))
    // By making this an inline function it can't be made both private and reified
    // so we need to think of a simpler implementation.
    inline fun <reified T> setAnimation() where T : AnimationState {
        animations
                .filterIsInstance<T>()
                .firstOrNull()
                ?.let {
                    if (currentAnimationState !is T) {
                        currentAnimationState = it
                    }
                }
    }

    val isActive get() = active

    fun deactivate() {
        active = false
    }


    fun activate(reset: Boolean = true) {
        active = true
        if (reset) {
            timePassed = 0f
            actions.clear()
        }
    }


    fun step(dt: Float) {
        timePassed += (dt * currentAnimationState.timeDilation)
    }


    fun currentFrame(): Sprite =
            currentAnimationState
                    .let { it.animation.getKeyFrame(timePassed, it.looping) }
                    .let {
                        sprite.apply {
                            setRegion(it)
                        }
                    }


    fun invokeAction() =
            currentAnimationState.animation
                    .getKeyFrameIndex(timePassed)
                    .let { idx ->
                        actions[idx]?.invoke(this)
                        actions.remove(idx)
                    }


    fun addAction(frameIdx: Int, action: AnimationData.() -> Unit) {
        if (actions.containsKey(frameIdx)) {
            logger.warn("CAnimated already has action for index $frameIdx!!!")
        }
        actions[frameIdx] = action
    }

    private var actions: MutableMap<Int, AnimationData.() -> Unit> = mutableMapOf()
}

class CAnimated(val anims: Map<AsepriteAsset, AnimationData>) : Component


class AnimationSystem : IteratingSystem(all(CAnimated::class.java).get()) {
    private val animatedM = mapperFor<CAnimated>()
    override fun processEntity(entity: Entity, dt: Float) {
        entity[animatedM]!!
                .anims
                .values
                .filter { it.isActive }
                .forEach {
                    it.step(dt)
                    it.invokeAction()
                }
    }
}
