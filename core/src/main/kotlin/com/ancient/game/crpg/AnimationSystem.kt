package com.ancient.game.crpg


import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.ancient.game.crpg.assetManagement.aseprite.AsepriteAnimation
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Sprite
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


sealed class AnimationState(val animation: AsepriteAnimation, val looping: Boolean, val timeDilation: Float = 1f)
class IdleAnimation(animation: Aseprite) :
        AnimationState(animation[CombatantAnimationNames.IDLE.animName], true)

class AttackAnimation(animation: Aseprite) : AnimationState(
        animation[CombatantAnimationNames.ATTACK.animName], false)

class MovingAnimation(animation: Aseprite) : AnimationState(animation[CombatantAnimationNames.MOVING.animName], true)

class SelectedAnimation(animation: Aseprite) : AnimationState(
        animation[SelectionCircleAnimationNames.SELECTED.animName], true)

class OnSelectAnimation(animation: Aseprite) : AnimationState(
        animation[SelectionCircleAnimationNames.ON_SELECT.animName], false, 2.5f)


sealed class AnimationActionTriggers
class OnIndex(val index: Int) : AnimationActionTriggers()
class OnTag(val tag: String) : AnimationActionTriggers()
object OnAnimationEnd : AnimationActionTriggers()

class AnimationData(
        var currentAnimationState: AnimationState,
        val animations: List<AnimationState>,
        var isActive: Boolean = true,
        var timePassed: Float = 0f
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var sprite = Sprite(currentAnimationState.animation.frame(0f))
    // By making this an inline function it can't be made both private and reified
    // so we need to think of a simpler implementation.
    inline fun <reified T> setAnimation(vararg actions: Pair<AnimationActionTriggers, () -> Unit?>, reset: Boolean = false) where T : AnimationState {
        if (currentAnimationState is T && !reset) {
            return
        }

        if (reset) {
            isActive = true
            timePassed = 0f
        }

        animations
                .filterIsInstance<T>()
                .firstOrNull()
                ?.let {
                    if (currentAnimationState !is T || reset) {
                        currentAnimationState = it
                        actions.forEach { (trigger, action) -> addAction(trigger, action) }
                    }
                }
    }

    fun step(dt: Float) {
        timePassed += (dt * currentAnimationState.timeDilation)
    }


    fun currentFrame(): Sprite =
            currentAnimationState
                    .let { it.animation.frame(timePassed, it.looping) }
                    .let {
                        sprite.apply {
                            setRegion(it)
                        }
                    }


    fun invokeAction() =
            currentAnimationState
                    .animation
                    .frameIndex(timePassed)
                    .let { idx ->
                        val before = actions[idx]?.first
                        actions[idx]?.let { (triggered, action) ->
                            println("During invoke $triggered")
                            println("${System.currentTimeMillis()}")
                            if (!triggered) action.invoke()
                            actions[idx] = true to action
                        }
                        val after = actions[idx]?.first
                    }


    private fun onFrame(frameIdx: Int, action: () -> Unit?) {
        actions[frameIdx] = false to action
    }

    private fun onTag(tag: String, action: () -> Unit?) {
        val idx = currentAnimationState.animation.frameTags.indexOfFirst { it.contains(tag) }
        if (idx == -1) throw RuntimeException("Tag $tag does not exist on ${currentAnimationState.animation.name}")
        onFrame(currentAnimationState.animation.frameTags.indexOfFirst { it.contains(tag) }, action)
    }

    private fun onEnd(action: () -> Unit?) {
        onFrame(currentAnimationState.animation.numFrames - 1, action)
    }


    fun addAction(trigger: AnimationActionTriggers, action: () -> Unit?) {
        when (trigger) {
            is OnIndex -> onFrame(trigger.index, action)
            is OnTag -> onTag(trigger.tag, action)
            is OnAnimationEnd -> onEnd(action)
        }
    }

    private var actions: MutableMap<Int, Pair<Boolean, () -> Unit?>> = mutableMapOf()
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
