package com.ancient.game.crpg

fun <T, R> T.letIf(predicate: Boolean, block: (T) -> R): R? {
    return if (predicate) {
        block(this)
    } else {
        null
    }
}
