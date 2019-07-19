package com.ancient.game.crpg.assetManagement


enum class SpriteAsset(val filePath: String) {
    SWORD_SHIELD("64_rpg_sword_shield.png"),
    ORC("64_orc.png"),
    LIGHT_SOURCE("64_light_source.png"),
}

enum class AsepriteAsset(val assetName: String) {
    SWORD_SHIELD("64_rpg_sword_shield_ase"),
    ORC("orc_ase"),
    SELECTION_CIRCLE("selection_circle")
}

const val MAP_FILEPATH = "64-dungeon-30x30.tmx"
