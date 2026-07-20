package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.map.CreatureSize
import com.ancient.game.crpg.systems.CTransform
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Stack

/**
 * Headless cover for movement. Ashley needs no GL context, so the thing I cannot
 * verify by playing — that a unit actually walks its path, and recovers when
 * stuck — is testable here.
 */
class BattleMovementSystemTest {

    private fun entityAt(
        position: Vector2,
        path: List<Vector2>,
        destination: Vector2?,
        size: CreatureSize = CreatureSize.SMALL
    ) = Entity().apply {
        add(CTransform(position.cpy(), 0f, 0.5f))
        add(
            CMovable(
                movementSpeed = 5f,
                destination = destination,
                path = Stack<Vector2>().apply { path.reversed().forEach { push(it) } },
                rotationSpeed = 600f,
                size = size
            )
        )
    }

    private fun step(engine: Engine, frames: Int, dt: Float = 1f / 60f) {
        repeat(frames) { engine.update(dt) }
    }

    @Test
    fun `a unit walks its path and arrives`() {
        val goal = Vector2(10f, 0f)
        val entity = entityAt(Vector2(0f, 0f), listOf(Vector2(5f, 0f), goal), goal)
        val engine = Engine().apply {
            addSystem(BattleMovementSystem({ false }))
            addEntity(entity)
        }

        step(engine, 240)

        val end = entity[CTransform.m()]!!.position
        assertTrue("did not arrive, ended at $end", end.dst(goal) < 0.5f)
    }

    @Test
    fun `arrival clears the destination so the unit stops`() {
        val goal = Vector2(3f, 0f)
        val entity = entityAt(Vector2(0f, 0f), listOf(goal), goal)
        val engine = Engine().apply {
            addSystem(BattleMovementSystem({ false }))
            addEntity(entity)
        }

        step(engine, 240)
        assertNull("destination should be cleared on arrival", entity[CMovable.m()]!!.destination)
    }

    @Test
    fun `a unit never ends a frame inside collision`() {
        // Wall across x >= 4. The mover must be stopped by it rather than
        // stepping through, however large the per-frame step is.
        val blocked = { p: Vector2 -> p.x >= 4f }
        val goal = Vector2(10f, 0f)
        val entity = entityAt(Vector2(0f, 0f), listOf(goal), goal)
        val engine = Engine().apply {
            addSystem(BattleMovementSystem(blocked))
            addEntity(entity)
        }

        repeat(240) {
            engine.update(1f / 60f)
            val p = entity[CTransform.m()]!!.position
            assertTrue("entered collision at $p", !blocked(p))
        }
    }

    @Test
    fun `being blocked triggers a re-path`() {
        // Without this the unit grinds against the obstacle forever, because
        // nothing else recomputes the route.
        var repathCalls = 0
        val detour = listOf(Vector2(4f, 5f), Vector2(10f, 5f))
        val goal = Vector2(10f, 0f)
        val entity = entityAt(Vector2(0f, 0f), listOf(goal), goal)

        val engine = Engine().apply {
            addSystem(
                BattleMovementSystem(
                    collidesAt = { p -> p.x >= 4f },
                    repath = { _, _, _ -> repathCalls++; detour }
                )
            )
            addEntity(entity)
        }

        step(engine, 240)

        assertTrue("expected a re-path while blocked, got $repathCalls", repathCalls > 0)
        assertNotNull("destination should survive a successful re-path", entity[CMovable.m()]!!.destination)
    }

    @Test
    fun `a re-path that finds nothing stops the unit instead of leaving it stuck`() {
        val goal = Vector2(10f, 0f)
        val entity = entityAt(Vector2(0f, 0f), listOf(goal), goal)
        val engine = Engine().apply {
            addSystem(
                BattleMovementSystem(
                    collidesAt = { p -> p.x >= 4f },
                    repath = { _, _, _ -> emptyList() }
                )
            )
            addEntity(entity)
        }

        step(engine, 240)

        assertNull("unreachable goal should clear the destination", entity[CMovable.m()]!!.destination)
        assertTrue("path should be emptied too", entity[CMovable.m()]!!.path.isEmpty())
    }

    @Test
    fun `an unobstructed unit never asks for a re-path`() {
        // Guards the blocked-frame counter against firing on ordinary movement.
        var repathCalls = 0
        val goal = Vector2(10f, 0f)
        val entity = entityAt(Vector2(0f, 0f), listOf(goal), goal)
        val engine = Engine().apply {
            addSystem(
                BattleMovementSystem(
                    collidesAt = { false },
                    repath = { _, _, _ -> repathCalls++; emptyList() }
                )
            )
            addEntity(entity)
        }

        step(engine, 240)
        assertEquals("clear ground should never trigger a re-path", 0, repathCalls)
    }
}
