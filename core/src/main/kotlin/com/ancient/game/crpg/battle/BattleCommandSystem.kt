package com.ancient.game.crpg.battle

import com.ancient.game.crpg.*
import com.ancient.game.crpg.battle.hauling.CHaulable
import com.ancient.game.crpg.battle.hauling.HaulableSystem
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


object CPlayerControlled : Component

enum class InputMode {
    SELECT,
    DIRECT
}

sealed class LeftClickKind()
class PositionClick(val worldPos: Vector2) : LeftClickKind()
class HaulableClick(val entity: Entity) : LeftClickKind()
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

    private val movableM: ComponentMapper<CMovable> = mapperFor()
    private val transformM: ComponentMapper<CTransform> = mapperFor()
    private val playerControlledM: ComponentMapper<CPlayerControlled> = mapperFor()
    private val selectableM: ComponentMapper<CSelectable> = mapperFor()
    private val animatedM: ComponentMapper<CAnimated> = mapperFor()
    private val haulableM: ComponentMapper<CHaulable> = mapperFor()
    private val deadM: ComponentMapper<CDead> = mapperFor()
    override fun onInput(mouseInput: MouseInput, left: Boolean, up: Boolean,
                         right: Boolean, down: Boolean) {


        mouseInput.left?.let { leftClick ->
            when (leftClick) {
                is MouseUp -> {
                    val worldPos = viewport.unproject(leftClick.position.cpy())
                    val click =
                            entities
                                    .firstOrNull {
                                        it[selectableM] != null &&
                                                it[transformM]?.let { transform ->
                                                    pointWithinTransformRadius(worldPos, transform)
                                                } ?: false
                                    }
                                    ?.let { target ->
                                        if (target.has(selectableM)) {
                                            if (target.has(haulableM)) { // TODO: Must come before playerControlled for dead CharacterSelect
                                                HaulableClick(target)
                                            } else if (target.has(playerControlledM)) {
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
                        is HaulableClick -> {
                            selectionSystem.select(click.entity)
                            val haulable = click.entity[haulableM]!!
                            if (haulable.hauler != null) {
                                haulingSystem.drop(haulable)
                            }
                            click.entity[transformM]!!.position to {
                                selectionSystem.selection.firstOrNull()?.let { ent ->
                                    haulingSystem.attemptToPickUp(ent, click.entity)
                                    selectionSystem.deselect(click.entity)
                                    click.entity[animatedM]?.anims?.values?.first()?.setAnimation<OnHaulAnimation>(
                                            OnAnimationEnd to { click.entity[animatedM]?.anims?.values?.first()?.setAnimation<IdleAnimation>() }
                                    )
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
                                .filter { it.has(selectableM) && it.has(playerControlledM) && it.has(movableM) }
                                .filter { it[deadM] == null }
                                .filter { selectionSystem.selection.contains(it) }
                                .forEach {

                                    val path = mapManager.findPath(it[transformM]!!.position, worldPos)

                                    it[animatedM]?.anims?.values?.first()?.setAnimation<MovingAnimation>()

                                    it[movableM]!!.destination = dest
                                    it[movableM]!!.onArrival = onArrival
                                    it[movableM]!!.path = path.let { p ->
                                        val stack = Stack<Vector2>()
                                        p.toList().reversed().forEach { tile ->
                                            // Move to middle of the tile
                                            stack.push(Vector2(tile.pos.x + 0.5f, tile.pos.y + 0.5f))
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
                is MouseUp -> if (rotationPivot == null) {
                    resetSelection()
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
        if (entity.has(selectableM) && entity.has(playerControlledM)) {
            entity[selectableM]!!.let {
                if (it.selected) {
                    if (destinationChanged) {
                        entity[movableM]?.destination = destination
                        destinationChanged = false
                    }
                    if (rotationChanged) {
                        entity[movableM]?.facingDirection = rotation?.angle()
                        rotationChanged = false
                    }

                }
            }

        }
    }


}
