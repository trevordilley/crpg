package com.ancient.game.crpg

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.Polygon
import kotlin.math.min


data class TriangleShapeData(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float)


fun ShapeRenderer.triangle(data: TriangleShapeData) {

    this.triangle(data.x1, data.y1, data.x2, data.y2, data.x3, data.y3)
}


fun EarClippingTriangulator.createRenderableFilledPolygonMesh(polygon: Polygon): List<TriangleShapeData> {
    val vertices = polygon.transformedVertices
    val triangleIndices = this.computeTriangles(vertices)
    return mutableListOf<TriangleShapeData>().apply {
        var i = 0
        while (i < triangleIndices.size) {
            add(
                    // Each of these lists should be used like this in the render code:
                    // shapeRenderer.triangle(it)
                    TriangleShapeData(
                            vertices[triangleIndices.get(i) * 2],
                            vertices[triangleIndices.get(i) * 2 + 1],
                            vertices[triangleIndices.get(i + 1) * 2],
                            vertices[triangleIndices.get(i + 1) * 2 + 1],
                            vertices[triangleIndices.get(i + 2) * 2],
                            vertices[triangleIndices.get(i + 2) * 2 + 1])
            )
            i += 3
        }
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
