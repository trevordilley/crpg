# CRPG Game
Mix Lords of Magic's site based dungeon overworld 
with Baldur's Gate style combat with a Dark Souls stamina 
management twist!

## Key Distinctions
* CRPG style combat, but less forgiving
    * Position and rotation are very important, getting shot in the back is real bad
    * Stamina dictates everything
    * Pause and plan style 

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

#### Dungeon
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
* Sound
    * Ambient drops of water and other
    spooky dungeons soudns
* Moving makes noice, causing little
graphical things to happen to symbolize
hearing stuff. This helps if we do
FOV based on entities
* Enemy AI based on FOV  
* Loot & "Mules"
    * Mules are characters that are non-combatants which
    move fast and can carry loot. Could also be player
    characters. 
    * Loot will be a selectable object that PC's can pick up
    * To "earn/claim" the loot, must be brought back to "camp"
* Health pips on character
* Make entities collide
* Attacks are determined by clicking on another entity
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

