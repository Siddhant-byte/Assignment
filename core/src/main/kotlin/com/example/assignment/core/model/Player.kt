package com.example.assignment.core.model

/**
 * A player participating in a match. Deliberately minimal - the engine only needs enough
 * identity to attribute score events to someone; anything richer (avatars, country, etc.)
 * belongs to a real backend/profile service, not this assignment's scope.
 */
data class Player(
    val id: String,
    val username: String,
)
