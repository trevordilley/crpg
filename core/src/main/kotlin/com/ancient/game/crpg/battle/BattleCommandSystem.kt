package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.ashley.get
import ktx.ashley.has
import ktx.ashley.mapperFor
import ktx.math.minus


class CSelectable(var selected: Boolean = false) : Component
object CPlayerControlled : Component

enum class InputMode {
    SELECT,
    DIRECT
}

class BattleCommandSystem(private val viewport: Viewport) : UserInputListener, IteratingSystem(
        all(CSelectable::class.java, CMovable::class.java, CTransform::class.java)
                .exclude(CDead::class.java)
                .get()) {

    private val log = gameLogger(this::class.java)

    private var destination: Vector2? = null
    private var rotation: Vector2? = null
    private var rotationPivot: Vector2? = null
    private var destinationChanged = false
    private var rotationChanged = false
    private var currentSelection: MutableSet<CSelectable> = mutableSetOf()
    private var mode: InputMode = InputMode.SELECT

    private val movable: ComponentMapper<CMovable> = mapperFor()
    private val transform: ComponentMapper<CTransform> = mapperFor()
    private val playerControlled: ComponentMapper<CPlayerControlled> = mapperFor()
    private val selectable: ComponentMapper<CSelectable> = mapperFor()

    override fun onInput(input: MouseInput, left: Boolean, up: Boolean,
                         right: Boolean, down: Boolean) {


        input.left?.let { left ->
            when (left) {
                is MouseUp -> {
                    val worldPos = viewport.unproject(Vector2(left.screenX, left.screenY))
                    entities
                            .firstOrNull {
                                it[selectable] != null &&
                                        it[transform]?.let { transform ->
                                            pointWithinTransformRadius(worldPos, transform)
                                        } ?: false
                            }
                            ?.let { select(it[selectable]!!) }
                            ?: {
                                entities
                                        .filter { it.has(selectable) && it.has(playerControlled) && it.has(movable) }
                                        .filter { currentSelection.contains(it[selectable]) }
                                        .forEach {
                                            it[movable]!!.destination = viewport.unproject(
                                                    Vector2(left.screenX, left.screenY))
                                        }
                            }.invoke()

                }
                else -> {
                }
            }
        }

        input.right?.let { right ->
            when (right) {
                is MouseDown -> {
                    rotationPivot = viewport.unproject(Vector2(right.screenX, right.screenY))
                }
                is MouseUp -> {
                    if (right.wasDragging) {
                        rotation = rotationPivot?.let { pivot ->
                            rotationChanged = true
                            val towards = viewport.unproject(Vector2(right.screenX, right.screenY))
                            rotationPivot = null
                            (towards - pivot).nor().also {
                            }
                        }
                    } else {
                        resetSelection()
                    }
                }
            }
        }
    }


    private fun select(selection: CSelectable) {
        select(listOf(selection))
    }

    private fun select(selection: List<CSelectable>) {
        deselect()
        selection.forEach {
            it.selected = true
        }
        currentSelection.addAll(selection)
    }

    private fun deselect() {
        currentSelection.forEach {
            it.selected = false
        }
        currentSelection.clear()
    }

    private fun pointWithinTransformRadius(point: Vector2,
                                           transform: CTransform) =
            Vector2.dst(point.x, point.y, transform.position.x,
                    transform.position.y) <= transform.radius


    private fun resetSelection() {
        deselect()
        mode = InputMode.SELECT
        destination = null
        rotation = null
        rotationPivot = null
        destinationChanged = true
        rotationChanged = true
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (entity.has(selectable) && entity.has(playerControlled)) {
            entity[selectable]!!.let {
                if (it.selected) {
                    if (destinationChanged) {
                        entity[movable]?.destination = destination
                        destinationChanged = false
                    }
                    if (rotationChanged) {
                        entity[movable]?.facingDirection = rotation?.angle()
                        rotationChanged = false
                    }

                }
            }

        }
    }


}
