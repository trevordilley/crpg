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

## TODO's by priority 

### Battle

MVP:
 - Combat is based on stamina, position, rotation, and speed
 - As a player entities with shields are dramatically more durable when their shields are up, but they move slower
 - As a player I can pause the game at any time and command my entities
 - As a player my entities can find their way intelligently through a dungeon
 - As a player I should be rewarded for taking advantage of bottle necks in dungeon, placing entities behind columns to avoid
 ranged attacks and Line of Site
 - As a player I should have entities that are faster or slower than others based on their gear and skills
 - As a player I should have melee and ranged weapons
 - As a player I should have a dungeon to explore
 - As a player I have enemies that are both melee and ranged, fast and slow
 - Entities should not overlap with one another. 
 - As a player I can find loot and must carry it out
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
* Polish
    * Fix rotation bug 
    * Optimize FOV 
        1. optimizing tile polygons
        2. Parallelize polygon calculations
    * Fix going around corners
        
        
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
* Populate with random sites


### Random Thoughts

#### Arbitrary Polyigonal Dungeons/Environments
* Create dungeons using marching cubes + perlin noise + other prng
* Use voronoi + delaunay to determine pathing maps. More dense amounts
of nodes will create denser maps which should allow pathing through arbitrary
dungeons. Create a delaunay triangulation to connect the nodes,  walk each node and 
mark them as within or outside collision polygons, and deconnect all nodes
within the collision polygons. 

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
