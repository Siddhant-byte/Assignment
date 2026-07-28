package com.example.assignment.core.model

/**
 * A single, immutable "gameplay -> score" fact emitted by a score source (e.g. the
 * [com.example.assignment.engine] module's fake match engine, or in production a real
 * WebSocket-backed backend).
 *
 * This is the *only* type shared between the `:engine` (producer) and `:leaderboard`
 * (consumer) modules. Neither module depends on the other directly - both depend on this
 * `:core` model - which is what keeps them independent per the assignment's Part 1 requirement.
 *
 * @property userId stable identifier of the player whose score changed.
 * @property username display name of the player.
 * @property delta the (always positive) amount the player's score increased by in this event.
 * @property newScore the player's total score *after* applying [delta]. Scores only ever increase.
 * @property timestamp epoch millis when the event was generated, useful for ordering/debugging.
 * @property sessionId identifies the generator session that produced this event. Combined with a
 * seeded [kotlin.random.Random] in the engine, replaying the same [sessionId]/seed reproduces the
 * exact same sequence of events - this is what "deterministic per session" means in Part 1.
 */
data class ScoreEvent(
    val userId: String,
    val username: String,
    val delta: Long,
    val newScore: Long,
    val timestamp: Long,
    val sessionId: Long,
)
