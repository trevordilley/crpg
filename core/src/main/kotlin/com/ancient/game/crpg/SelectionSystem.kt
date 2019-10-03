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
        val kind: SelectableKind,
        var selected: Boolean = false,
        var newlySelected: Boolean = false,
        var newlyDeselected: Boolean = false
) : Component


class SelectionSystem : IteratingSystem(all(CSelectable::class.java).get()) {

    private val selectionM = mapperFor<CSelectable>()
    private val animationM = mapperFor<CAnimated>()
    private var currentSelection: MutableSet<CSelectable> = mutableSetOf()
    val selection: Set<CSelectable> get() = currentSelection


    fun select(selection: CSelectable) {
        println("Selecting ${selection.kind}")
        select(listOf(selection))
    }


    fun select(selection: List<CSelectable>) {

        if (selection.isNotEmpty()) {
            currentSelection
                    .filter { !selection.contains(it) }
                    .toMutableSet()
                    .let { deselect(it) }
            currentSelection = selection.toMutableSet()
            selection
                    .partition { it.selected }
                    .let { (alreadySelected, newlySelected) ->
                        alreadySelected.forEach {
                            it.selected = true
                            it.newlyDeselected = false
                        }
                        newlySelected.forEach {
                            it.selected = true
                            it.newlySelected = true
                        }
                    }
            currentSelection.addAll(selection)

        }

    }

    fun deselect(selectable: CSelectable) {
        selectable.selected = false
        selectable.newlyDeselected = true
    }

    fun deselect(selectables: Iterable<CSelectable>) {
        selectables.forEach {
            deselect(it)
        }
    }

    fun deselectAll() {
        currentSelection.forEach {
            deselect(it)
        }
        currentSelection.clear()
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
