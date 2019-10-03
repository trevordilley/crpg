package com.ancient.game.crpg


import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

enum class Allegiance {
    PLAYER,
    ENEMY
}

sealed class SelectableKind
class CharacterSelect(val allegiance: Allegiance) : SelectableKind()
object HaulableSelect : SelectableKind()
class CSelectable(
        var kind: SelectableKind,
        var selected: Boolean = false,
        var newlySelected: Boolean = false,
        var newlyDeselected: Boolean = false
) : Component


class SelectionSystem : IteratingSystem(all(CSelectable::class.java).get()) {

    private val selectionM = mapperFor<CSelectable>()
    private val animationM = mapperFor<CAnimated>()
    private var characterSelection: MutableList<Entity> = mutableListOf()
    private var targetSelection: Entity? = null
    val selection: List<Entity> get() = characterSelection

    fun select(selection: Entity) {
        selection[selectionM]!!.let { selected ->
            if (selected.kind is HaulableSelect) {
                targetSelection = select(listOf(selection), targetSelection?.let { listOf(it) } ?: listOf()).firstOrNull()
            } else {
                characterSelection = select(listOf(selection), characterSelection).toMutableList()
            }

        }
    }


    fun select(selection: Collection<Entity>, currentSelection: Collection<Entity>): List<Entity> {
        if (selection.isNotEmpty()) {
            currentSelection
                    .filter { !selection.contains(it) }
                    .toMutableSet()
                    .let { deselect(it) }
            selection
                    .partition { it[selectionM]!!.selected }
                    .let { (alreadySelected, newlySelected) ->
                        alreadySelected.forEach {
                            it[selectionM]!!.selected = true
                            it[selectionM]!!.newlyDeselected = false
                        }
                        newlySelected.forEach {
                            it[selectionM]!!.selected = true
                            it[selectionM]!!.newlySelected = true
                        }
                    }
            return selection.toList()
        } else {
            return listOf()
        }
    }

    fun deselect(selectable: Entity) {
        selectable[selectionM]!!.selected = false
        selectable[selectionM]!!.newlyDeselected = true
    }

    fun deselect(selectables: Iterable<Entity>) {
        selectables.forEach {
            deselect(it)
        }
    }

    fun deselectAll() {
        characterSelection.forEach {
            deselect(it)
        }
        characterSelection.clear()
        targetSelection?.let { deselect(it) }
        targetSelection = null
    }


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val selected = entity[selectionM]!!
        val animated =
                entity[animationM]!!.anims.getValue(AsepriteAsset.SELECTION_CIRCLE)


        // Update position
        if (selected.newlySelected) {
            animated
                    .apply {
                        setAnimation<OnSelectAnimation>(OnAnimationEnd to {
                            setAnimation<SelectedAnimation>()
                        }, reset = true)
                    }
            selected.newlySelected = false
        } else if (selected.newlyDeselected) {
            animated.isActive = false
            selected.newlyDeselected = false
            selected.selected = false
        }
    }

}
