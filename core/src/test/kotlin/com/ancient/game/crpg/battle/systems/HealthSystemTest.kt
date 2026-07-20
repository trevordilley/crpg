package com.ancient.game.crpg.battle.systems

import com.ancient.game.crpg.equipment.Armor
import com.ancient.game.crpg.equipment.Equipment
import com.ancient.game.crpg.equipment.Nothing
import com.ancient.game.crpg.equipment.Shield
import com.ancient.game.crpg.systems.Allegiance
import com.ancient.game.crpg.systems.CSelectable
import com.ancient.game.crpg.systems.CTransform
import com.ancient.game.crpg.systems.CharacterSelect
import com.ancient.game.crpg.systems.SelectionSystem
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.has
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cover for the combat model — stamina as the damage buffer, shields reducing
 * within an arc, and hits from behind bypassing stamina to reach health.
 *
 * This is the "position and rotation matter, being shot in the back is really
 * bad" design from the README, and it had no tests at all. None of it needs a GL
 * context.
 *
 * Rotation convention: an entity faces along its `rotation` in degrees, and the
 * damage angle is measured from the defender to the damage origin. So an origin
 * placed at +x of a defender rotated to 0 is a frontal hit.
 */
class HealthSystemTest {

    private fun defender(
        rotation: Float = 0f,
        stamina: Int = 100,
        health: Int = 3,
        shield: Shield? = null,
        armor: Armor? = null,
        rechargeRate: Int = 1
    ) = Entity().apply {
        add(CTransform(Vector2(0f, 0f), rotation, 0.5f))
        add(
            CCombatant(
                Player,
                Equipment(shield ?: Nothing, Nothing, armor)
            )
        )
        add(CHealth(stamina, 100, rechargeRate, health, 3))
    }

    private fun engineWith(entity: Entity): Engine = Engine().apply {
        addSystem(HealthSystem(SelectionSystem()))
        addEntity(entity)
    }

    /** Origin directly in front of a defender facing 0 degrees. */
    private val fromFront = Vector2(10f, 0f)

    /** Origin directly behind. */
    private val fromBehind = Vector2(-10f, 0f)

    @Test
    fun `a frontal hit is absorbed by stamina and leaves health alone`() {
        val e = defender(stamina = 100, health = 3)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 30, originPosition = fromFront))

        engineWith(e).update(0f)

        assertEquals("stamina should absorb the hit", 70, e[CHealth.m()]!!.stamina)
        assertEquals("health must be untouched while stamina remains", 3, e[CHealth.m()]!!.health)
    }

    @Test
    fun `a hit from behind bypasses stamina and costs health`() {
        // The core of the design: getting hit in the back is meant to hurt even
        // at full stamina.
        val e = defender(stamina = 100, health = 3)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 30, originPosition = fromBehind))

        engineWith(e).update(0f)

        assertEquals("stamina should be untouched by a back attack", 100, e[CHealth.m()]!!.stamina)
        assertEquals("a back attack should cost health directly", 2, e[CHealth.m()]!!.health)
    }

    @Test
    fun `once stamina is gone frontal hits reach health too`() {
        val e = defender(stamina = 0, health = 3, rechargeRate = 0)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 30, originPosition = fromFront))

        engineWith(e).update(0f)

        assertEquals(2, e[CHealth.m()]!!.health)
    }

    @Test
    fun `a shield reduces damage inside its arc but not outside it`() {
        val shield = Shield("Test Shield", damagePercentReduction = 0.5f, protectionArc = 90f)

        val guarded = defender(stamina = 100, shield = shield)
        guarded[CHealth.m()]!!.damages.add(Damage(stamina = 40, originPosition = fromFront))
        engineWith(guarded).update(0f)

        val unguarded = defender(stamina = 100)
        unguarded[CHealth.m()]!!.damages.add(Damage(stamina = 40, originPosition = fromFront))
        engineWith(unguarded).update(0f)

        assertEquals("50% reduction inside the arc", 80, guarded[CHealth.m()]!!.stamina)
        assertEquals("no reduction without a shield", 60, unguarded[CHealth.m()]!!.stamina)
    }

    @Test
    fun `a shield does not help against a hit outside its arc`() {
        // 90 degree arc facing 0, so a hit from 90 degrees off is outside it but
        // still within the 180 degree front arc, i.e. stamina still absorbs it.
        val shield = Shield("Test Shield", damagePercentReduction = 0.5f, protectionArc = 90f)
        val e = defender(stamina = 100, shield = shield)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 40, originPosition = Vector2(0f, 10f)))

        engineWith(e).update(0f)

        assertEquals("shield should not reduce a hit from the flank", 60, e[CHealth.m()]!!.stamina)
    }

    @Test
    fun `armor subtracts flat damage`() {
        val e = defender(stamina = 100, armor = Armor("Plate", damageReduction = 10, speedCostPercent = 0f))
        e[CHealth.m()]!!.damages.add(Damage(stamina = 30, originPosition = fromFront))

        engineWith(e).update(0f)

        assertEquals("30 damage less 10 armor", 80, e[CHealth.m()]!!.stamina)
    }

    @Test
    fun `stamina floors at zero rather than going negative`() {
        val e = defender(stamina = 10, rechargeRate = 0)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 999, originPosition = fromFront))

        engineWith(e).update(0f)

        assertEquals(0, e[CHealth.m()]!!.stamina)
    }

    @Test
    fun `breaking stamina locks out recharge for a while`() {
        // Otherwise a broken guard would refill instantly and the stamina model
        // would not punish anything.
        val e = defender(stamina = 10, rechargeRate = 5)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 999, originPosition = fromFront))

        val engine = engineWith(e)
        engine.update(0f)
        assertTrue("expected a recharge lockout", e[CHealth.m()]!!.staminaNotRechargingForSeconds > 0f)

        // Well past the recharge interval, but inside the lockout.
        repeat(5) { engine.update(0.11f) }
        assertEquals("stamina must not recharge during the lockout", 0, e[CHealth.m()]!!.stamina)
    }

    @Test
    fun `stamina recharges once the lockout expires`() {
        val e = defender(stamina = 50, rechargeRate = 2)
        val engine = engineWith(e)

        repeat(10) { engine.update(0.11f) }

        assertTrue(
            "stamina should climb back, was ${e[CHealth.m()]!!.stamina}",
            e[CHealth.m()]!!.stamina > 50
        )
    }

    @Test
    fun `stamina never exceeds its maximum`() {
        val e = defender(stamina = 99, rechargeRate = 10)
        val engine = engineWith(e)

        repeat(50) { engine.update(0.11f) }

        assertTrue(
            "stamina overflowed to ${e[CHealth.m()]!!.stamina}",
            e[CHealth.m()]!!.stamina <= e[CHealth.m()]!!.maxStamina
        )
    }

    @Test
    fun `running out of health marks the entity dead`() {
        val e = defender(stamina = 0, health = 0, rechargeRate = 0)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 10, originPosition = fromFront))

        engineWith(e).update(0f)

        assertTrue("entity should be marked dead", e.has(CDead.m()))
    }

    @Test
    fun `the dead are deselected so they cannot be commanded`() {
        val selection = SelectionSystem()
        val e = defender(stamina = 0, health = 0, rechargeRate = 0)
        e.add(CSelectable(kind = CharacterSelect(Allegiance.PLAYER)))
        selection.select(e)
        assertTrue("test premise: entity starts selected", selection.selection.contains(e))

        e[CHealth.m()]!!.damages.add(Damage(stamina = 10, originPosition = fromFront))
        Engine().apply {
            addSystem(HealthSystem(selection))
            addEntity(e)
        }.update(0f)

        assertFalse("dead entities must not stay selected", selection.selection.contains(e))
    }

    @Test
    fun `damage is consumed so one hit is not applied twice`() {
        val e = defender(stamina = 100)
        e[CHealth.m()]!!.damages.add(Damage(stamina = 30, originPosition = fromFront))

        val engine = engineWith(e)
        engine.update(0f)
        val afterFirst = e[CHealth.m()]!!.stamina
        engine.update(0f)

        assertTrue("damage queue should be cleared", e[CHealth.m()]!!.damages.isEmpty())
        assertEquals("the same hit must not land twice", afterFirst, e[CHealth.m()]!!.stamina)
    }
}
