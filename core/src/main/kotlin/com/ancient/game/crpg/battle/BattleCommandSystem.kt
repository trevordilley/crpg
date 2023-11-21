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


object CPlayerControlled : Component {
    fun m() = mapperFor<CPlayerControlled>()
}

class CDiscovery(val description: String) : Component {
    companion object {
        fun m() = mapperFor<CDiscovery>()
    }
}

enum class InputMode {
    SELECT,
    DIRECT
}

sealed class LeftClickKind()
class PositionClick(val worldPos: Vector2) : LeftClickKind()
class InteractableClick(val entity: Entity) : LeftClickKind()
class CharacterClick(val entity: Entity) : LeftClickKind()
class BattleCommandSystem(private val viewport: Viewport,
                          private val mapManager: MapManager,
                          private val selectionSystem: SelectionSystem,
                          private val haulingSystem: HaulableSystem) : UserInputListener, IteratingSystem(
        all(CSelectable::class.java, CTransform::class.java)
                .get()) {

    private val log = gameLogger(this::class.java)

    private var destination: Vector2? = null
    private var rotation: Vector2? = null
    private var rotationPivot: Vector2? = null
    private var destinationChanged = false
    private var rotationChanged = false
    private var mode: InputMode = InputMode.SELECT

    override fun onInput(mouseInput: MouseInput, left: Boolean, up: Boolean,
                         right: Boolean, down: Boolean) {


        mouseInput.left?.let { leftClick ->
            when (leftClick) {
                is MouseUp -> {
                    val worldPos = viewport.unproject(leftClick.position.cpy())
                    val click =
                            entities
                                    .firstOrNull {
                                        it[CSelectable.m()] != null &&
                                                it[CTransform.m()]?.let { transform ->
                                                    pointWithinTransformRadius(worldPos, transform)
                                                } ?: false
                                    }
                                    ?.let { target ->
                                        if (target.has(CSelectable.m())) {
                                            if (target.has(CHaulable.m()) || target.has(
                                                            CDiscovery.m())) { // TODO: Must come before playerControlled for dead CharacterSelect
                                                InteractableClick(target)
                                            } else if (target.has(CPlayerControlled.m())) {
                                                CharacterClick(target)
                                            } else {
                                                println("Should not have fallend into this case! $target")
                                                PositionClick(worldPos)
                                            }
                                        } else PositionClick(worldPos)
                                    }
                                    ?: PositionClick(worldPos)

                    // Selection
                    val (destPos, onArrival) = when (click) {
                        is CharacterClick -> {
                            selectionSystem.select(click.entity)
                            null to null // clicked on a character, so probably just reselecting
                        }
                        is InteractableClick -> {
                            selectionSystem.select(click.entity)
                            val haulable = click.entity[CHaulable.m()]
                            if (haulable?.hauler != null) {
                                haulingSystem.drop(haulable)
                            }

                            val discoverable = click.entity[CDiscovery.m()]

                            click.entity[CTransform.m()]!!.position to {
                                if (haulable != null) {
                                    selectionSystem.selection.firstOrNull()?.let { ent ->
                                        haulingSystem.attemptToPickUp(ent, click.entity)
                                        selectionSystem.deselect(click.entity)
                                        click.entity[CAnimated.m()]?.anims?.values?.first()?.setAnimation<OnHaulAnimation>(
                                                OnAnimationEnd to { click.entity[CAnimated.m()]?.anims?.values?.first()?.setAnimation<IdleAnimation>() }
                                        )
                                    }
                                }
                                if (discoverable != null) {
                                    println(discoverable.description)
                                }
                            }
                        }
                        is PositionClick -> {
                            click.worldPos to null
                        }
                    }


                    // Movement
                    destPos?.let { dest ->
                        entities
                                .filter { it.has(CSelectable.m()) && it.has(CPlayerControlled.m()) && it.has(CMovable.m()) }
                                .filter { it[CDead.m()] == null }
                                .filter { selectionSystem.selection.contains(it) }
                                .forEach {

                                    val path = mapManager.findPath(it[CTransform.m()]!!.position, worldPos)

                                    it[CAnimated.m()]?.anims?.values?.first()?.setAnimation<MovingAnimation>()

                                    it[CMovable.m()]!!.destination = dest
                                    it[CMovable.m()]!!.onArrival = onArrival
                                    it[CMovable.m()]!!.path = path.let { p ->
                                        val stack = Stack<Vector2>()
                                        p.toList().reversed().forEach { navPoint ->
                                            stack.push(Vector2(navPoint.x.toFloat(), navPoint.y.toFloat()))
                                        }
                                        stack
                                    }
                                }

                    }


                }
                else -> {
                }
            }
        }

        mouseInput.right?.let { rightClick ->
            when (rightClick) {
                is MouseUp -> {
                    if (rotationPivot == null) {
                        resetSelection()
                    }
                }
                is MouseButtonDragging -> {
                    val pos = viewport.unproject(rightClick.position.cpy())

                    if (rotationPivot == null) {
                        rotationPivot = pos
                    } else {
                        rotationChanged = true
                        rotation = (pos - rotationPivot!!).nor().cpy()
                        rotationPivot = null
                    }
                }

                is MouseDown -> println("Mouse down")
            }
        }
    }


    private fun pointWithinTransformRadius(point: Vector2,
                                           transform: CTransform) =
            Vector2.dst(point.x, point.y, transform.position.x,
                    transform.position.y) <= transform.radius


    private fun resetSelection() {
        selectionSystem.deselectAll()
        mode = InputMode.SELECT
        destination = null
        rotation = null
        rotationPivot = null
        destinationChanged = true
        rotationChanged = true
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        if (entity.has(CSelectable.m()) && entity.has(CPlayerControlled.m())) {
            entity[CSelectable.m()]!!.let {
                if (it.selected) {
                    if (destinationChanged) {
                        entity[CMovable.m()]?.destination = destination
                        destinationChanged = false
                    }
                    if (rotationChanged) {
                        entity[CMovable.m()]?.facingDirection = rotation?.angle()
                        rotationChanged = false
                    }

                }
            }

        }
    }


}
