package com.ancient.game.crpg.battle

import com.ancient.game.crpg.CAnimated
import com.ancient.game.crpg.CTransform
import com.ancient.game.crpg.SiUnits
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get
import ktx.ashley.mapperFor


class BattleHealthUiRenderer(private val viewport: Viewport) : IteratingSystem(
        all(
                CHealth::class.java,
                CTransform::class.java,
                CAnimated::class.java
        ).get()) {

    private var healthUiToRender = mutableListOf<Entity>()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[CHealth.m()]?.let { healthUiToRender.add(entity) }
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        val shapeRenderer = ShapeRenderer()
        shapeRenderer.projectionMatrix =
                viewport.camera.combined
        //viewport.camera.combined.cpy().scale(SiUnits.PIXELS_TO_METER, SiUnits.PIXELS_TO_METER, 1f)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        healthUiToRender.forEach { entity ->
            val sprite = entity[CAnimated.m()]!!.anims.values.first().currentFrame()
            val health = entity[CHealth.m()]!!

            val (x, y, r) =
                    entity[CTransform.m()]!!
                            .let {
                                Triple(
                                        it.position.x,
                                        it.position.y,
                                        it.rotation
                                )
                            }
            val baseAlpha = 0.05f
            // Show the health as an arc

            val healthPct = health.health.toFloat() / health.maxHealth.toFloat()
            val healthColor = Color(1f, 0f, 0f, (baseAlpha + (1 - healthPct)))
            val healthDegrees = if (health.stamina == 0) {
                360f
            } else {
                180f
            }
            shapeRenderer.apply {
                color = healthColor
                arc(
                        x,
                        y,
                        (((sprite.width * healthPct) * SiUnits.PIXELS_TO_METER) / 2),
                        r + 90f,
                        healthDegrees, 100
                )

                val stamPct = health.stamina.toFloat() / health.maxStamina.toFloat()
                val stamColor = Color((1 - stamPct), stamPct, 0f, baseAlpha + (1 - stamPct))
                listOf(180f)
                        .forEach { deg ->
                            color = stamColor
                            arc(
                                    x,
                                    y,
                                    ((
                                            (sprite.width * stamPct)
                                                    * SiUnits.PIXELS_TO_METER) / 2
                                            )
                                    ,
                                    r - (deg / 2),
                                    deg,
                                    100)
                        }
            }
        }
        shapeRenderer.end()
        healthUiToRender.clear()
    }
}
