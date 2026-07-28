package com.example.assignment.leaderboard

/**
 * Internal running total for one user, folded up from the incoming [com.example.assignment.core.model.ScoreEvent]
 * stream. Not exposed outside this module - callers only ever see the derived [com.example.assignment.core.model.LeaderboardEntry]
 * list produced by [RankingRules].
 */
internal data class ScoreSnapshot(
    val userId: String,
    val username: String,
    val score: Long,
)
