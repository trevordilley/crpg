package com.ancient.game.crpg.assetManagement


enum class AsepriteAsset(val assetName: String) {
    SWORD_SHIELD("64_rpg_sword_shield_ase"),
    ORC("orc"),
    SELECTION_CIRCLE("selection_circle"),
    TREASURE("treasure"),
    HEALING_DROP_ZONE("healing_dropzone"),
    LOOT_DROP_ZONE("cart")
}

const val LEVEL_FILEPATH = "levels/main.level.json"
const val LEVEL_BACKGROUND_FILEPATH = "levels/main-background.png"
