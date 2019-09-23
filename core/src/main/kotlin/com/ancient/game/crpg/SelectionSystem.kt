package com.ancient.game.crpg


import com.ancient.game.crpg.assetManagement.AsepriteAsset
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import ktx.ashley.get
import ktx.ashley.mapperFor

class CSelectable(
        var selected: Boolean = false,
        var newlySelected: Boolean = false,
        var newlyDeselected: Boolean = false
) : Component


class SelectionSystem : IteratingSystem(all(CSelectable::class.java).get()) {

    private val selectionMapper = mapperFor<CSelectable>()
    private val animationM = mapperFor<CAnimated>()
    private var currentSelection: MutableSet<CSelectable> = mutableSetOf()
    val selection: Set<CSelectable> get() = currentSelection


    fun select(selection: CSelectable) {
        select(listOf(selection))
    }

    fun select(selection: List<CSelectable>) {
        deselect()
        selection
                .partition { it.selected }
                .let { (alreadySelected, newlySelected) ->
                    alreadySelected.forEach {
                        it.selected = true
                        it.newlyDeselected = false
                    }
                    newlySelected.forEach {
                        println("Newly selected")
                        it.selected = true
                        it.newlySelected = true
                    }
                }
        currentSelection.addAll(selection)
    }

    fun deselect() {
        currentSelection.forEach {
            it.selected = false
            it.newlyDeselected = true
        }
        currentSelection.clear()
    }


    override fun processEntity(entity: Entity, deltaTime: Float) {
        val selected = entity[selectionMapper]!!
        val animated = entity[animationM]!!.anims.getValue(AsepriteAsset.SELECTION_CIRCLE)
        // Update position
        if (selected.newlySelected) {
            animated
                    .apply {
                        activate()
                        setAnimation<OnSelectAnimation>()
                        addAction(5) {
                            setAnimation<SelectedAnimation>()
                        }
                    }
//            selected
//                    .selectionCircle
//                    .apply {
//                        add(
//                                CRenderableSprite(
//                                        Sprite(
//                                                selectionAnimationState.animation.getKeyFrame(0f)
//                                        )
//                                )
//                        )
//                        add(
//                                CAnimated(
//                                        selected.onSelectAnimation,
//                                        listOf(selected.selectedAnimation, selected.onSelectAnimation)
//                                )
//                                        .apply {
//                                            setAnimation<OnSelectAnimation>()
//                                            addAction(5) {
//                                                setAnimation<SelectedAnimation>()
//                                            }
//                                        })
//
//                    }
            selected.newlySelected = false
        } else if (selected.newlyDeselected) {
            animated.deactivate()
//            selected.selectionCircle.apply {
//                remove(CRenderableSprite::class.java)
//                remove(CAnimated::class.java)
//            }
            selected.newlyDeselected = false
            selected.selected = false
        }
    }

}
