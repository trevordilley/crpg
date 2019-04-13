package com.ancient.game.crpg

import kotlin.math.min

fun <T, R> T.letIf(predicate: Boolean, block: (T) -> R): R? {
    return if (predicate) {
        block(this)
    } else {
        null
    }
}

fun normalizeDeg(deg: Float) = (deg + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES

const val FULL_ROTATION_DEGREES = 360f
fun angleWithinArc(rotation: Float, angle: Float, arc: Float): Boolean {

    val normRotation = rotation % FULL_ROTATION_DEGREES
    val normalizedAngle = ((angle - normRotation) + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES
    val halfArc = arc / 2f

    val withinPositiveArc = normalizedAngle in 0f..halfArc
    val withinNegativeShieldArc = (normalizedAngle - FULL_ROTATION_DEGREES) in -halfArc..0f
    return (withinPositiveArc || withinNegativeShieldArc)
}

//TODO: I'm sure rotation is sub-optimal...
fun rotationStep(speed: Float, currentRotation: Float) = normalizeDeg(currentRotation + speed).let {
    Math.floor(it.toDouble())
}.toFloat()

fun determineRotationDistance(targetRotation: Float, currentRotation: Float): Float {
    // Example: cur 348, target 5, distance is 17
    val d1 = Math.abs(targetRotation - currentRotation)
    val d2 = Math.abs((FULL_ROTATION_DEGREES - currentRotation + targetRotation))
    return min(d1, d2)
}

fun rotate(currentRotation: Float, targetRotation: Float, rotationSpeed: Float): Float {
    val rotDelta = determineRotationDistance(targetRotation, currentRotation)
    return if (rotDelta.toInt() <= rotationSpeed) {
        // Rotation difference is so small we should just
        // set it to the target. This may look "snappy" with
        // high rotationSpeed values.
        targetRotation
    } else {

        val clockwise = rotationStep(-rotationSpeed, currentRotation)
        val clockwiseDistance = determineRotationDistance(targetRotation, clockwise)

        val counterClockwise = rotationStep(rotationSpeed, currentRotation)
        val counterClockwiseDistance = determineRotationDistance(targetRotation, counterClockwise)

        if (clockwiseDistance < counterClockwiseDistance) {
            clockwise
        } else {
            counterClockwise
        }
    }
}
