package com.ancient.game.crpg

import com.pholser.junit.quickcheck.Property
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitQuickcheck::class)
internal class UtilsKtTest {

    private fun positiveNormedDegrees(deg: Int) = Math.abs(deg.toFloat()) % FULL_ROTATION_DEGREES

    @Property
    fun `all angles facing 0 deg with 180 deg arc`(angle: Int) {
        val normed = positiveNormedDegrees(angle)
        if (normed > 90f && normed < 270f) {
            assert(!angleWithinArc(0f, normed, 180f))
        } else {
            assert(angleWithinArc(0f, normed, 180f))
        }
    }

    @Property
    fun `all rotations with 90 angle and 180 arc`(rotation: Int) {
        val normedRot = positiveNormedDegrees(rotation)
        if (normedRot in 0f..180f) {
            assert(angleWithinArc(normedRot, 90f, 180f))
        } else {
            // BROKE
            assert(!angleWithinArc(normedRot, 90f, 180f))
        }
    }

    @Property
    fun `all arcs facing 0 degrees with 90 angle`(arc: Int) {
        val normedArc = positiveNormedDegrees(arc)

        if (normedArc > 180f) {
            assert(angleWithinArc(0f, 90f, normedArc))
        } else {
            assert(!angleWithinArc(0f, 90f, normedArc))
        }
    }

    @Test
    fun `angle 90 within 180 arc while facing 0 `() {
        assert(angleWithinArc(0f, 90f, 180f))
    }

    @Test
    fun `angle 180 NOT within 180 arc facing 0`() {
        assert(!angleWithinArc(0f, 180f, 180f))
    }

    @Test
    fun `rotating from 0 degrees to 90 degrees should result in 1 degree after first tick`() {
        val a = rotate(0f, 90f, 1f)
        assert(a == 1f) { " Expected 1f but got $a" }
    }

    @Test
    fun `negative degrees properly normalized`() {
        assert(normalizeDeg(-1f) == 359f)
        assert(normalizeDeg(-90f) == 270f)
        assert(normalizeDeg(90f) == 90f)
    }


    @Property
    fun `rotate correctly over time`(targetAngleInt: Int, startRotationInt: Int) {
        val targetAngle = positiveNormedDegrees(targetAngleInt)
        val speed = 8f

        var currentAngle = positiveNormedDegrees(startRotationInt)

        println("Current: $currentAngle -- Target: $targetAngle")

        val numAllowedSteps = 200
        for (i in 0..numAllowedSteps) {
            currentAngle = rotate(currentAngle, targetAngle, speed)
            if (currentAngle == targetAngle) {
                assert(true)
                return
            }
        }
        assert(false)
    }

    @Test
    fun `rotate to 5 from 348`() {
        //dt: 343.62723 			 c: 334.76614 -- cc: 350.76614 			 current: 348.86108 -- target: 5.233848
        val a = rotate(348.86108f, 5.233848f, 8f)
        assert(a >= 356) { "Expected a to be around 356, got $a" }
    }

    @Test
    fun `rot distance from 348 to 5 is 17`() {
        val dist = determineRotationDistance(5f, 348f)
        val expected = 17f
        assert(dist == expected) { "Expected distance of $expected but got $dist" }

    }
}