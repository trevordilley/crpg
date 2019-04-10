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
}