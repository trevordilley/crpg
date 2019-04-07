package com.ancient.game.crpg.equipment

enum class NumberHandsToWield {
    ONE,
    TWO
}


data class Armor(
        val name: String,
        val damageReduction: Int,
        val speedCostPercent: Float
)

sealed class Wielded

data class MeleeWeapon(
        val name: String,
        val staminaCost: Int,
        val staminaDamage: Int,
        val duration: Float,
        val numHands: NumberHandsToWield,
        val range: Float
) : Wielded()

data class Shield(
        val name: String,
        val damagePercentReduction: Float
) : Wielded()

object Nothing : Wielded()

data class Equipment(val leftHand: Wielded, val rightHand: Wielded, val armor: Armor?)

