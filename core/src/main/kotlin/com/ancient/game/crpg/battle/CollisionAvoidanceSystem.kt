package com.ancient.game.crpg.battle

import com.ancient.game.crpg.CTransform
import com.ancient.game.crpg.UserInputManager
import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family.all
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import ktx.ashley.get
import ktx.ashley.has
import ktx.ashley.mapperFor
import ktx.math.minus
import ktx.math.times
import kotlin.math.max

/**
 * Component that marks an entity as capable of avoiding collisions with other entities.
 * @param avoidanceRadius The radius around the entity to check for potential collisions
 * @param avoidanceStrength How strongly the entity should avoid collisions (higher values = stronger avoidance)
 */
data class CCollisionAvoidance(
    val avoidanceRadius: Float = 1.0f,
    val avoidanceStrength: Float = 1.0f
) : Component {
    companion object {
        fun m() = mapperFor<CCollisionAvoidance>()
    }
}

/**
 * System that handles entity-to-entity collision avoidance.
 * This system detects potential collisions between entities and calculates avoidance vectors.
 */
class CollisionAvoidanceSystem : IteratingSystem(
    all(
        CCollisionAvoidance::class.java,
        CMovable::class.java,
        CTransform::class.java
    )
    .exclude(CDead::class.java)
    .get()
) {
    private val collisionAvoidanceM: ComponentMapper<CCollisionAvoidance> = mapperFor()
    private val movableM: ComponentMapper<CMovable> = mapperFor()
    private val transformM: ComponentMapper<CTransform> = mapperFor()
    
    // Store calculated avoidance vectors for each entity
    private val avoidanceVectors: MutableMap<Entity, Vector2> = mutableMapOf()
    
    override fun update(deltaTime: Float) {
        // Clear previous avoidance vectors
        avoidanceVectors.clear()
        
        // Process each entity to calculate avoidance vectors
        super.update(deltaTime)
        
        // Apply avoidance vectors to entity positions
        avoidanceVectors.forEach { (entity, avoidanceVector) ->
            val movable = entity[movableM] ?: return@forEach
            
            // Only apply avoidance if the entity is moving
            if (movable.destination != null) {
                val transform = entity[transformM] ?: return@forEach
                
                // Apply the avoidance vector to the entity's position
                transform.position.add(avoidanceVector)
            }
        }
    }
    
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val dt = UserInputManager.deltaTime(deltaTime)
        val transform = entity[transformM] ?: return
        val collisionAvoidance = entity[collisionAvoidanceM] ?: return
        val movable = entity[movableM] ?: return
        
        // Only process entities that are moving
        if (movable.destination == null) return
        
        // Calculate avoidance vector for this entity
        val avoidanceVector = calculateAvoidanceVector(entity, collisionAvoidance, transform)
        
        // Store the avoidance vector for later application
        if (avoidanceVector.len2() > 0) {
            avoidanceVectors[entity] = avoidanceVector.scl(collisionAvoidance.avoidanceStrength * dt)
        }
    }
    
    /**
     * Calculate an avoidance vector for an entity based on nearby entities.
     */
    private fun calculateAvoidanceVector(
        entity: Entity,
        collisionAvoidance: CCollisionAvoidance,
        transform: CTransform
    ): Vector2 {
        val avoidanceVector = Vector2(0f, 0f)
        val entityPosition = transform.position
        val entityRadius = transform.radius
        
        // Check all other entities with transforms for potential collisions
        entities.forEach { otherEntity ->
            // Skip self
            if (otherEntity == entity) return@forEach
            
            // Skip dead entities
            if (otherEntity.has(CDead::class)) return@forEach
            
            val otherTransform = otherEntity[transformM] ?: return@forEach
            val otherPosition = otherTransform.position
            val otherRadius = otherTransform.radius
            
            // Calculate distance between entities
            val distanceVector = Vector2(entityPosition).sub(otherPosition)
            val distance = distanceVector.len()
            
            // Calculate minimum distance needed to avoid collision
            val minDistance = entityRadius + otherRadius + collisionAvoidance.avoidanceRadius
            
            // If entities are too close, calculate avoidance force
            if (distance < minDistance && distance > 0) {
                // Normalize the distance vector
                distanceVector.nor()
                
                // Scale avoidance force based on how close entities are
                // (closer = stronger avoidance)
                val avoidanceForce = max(0f, minDistance - distance) / minDistance
                
                // Add to the total avoidance vector
                avoidanceVector.add(distanceVector.scl(avoidanceForce))
            }
        }
        
        return avoidanceVector
    }
}
