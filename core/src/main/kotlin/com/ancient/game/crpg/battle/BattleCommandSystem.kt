package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.map.MapManager
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
import java.util.Stack


class CSelectable(var selected: Boolean = false) : Component
object CPlayerControlled : Component

enum class InputMode {
    SELECT,
    DIRECT
}

class BattleCommandSystem(private val viewport: Viewport,
                          private val mapManager: MapManager) : UserInputListener, IteratingSystem(
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


    override fun onInput(mouseInput: MouseInput, left: Boolean, up: Boolean,
                         right: Boolean, down: Boolean) {


        mouseInput.left?.let { leftClick ->
            when (leftClick) {
                is MouseUp -> {
                    val worldPos = viewport.unproject(Vector2(leftClick.screenX, leftClick.screenY))
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

                                            val destination = viewport.unproject(
                                                    Vector2(leftClick.screenX, leftClick.screenY))

                                            val path = mapManager.findPath(it[transform]!!.position, destination)

                                            it[movable]!!.destination = viewport.unproject(
                                                    Vector2(leftClick.screenX, leftClick.screenY))
                                            it[movable]!!.path = path.let { p ->
                                                val stack = Stack<Vector2>()
                                                p.toList().reversed().forEach { tile ->
                                                    // Move to middle of the tile
                                                    stack.push(Vector2(tile.pos.x + 0.5f, tile.pos.y + 0.5f))
                                                }
                                                stack
                                            }
                                        }
                            }.invoke()

                }
                else -> {
                }
            }
        }

        mouseInput.right?.let { rightClick ->
            when (rightClick) {
                is MouseDown -> {
                    rotationPivot = viewport.unproject(Vector2(rightClick.screenX, rightClick.screenY))
                }
                is MouseUp -> {
                    if (rightClick.wasDragging) {
                        rotation = rotationPivot?.let { pivot ->
                            rotationChanged = true
                            val towards = viewport.unproject(Vector2(rightClick.screenX, rightClick.screenY))
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
