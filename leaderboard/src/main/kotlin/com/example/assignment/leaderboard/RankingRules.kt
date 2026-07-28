package com.example.assignment.leaderboard

import com.example.assignment.core.model.LeaderboardEntry

/**
 * Pure ranking algorithm - the "business rules" half of Part 1's Leaderboard module, deliberately
 * factored out of [DefaultLeaderboardEngine] so it can be unit-tested with plain data, with no
 * coroutines/Flow machinery involved at all.
 *
 * Rules (as specified by the assignment):
 * - Sorted by score, descending.
 * - Equal scores share the same rank ("same score -> same rank").
 * - The next distinct score's rank skips ahead by the number of tied entries above it
 *   (standard competition ranking, e.g. scores 100/90/90/80 -> ranks 1/2/2/4).
 *
 * Ties are additionally broken by ascending [ScoreSnapshot.userId] purely to keep row *order*
 * stable and deterministic across recomputations (their `rank` value is still equal, per the
 * rule above) - this avoids cosmetic reordering/flicker among tied players when the underlying
 * map's iteration order isn't guaranteed.
 */
internal object RankingRules {

    fun rank(snapshots: Collection<ScoreSnapshot>): List<LeaderboardEntry> {
        val sorted = snapshots.sortedWith(
            compareByDescending<ScoreSnapshot> { it.score }.thenBy { it.userId },
        )

        var currentRank = 0
        var previousScore: Long? = null

        return sorted.mapIndexed { index, snapshot ->
            val position = index + 1 // 1-based
            if (previousScore == null || snapshot.score != previousScore) {
                currentRank = position
            }
            previousScore = snapshot.score

            LeaderboardEntry(
                rank = currentRank,
                userId = snapshot.userId,
                username = snapshot.username,
                score = snapshot.score,
            )
        }
    }
}
