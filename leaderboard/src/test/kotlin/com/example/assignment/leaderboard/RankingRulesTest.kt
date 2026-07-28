package com.example.assignment.leaderboard

import com.example.assignment.core.model.LeaderboardEntry
import kotlin.test.assertEquals
import org.junit.Test

class RankingRulesTest {

    private fun snapshot(userId: String, score: Long, username: String = userId) =
        ScoreSnapshot(userId = userId, username = username, score = score)

    @Test
    fun `empty input produces an empty leaderboard`() {
        assertEquals(emptyList<LeaderboardEntry>(), RankingRules.rank(emptyList()))
    }

    @Test
    fun `single player is rank 1`() {
        val result = RankingRules.rank(listOf(snapshot("u1", 50)))

        assertEquals(1, result.single().rank)
    }

    @Test
    fun `sorts strictly by score descending when there are no ties`() {
        val result = RankingRules.rank(
            listOf(snapshot("low", 10), snapshot("high", 100), snapshot("mid", 50)),
        )

        assertEquals(listOf("high", "mid", "low"), result.map { it.userId })
        assertEquals(listOf(1, 2, 3), result.map { it.rank })
    }

    @Test
    fun `equal scores share the same rank and the next rank skips accordingly`() {
        // 100, 90, 90, 80 -> ranks 1, 2, 2, 4 (standard competition ranking)
        val result = RankingRules.rank(
            listOf(snapshot("a", 100), snapshot("b", 90), snapshot("c", 90), snapshot("d", 80)),
        )

        assertEquals(listOf(1, 2, 2, 4), result.map { it.rank })
        assertEquals(listOf("a", "b", "c", "d"), result.map { it.userId })
    }

    @Test
    fun `three-way tie skips the following rank by three`() {
        // 50, 50, 50, 40 -> ranks 1, 1, 1, 4
        val result = RankingRules.rank(
            listOf(snapshot("a", 50), snapshot("b", 50), snapshot("c", 50), snapshot("d", 40)),
        )

        assertEquals(listOf(1, 1, 1, 4), result.map { it.rank })
    }

    @Test
    fun `ties are ordered deterministically by userId to avoid cosmetic flicker`() {
        val first = RankingRules.rank(listOf(snapshot("zeta", 10), snapshot("alpha", 10)))
        val second = RankingRules.rank(listOf(snapshot("zeta", 10), snapshot("alpha", 10)))

        assertEquals(listOf("alpha", "zeta"), first.map { it.userId })
        assertEquals(first, second, "repeated ranking of the same tied input must be identical")
    }

    @Test
    fun `all players tied at zero still rank correctly`() {
        val result = RankingRules.rank(listOf(snapshot("a", 0), snapshot("b", 0), snapshot("c", 0)))

        assertEquals(listOf(1, 1, 1), result.map { it.rank })
    }
}
