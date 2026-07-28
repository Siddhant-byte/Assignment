package com.example.assignment.engine

import com.example.assignment.core.model.ScoreEvent
import kotlinx.coroutines.flow.Flow

/**
 * Anything that can produce a live stream of [ScoreEvent]s.
 *
 * The `:leaderboard` module (and the `:app` composition root) depend on *this* abstraction,
 * never on [RandomScoreEngine] directly. That's the seam that keeps the two "independent
 * modules" from Part 1 actually decoupled: swapping the fake in-memory generator for a real
 * WebSocket-backed backend later means writing one new `ScoreEventSource` implementation and
 * changing a single line in the composition root - zero changes to `:leaderboard` or to the
 * `LeaderboardViewModel`.
 */
interface ScoreEventSource {
    val events: Flow<ScoreEvent>
}
