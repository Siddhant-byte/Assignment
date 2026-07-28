package com.example.assignment.leaderboard

import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.core.model.ScoreEvent
import kotlinx.coroutines.flow.Flow

/**
 * Leaderboard Module (Part 1, Module 2) - the *consumer* side of the pipeline.
 *
 * Deliberately shaped as "any `Flow<ScoreEvent>` in, `Flow<List<LeaderboardEntry>>` out": this
 * module has no compile-time dependency on `:engine` whatsoever (see this module's
 * `build.gradle.kts` - it only depends on `:core`), so it is structurally impossible for it to
 * import [com.example.assignment.engine.RandomScoreEngine] or generate scores itself. It only
 * ever reacts to whatever [ScoreEvent]s it is handed.
 */
interface LeaderboardEngine {
    fun observe(events: Flow<ScoreEvent>): Flow<List<LeaderboardEntry>>
}
