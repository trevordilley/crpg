package com.ancient.game.crpg.systems

import com.badlogic.ashley.core.Entity
import ktx.ashley.get
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `selection` is not just a UI nicety — `BattleCommandSystem` takes
 * `selection.firstOrNull()` to decide which character picks up a haulable, so
 * anything stale in the list becomes a wrong gameplay decision.
 */
class SelectionSystemTest {

    private fun character() = Entity().apply {
        add(CSelectable(kind = CharacterSelect(Allegiance.PLAYER)))
    }

    private fun haulable() = Entity().apply {
        add(CSelectable(kind = HaulableSelect))
    }

    @Test
    fun `selecting a character puts it in the selection`() {
        val system = SelectionSystem()
        val e = character()
        system.select(e)

        assertTrue(system.selection.contains(e))
        assertTrue(e[CSelectable.m()]!!.selected)
    }

    @Test
    fun `deselecting removes from the selection list, not just the flag`() {
        // Regression: deselect() used to flip the component flags but leave the
        // entity in characterSelection, so `selection` kept returning it.
        val system = SelectionSystem()
        val e = character()
        system.select(e)
        system.deselect(e)

        assertFalse("flag cleared", e[CSelectable.m()]!!.selected)
        assertFalse("but it was still in the selection list", system.selection.contains(e))
    }

    @Test
    fun `a deselected character is never the one chosen to haul`() {
        // The bug with teeth: HealthSystem deselects on death, and
        // BattleCommandSystem hauls with selection.firstOrNull(). If the dead
        // stayed in the list, a corpse could be ordered to pick up the treasure.
        val system = SelectionSystem()
        val dead = character()
        val alive = character()

        system.select(dead)
        system.select(alive)
        system.deselect(dead)

        assertNotEquals("a deselected entity must not be the hauler", dead, system.selection.firstOrNull())
        assertEquals(alive, system.selection.firstOrNull())
    }

    @Test
    fun `selecting a new character replaces the previous selection`() {
        val system = SelectionSystem()
        val first = character()
        val second = character()

        system.select(first)
        system.select(second)

        assertEquals(listOf(second), system.selection)
        assertFalse("the replaced character should be deselected", first[CSelectable.m()]!!.selected)
    }

    @Test
    fun `deselectAll empties the selection`() {
        val system = SelectionSystem()
        val e = character()
        system.select(e)
        system.deselectAll()

        assertTrue(system.selection.isEmpty())
        assertFalse(e[CSelectable.m()]!!.selected)
    }

    @Test
    fun `haulables are tracked separately from characters`() {
        // Selecting treasure must not clear the party, or clicking loot would
        // silently drop your command selection.
        val system = SelectionSystem()
        val char = character()
        val loot = haulable()

        system.select(char)
        system.select(loot)

        assertTrue("character selection should survive selecting a haulable", system.selection.contains(char))
    }
}
