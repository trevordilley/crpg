package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.equipment.Equipment
import com.ancient.game.crpg.equipment.Nothing
import com.ancient.game.crpg.systems.Allegiance
import com.ancient.game.crpg.systems.CSelectable
import com.ancient.game.crpg.systems.CTransform
import com.ancient.game.crpg.systems.CharacterSelect
import com.ancient.game.crpg.systems.HaulableSelect
import com.ancient.game.crpg.systems.SelectionSystem
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.has
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hauling loop: pick up loot or a fallen ally, drag them to a drop zone,
 * and either bank the treasure or resurrect the body.
 *
 * Note CAnimated cannot be constructed here — AnimationData builds a Sprite in
 * its constructor, which needs a GL context — so these exercise the systems
 * without animation components. That is deliberate cover for the null paths as
 * well as the logic.
 */
class HaulingTest {

    private fun hauler(at: Vector2) = Entity().apply {
        add(CTransform(at.cpy(), 0f, 0.5f))
    }

    private fun treasure(at: Vector2, value: Int = 100) = Entity().apply {
        add(CTransform(at.cpy(), 0f, 0.5f))
        add(CHaulable())
        add(CTreasure(value))
        add(CSelectable(kind = HaulableSelect))
    }

    private fun corpse(at: Vector2, playerControlled: Boolean = true) = Entity().apply {
        add(CTransform(at.cpy(), 0f, 0.5f))
        add(CCombatant(Player, Equipment(Nothing, Nothing, null)))
        add(CHealth(0, 100, 1, 0, 3))
        add(CSelectable(kind = CharacterSelect(Allegiance.PLAYER)))
        add(CDead())
        if (playerControlled) add(CPlayerControlled)
    }

    private fun dropZone(kind: DropZoneKind, bounds: Rectangle) = Entity().apply {
        add(CDropZone(kind, bounds))
    }

    // --- HaulableSystem ---------------------------------------------------

    @Test
    fun `a haulable follows its hauler once it lags behind`() {
        val system = HaulableSystem()
        val carrier = hauler(Vector2(10f, 0f))
        val loot = treasure(Vector2(0f, 0f))
        loot[CHaulable.m()]!!.hauler = carrier

        val engine = Engine().apply { addSystem(system); addEntity(loot) }
        repeat(60) { engine.update(1f / 60f) }

        val moved = loot[CTransform.m()]!!.position
        assertTrue("loot should have been dragged toward the hauler, at $moved", moved.x > 0f)
    }

    @Test
    fun `an unhauled item stays where it was dropped`() {
        val system = HaulableSystem()
        val loot = treasure(Vector2(3f, 4f))

        val engine = Engine().apply { addSystem(system); addEntity(loot) }
        repeat(60) { engine.update(1f / 60f) }

        assertEquals(Vector2(3f, 4f), loot[CTransform.m()]!!.position)
    }

    @Test
    fun `pickup only works within reach`() {
        val system = HaulableSystem()
        val carrier = hauler(Vector2(0f, 0f))

        val nearby = treasure(Vector2(0.3f, 0f))
        system.attemptToPickUp(carrier, nearby)
        assertNotNull("should pick up something within reach", nearby[CHaulable.m()]!!.hauler)

        val distant = treasure(Vector2(5f, 0f))
        system.attemptToPickUp(carrier, distant)
        assertNull("must not pick up across the map", distant[CHaulable.m()]!!.hauler)
    }

    @Test
    fun `dropping releases the item`() {
        val system = HaulableSystem()
        val carrier = hauler(Vector2(0f, 0f))
        val loot = treasure(Vector2(0.3f, 0f))

        system.attemptToPickUp(carrier, loot)
        system.drop(loot[CHaulable.m()]!!)

        assertNull(loot[CHaulable.m()]!!.hauler)
    }

    // --- DropZoneSystem ---------------------------------------------------

    @Test
    fun `treasure delivered to the cart is claimed and removed`() {
        val selection = SelectionSystem()
        val system = DropZoneSystem(selection)
        val loot = treasure(Vector2(1f, 1f), value = 250)
        val cart = dropZone(TreasureKind, Rectangle(0f, 0f, 4f, 4f))

        val engine = Engine().apply {
            addSystem(system)
            addEntity(cart)
            addEntity(loot)
        }
        engine.update(0f)

        assertEquals("loot value should be banked", 250, system.claimedValue)
        assertFalse("claimed loot should leave the world", engine.entities.contains(loot))
    }

    @Test
    fun `treasure outside the zone is left alone`() {
        val system = DropZoneSystem(SelectionSystem())
        val loot = treasure(Vector2(50f, 50f))
        val cart = dropZone(TreasureKind, Rectangle(0f, 0f, 4f, 4f))

        val engine = Engine().apply {
            addSystem(system)
            addEntity(cart)
            addEntity(loot)
        }
        engine.update(0f)

        assertEquals(0, system.claimedValue)
        assertTrue(engine.entities.contains(loot))
    }

    @Test
    fun `a body in the healing zone starts being healed`() {
        val system = DropZoneSystem(SelectionSystem())
        val body = corpse(Vector2(1f, 1f)).apply { add(CHaulable()) }
        val healer = dropZone(HealingKind, Rectangle(0f, 0f, 4f, 4f))

        val engine = Engine().apply {
            addSystem(system)
            addEntity(healer)
            addEntity(body)
        }
        engine.update(0f)

        assertTrue("body should be flagged as healing", body[CDead.m()]!!.beingHealed)
    }

    @Test
    fun `a healing zone does not consume the body`() {
        // Unlike treasure, a corpse must survive to be resurrected.
        val system = DropZoneSystem(SelectionSystem())
        val body = corpse(Vector2(1f, 1f)).apply { add(CHaulable()) }
        val healer = dropZone(HealingKind, Rectangle(0f, 0f, 4f, 4f))

        val engine = Engine().apply {
            addSystem(system)
            addEntity(healer)
            addEntity(body)
        }
        engine.update(0f)

        assertTrue(engine.entities.contains(body))
    }

    // --- DeadSystem -------------------------------------------------------

    @Test
    fun `a fallen ally becomes haulable so it can be dragged away`() {
        val body = corpse(Vector2(0f, 0f))
        val engine = Engine().apply {
            addSystem(DeadSystem(HaulableSystem()))
            addEntity(body)
        }
        engine.update(1f / 60f)

        assertTrue("the dead should be draggable", body.has(CHaulable.m()))
        assertTrue(
            "and selectable as cargo rather than as a character",
            body[CSelectable.m()]!!.kind is HaulableSelect
        )
    }

    @Test
    fun `enemy corpses are removed immediately rather than left to haul`() {
        val orc = corpse(Vector2(0f, 0f), playerControlled = false)
        val engine = Engine().apply {
            addSystem(DeadSystem(HaulableSystem()))
            addEntity(orc)
        }
        engine.update(1f / 60f)

        assertFalse(engine.entities.contains(orc))
    }

    @Test
    fun `an unhealed body eventually perma-dies`() {
        val body = corpse(Vector2(0f, 0f))
        val engine = Engine().apply {
            addSystem(DeadSystem(HaulableSystem()))
            addEntity(body)
        }
        // timeTillPermaDeath is 20s
        repeat(25) { engine.update(1f) }

        assertFalse("body should be gone after the perma-death timer", engine.entities.contains(body))
    }

    @Test
    fun `a body being healed does not perma-die`() {
        val body = corpse(Vector2(0f, 0f))
        val engine = Engine().apply {
            addSystem(DeadSystem(HaulableSystem()))
            addEntity(body)
        }
        engine.update(1f / 60f)
        body[CDead.m()]!!.beingHealed = true

        repeat(25) { engine.update(1f) }

        // Either resurrected (CDead removed) or still present — the one thing it
        // must not be is deleted while under care.
        assertTrue("a body under care must not be deleted", engine.entities.contains(body))
    }

    @Test
    fun `healing resurrects with full health and partial stamina`() {
        val body = corpse(Vector2(0f, 0f))
        val engine = Engine().apply {
            addSystem(DeadSystem(HaulableSystem()))
            addEntity(body)
        }
        engine.update(1f / 60f)
        body[CDead.m()]!!.beingHealed = true

        // timeTillRessurection is 5s
        repeat(7) { engine.update(1f) }

        assertFalse("should no longer be dead", body.has(CDead.m()))
        assertFalse("and no longer cargo", body.has(CHaulable.m()))
        assertEquals("health restored", 3, body[CHealth.m()]!!.health)
        assertEquals("revived at a quarter stamina", 25, body[CHealth.m()]!!.stamina)
        assertTrue(
            "and selectable as a character again",
            body[CSelectable.m()]!!.kind is CharacterSelect
        )
    }
}
