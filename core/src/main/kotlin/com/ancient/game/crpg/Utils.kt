package com.ancient.game.crpg

fun <T, R> T.letIf(predicate: Boolean, block: (T) -> R): R? {
    return if (predicate) {
        block(this)
    } else {
        null
    }
}

const val FULL_ROTATION_DEGREES = 360f
fun angleWithinArc(rotation: Float, angle: Float, arc: Float): Boolean {

    val normRotation = rotation % FULL_ROTATION_DEGREES
    val normalizedAngle = ((angle - normRotation) + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES
    val halfArc = arc / 2f

    val withinPositiveArc = normalizedAngle in 0f..halfArc
    val withinNegativeShieldArc = (normalizedAngle - FULL_ROTATION_DEGREES) in -halfArc..0f
    return (withinPositiveArc || withinNegativeShieldArc)
}
