package com.ancient.game.crpg


import com.ancient.game.crpg.assetManagement.aseprite.Aseprite
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.ashley.get
import ktx.ashley.mapperFor
import org.slf4j.LoggerFactory

enum class AnimationNames(val animName: String) {
    IDLE("Idle"),
    MOVING("walking"),
    ATTACK("Attack")
}


sealed class AnimationState(val animation: Animation<TextureRegion>, val looping: Boolean)
class IdleAnimation(animation: Aseprite) :
        AnimationState(animation[AnimationNames.IDLE.animName], true)

class AttackAnimation(animation: Aseprite) : AnimationState(animation[AnimationNames.ATTACK.animName], false)
class MovingAnimation(animation: Aseprite) : AnimationState(animation[AnimationNames.MOVING.animName], true)

class CAnimated(private val currentAnimationState: AnimationState) : Component {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var timePassed: Float = 0f
    fun step(dt: Float) {
        timePassed += dt
    }


    fun currentFrame(): TextureRegion = currentAnimationState.let {
        it.animation.getKeyFrame(timePassed, it.looping)
    }

    fun invokeAction() =
            currentAnimationState.animation
                    .getKeyFrameIndex(timePassed)
                    .let { idx ->
                        actions[idx]?.invoke(this)
                        actions.remove(idx)
                    }

    fun addAction(frameIdx: Int, action: CAnimated.() -> Unit) {
        if (actions.containsKey(frameIdx)) {
            logger.warn("CAnimated already has action for index $frameIdx!!!")
        }
        actions[frameIdx] = action
    }

    private var actions: MutableMap<Int, CAnimated.() -> Unit> = mutableMapOf()
}

class AnimationSystem() : IteratingSystem(
        all(
                CAnimated::class.java,
                CRenderableSprite::class.java
        ).get()) {
    private val animated = mapperFor<CAnimated>()
    private val rendered = mapperFor<CRenderableSprite>()
    override fun processEntity(entity: Entity, dt: Float) {
        val anim = entity[animated]!!
        anim.step(dt)
        val render = entity[rendered]!!
        // TODO: Is this correct? Unsure if this is performant...
        render.sprite.setRegion(anim.currentFrame())

        anim.invokeAction()
    }
}
