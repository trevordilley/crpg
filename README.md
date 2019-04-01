# CRPG Game
Mix Lords of Magic's site based dungeon overworld 
with Baldur's Gate style combat with a Dark Souls stamina 
management twist!

## Key Distinctions
* CRPG style combat, but less forgiving
    * Position and rotation are very important, getting shot in the back is real bad
    * Stamina dictates everything
    * Pause and plan style 
    * IDEA: Think about Nox gameplay, and 
    see if it can kinda scale out to RTS level scale. 
    Individually determine when units will attack, shoot, dodge roll, or be blocking.

## TODO's by priority 

### Battle
* Rotation mechanics
  * When moving, different kinds of speeds affect how you rotate. If you command a PC to move to a point it may or may not
  need to rotate toward that point based on how fast it moves. 
    * Walking: Can be rotated any direction while moving in any direction
      * Things that involve walking are having shields up and sneaking
    * Running: Can be rotated in any direction within a 180 degree arc centered on the direction of the run
      * Examples: Running and gunning
    * Sprint/Charging: Can be rotated in any direction within a 30-45 degree arc center on the direction of the sprint. 
      * Example: Charging! Running like this burns stamina.
  * TODO: Smooth rotation
  * TODO: Different movement speeds
  * TODO: Rotate based on movement speed rules
* Shield arc-angle implemented
  * Blue arc in the front of the character with variable range
  * Zero stamina means the shield arc drops until some stamina recovers. 
* Stamina represented (perhaps change the color of the sprite to red as it runs out, then blinking when they're out of stamina?)
  * A bar may be what we want in the future but it is probably more work to implement?
  * Stamina recovers at some rate. 
* Add another player entity, implement selecting differnt entities
* Make entities collide
* Entities can attack each other on a cool-down for Melee attacks, costs stamina
* Attacks are determined by clicking on another entity
* Hitting an entity against the shield arc costs stamina. 
* Health pips on character
* Hitting against a side not covered by shield arc hits pips


### Overworld
* Map scene shows
* Place party
* Move party
    * LOW: Moving party drains stamina
    * LOW: Needs rest when stamina is drained
    * LOW: Stamina drains slower near familiar
    territory. 
* Populate with random sites
 
