package com.example.assignment.leaderboard

import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.core.model.ScoreEvent
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultLeaderboardEngineTest {

    private fun event(userId: String, newScore: Long, username: String = userId) = ScoreEvent(
        userId = userId,
        username = username,
        delta = newScore,
        newScore = newScore,
        timestamp = 0L,
        sessionId = 1L,
    )

    @Test
    fun `never generates its own events - purely derives state from what it is given`() = runTest {
        val engine = DefaultLeaderboardEngine(dispatcher = StandardTestDispatcher(testScheduler))

        val emissions = engine.observe(flowOf()).toList()

        // With no input events at all, the only emission is the scan's empty seed - proof this
        // module produces nothing on its own; it strictly reacts to its input.
        assertEquals(listOf(emptyList<LeaderboardEntry>()), emissions)
    }

    @Test
    fun `derives a correctly ranked leaderboard from a stream of score events`() = runTest {
        val engine = DefaultLeaderboardEngine(dispatcher = StandardTestDispatcher(testScheduler))
        val events = flowOf(
            event("u1", newScore = 10),
            event("u2", newScore = 20),
            event("u3", newScore = 10),
        )

        val finalState = engine.observe(events).toList().last()

        assertEquals(listOf("u2", "u1", "u3"), finalState.map { it.userId })
        assertEquals(listOf(1, 2, 2), finalState.map { it.rank })
    }

    @Test
    fun `does not re-emit when the computed ranking has not actually changed`() = runTest {
        val engine = DefaultLeaderboardEngine(dispatcher = StandardTestDispatcher(testScheduler))
        val firstUpdate = event("u1", newScore = 10)
        val duplicateOfFirstUpdate = firstUpdate.copy()

        val emissions = engine.observe(flowOf(firstUpdate, duplicateOfFirstUpdate)).toList()

        // Expect exactly 2 emissions: the scan's empty seed, then the one real ranking change.
        // The duplicate event folds into an identical map -> identical ranked list ->
        // distinctUntilChanged suppresses the would-be third, "flickering" emission.
        assertEquals(2, emissions.size)
        assertEquals(listOf("u1"), emissions.last().map { it.userId })
    }

    @Test
    fun `re-ranks as scores overtake each other in real time`() = runTest {
        val engine = DefaultLeaderboardEngine(dispatcher = StandardTestDispatcher(testScheduler))
        val events = flowOf(
            event("leader", newScore = 100),
            event("chaser", newScore = 50),
            event("chaser", newScore = 150), // chaser overtakes leader
        )

        val finalState = engine.observe(events).toList().last()

        assertEquals(listOf("chaser", "leader"), finalState.map { it.userId })
        assertEquals(listOf(1, 2), finalState.map { it.rank })
    }
}
