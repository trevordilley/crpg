# CRPG Game
Mix Lords of Magic's site based dungeon overworld 
with Baldur's Gate style combat with a Dark Souls stamina 
management twist!

## Key Distinctions
* CRPG style combat, but less forgiving
    * Position and rotation are very important, getting shot in the back is real bad
    * Stamina dictates everything
    * Pause and plan style 

## Asset Curation/Creation
Tilesets ripped from Google Image search

TileSet management using Tiled
https://www.mapeditor.org/

Sprite/Animation management using Aseprite
https://www.aseprite.org/

## Current Tasks
- On-Hit effect (flash white, something a character is damaged.)
- Smoother camera movement
- Multi-select rectangle when dragging mouse. 
- Sound effects, background music.
- Ranged attacks
- fix character rotation, sometimes they take the long way
- Enemy AI is FoV based
- Pretty things like dust clouds
- Implement hauling system. Something being hauled follows behind the hauler. 
    - Clicking on a hauler, then clicking on something haulable within range will attach
    the haulable entity to the hauler. Will follow if the hauler moves
    - Clicking on an attached haulable entity will detach it from the hauler
    - Things that could be hauled:
        - Loot
        - Downed NPCs
- *Requires Hauling Implemented* When an NPC runs out of Hit Points, it's now "DOWN" and has a circular timer start. When the time runs out they
die for good. Enemies will attack other targets unless that's the only target in range. The downed NPC cannot move
or do anything else. They become `haulable` though, and when a friendly NPC starts to `haul` the downed character
the countdown slows dramatically. They will eventually be able to be restored if they are hauled to a healer. 
   

## TODO's by priority 

### BUGS 

- Smoother Camera movement. When using the WASD or arrow keys to move the camera, move a _support point_ rather than 
the camera's position itself. Instead have the camera _chase the support point_, speeding up the farther away it is from
the support point and smoothly slowing down as it gets closer. 

### Improvements

### Battle

MVP:
 x Combat is based on stamina, position, rotation, and speed
 x As a player entities with shields are dramatically more durable when their shields are up, but they move slower
 x As a player I can pause the game at any time and command my entities
 x As a player my entities can find their way intelligently through a dungeon
 x As a player I should be rewarded for taking advantage of bottle necks in dungeon, placing entities behind columns to avoid
 ranged attacks and Line of Site
 - As a player I should have entities that are faster or slower than others based on their gear and skills
 - As a player I should have melee and ranged weapons
 - As a player I should have a dungeon to explore
 - As a player I have enemies that are both melee and ranged, fast and slow
 - Entities should not overlap with one another. 
 - As a player I can find loot and must carry it out
    - Implementation: A "haulable" thing will follow behind the entity carrying it.
    - Loot near a wagon is added to the wagon and will be carried out, the loot is *claimed* 
 - As a player, the light level in a dungeon or area plays an important tactical role
    
#### Dungeon
* UI Layer
    * Fix shapes showing direction of characters
    * Fix pathing line
    * Mouse over character shows their data (Gear with numbers) 
    * Flavor text messages (like in Nox, nice white floating text that disappears)
* Ranged attacks
* Enemy AI based on FOV  
* Loot & "Mules"
    * Mules are characters that are non-combatants which
    move fast and can carry loot. Could also be player
    characters. 
    * Loot will be a selectable object that PC's can pick up
    * To "earn/claim" the loot, must be brought back to "camp"
    * Some loot isn't actual materials. Characters can also find 
    "things of interest" such as veins of ore, fresh springs of water, arcane
    writings, and scrolls/maps to other locations and knowledge. 
    `for demo this can just be flavor text`
* Health pips on character
* Make entities collide
* Attacks are determined by clicking on another entity
* Sound
    * Ambient drops of water and other
    spooky dungeons sounds
    * Moving makes noise, causing little
    graphical things to happen to symbolize
    hearing stuff. This helps if we do
    FOV based on entities
##### Polish
  - [ ] Clicking on a PC
  stamina circle, vulnerability circle, life pips all fade in and scale to correct size.
  - [ ] Clicking away from a PC (deselecting)
  stamina circle, vulnerability circle, life pips all fade out and shrink a little
  - [ ] Clicking for a PC to move
  line grows along path, on reaching destination a circle grows very quickly beyond target radius and smoothly shrinks to proper radius. Circle appears to be spinning
  - [ ] Setting PC rotation
  Draw line from anchor point to mouse, upon release line glows and dissappears
  - [ ] Clicking on an enemy (PC to attack)
  Same as other line, but different aggressive color
  - [ ] PC moving
  Small dust clouds, maybe tiny distortion representing noise?
  - [ ] PC and NPC attacking
  Attack animations, sounds
  - [ ] PC and NPC getting hit
  Damage animations, changing color to red, blood splatter
  - [ ] Camera movement smoothed out
  Immediately moves when the player pushes a direction, but slows down a little as it reaches the destination.
  - [ ] Fix going around corners
        
        
### Demo Battle Sceen
30 x 30 dungeon
party: 
    2 fighters with shields
    2 fighter with great sword
    2 fighters with bows
    4 "scouts" (fast, no armor or shields, low damage)
    
Starting location has a fire and tents

Dungeon has:
    These bad guys: 
        goblins: Fast but very weak. Will kill you if they sneak up
        shield orcs: Have shields, moderate damage. Can hold the line
        axe orcs: Big axes, no shields. Can be dangerous unless held back with shield
        archer orcs: Shoot arrows
        big orc: Big Axe, big shield, must be surrounded to safely dispatch. Multiple health pips. 
    And these dungeon features:
        Big open room
        Narrow hallway with scary corners
        Room with many pillars
        Treasure in several rooms
        Room that is a circular hallway
        Big boss room with big orc and big treasure
    Players should find (`probably with just flavor text for now`)
        Coins
        Gems
        Big statue
        Map to another location
        Vein of ore
        Spring of fresh water
        Arcane writings that need to be scribbled down
### Overworld
* Map scene shows
* Place party
* Move party
    * LOW: Moving party drains stamina
    * LOW: Needs rest when stamina is drained
    * LOW: Stamina drains slower near familiar
    territory. 
* Populate with features
  * Forests
  * Mountains
  * Plains
  * Rivers
* Populate with random sites
Sites are either friendly or unfriendly.

If a site is friendly, and it has valuable resources near it
(stuff that was discovered during an adventure) then 
towns will grow roads towards it. Once the road connects 
to the site, it get's access to those resources. 
  * Dungeons
  * Towns
    * Roads between towns?


#### Future
Shorter roads lead to more effcient extraction (cheaper markets?)

If a road becomes threatened, then the resource can either disappear
or become more expensive. 

* Based on resources brought to a town, different goods are available
to purchase at town
* Towns have markets by the way! 


### Random Thoughts


## Future
* Magic
    * Casting a spell drains all stamina?
    
* Level up system!
    * For Demo
        * 1000 points per level
        * Increases stamina
        * Goblin gives 50xp
        * Orc gives 100xp
        * Big orc gives 300xp 

## Maybe/Future

* Rotation mechanics
  * When moving, different kinds of speeds affect how you rotate. If you command a PC to move to a point it may or may not
  need to rotate toward that point based on how fast it moves. 
    * Running: Can be rotated in any direction within a 180 degree arc centered on the direction of the run
    Default movement speed.
      * Examples: Running and gunning
    * Sprint/Charging: Can be rotated in any direction within a 30-45 degree arc center on the direction of the sprint. 
      * Example: Charging! Running like this burns stamina.
  * TODO: Different movement speeds
  * TODO: Rotate based on movement speed rules

## DONE

* Line of Sight
    * References
        * https://www.redblobgames.com/articles/visibility/
        * https://ncase.me/sight-and-light/
        * http://saltares.com/blog/games/2d-vision-system-with-ashley-and-box2d/
    * Need to convert tiles to polygons
    * Find all polygons in site radius of each unit and enemy
    * Grab all lines on polygons
    * Execute above algorithms
    * Field-of-view based on direction
    entity is facing?  
* Optimize FOV 
    1. optimizing tile polygons
    2. Parallelize polygon calculations (UNNECESARRY)
