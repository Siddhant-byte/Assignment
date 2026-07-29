package com.example.assignment.engine

import com.example.assignment.core.model.Player
import com.example.assignment.core.model.ScoreEvent
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RandomScoreEngineTest {

    private val players = listOf(
        Player(id = "u1", username = "Alice"),
        Player(id = "u2", username = "Bob"),
        Player(id = "u3", username = "Cara"),
    )

    /** Collects exactly [count] events from [flow-like] engine, then cancels the collecting job. */
    private suspend fun collectN(
        engine: RandomScoreEngine,
        count: Int,
    ) = kotlinx.coroutines.coroutineScope {
        val collected = mutableListOf<ScoreEvent>()
        val job = launch {
            engine.events.collect { event ->
                collected += event
                if (collected.size == count) cancel()
            }
        }
        job.join()
        collected
    }

    @Test
    fun `same seed produces an identical deterministic event sequence`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engineA = RandomScoreEngine(players, seed = 42L, dispatcher = dispatcher, clock = { 0L })
        val engineB = RandomScoreEngine(players, seed = 42L, dispatcher = dispatcher, clock = { 0L })

        val eventsA = collectN(engineA, 20)
        val eventsB = collectN(engineB, 20)

        assertEquals(eventsA, eventsB, "same seed must replay the exact same event sequence")
    }

    @Test
    fun `different seeds produce different event sequences`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engineA = RandomScoreEngine(players, seed = 1L, dispatcher = dispatcher, clock = { 0L })
        val engineB = RandomScoreEngine(players, seed = 2L, dispatcher = dispatcher, clock = { 0L })

        val eventsA = collectN(engineA, 20)
        val eventsB = collectN(engineB, 20)

        assertNotEquals(eventsA, eventsB)
    }

    @Test
    fun `a user's score only ever increases`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RandomScoreEngine(players, seed = 7L, dispatcher = dispatcher, clock = { 0L })

        val lastScore = mutableMapOf<String, Long>()
        for (event in collectN(engine, 50)) {
            val previous = lastScore[event.userId] ?: 0L
            assertTrue(
                event.newScore > previous,
                "expected ${event.userId}'s score to strictly increase from $previous, got ${event.newScore}",
            )
            assertTrue(event.delta > 0, "delta must always be positive")
            lastScore[event.userId] = event.newScore
        }
    }

    @Test
    fun `targets random users across the full player pool`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RandomScoreEngine(players, seed = 99L, dispatcher = dispatcher, clock = { 0L })

        val seenUserIds = collectN(engine, 60).map { it.userId }.toSet()

        assertEquals(players.map { it.id }.toSet(), seenUserIds)
    }

    @Test
    fun `delay between consecutive emissions stays within the configured bounds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val minDelay = 500L
        val maxDelay = 2000L
        val engine = RandomScoreEngine(
            players = players,
            seed = 3L,
            minDelayMs = minDelay,
            maxDelayMs = maxDelay,
            dispatcher = dispatcher,
            clock = { 0L },
        )

        val timestamps = mutableListOf<Long>()
        val job = launch {
            engine.events.collect {
                timestamps += testScheduler.currentTime
                if (timestamps.size == 10) cancel()
            }
        }
        job.join()

        for (i in 1 until timestamps.size) {
            val gap = timestamps[i] - timestamps[i - 1]
            assertTrue(gap in minDelay..maxDelay, "gap $gap between emissions was outside [$minDelay, $maxDelay]")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an empty player pool`() {
        RandomScoreEngine(players = emptyList(), seed = 1L)
    }

    @Test
    fun `re-collecting the same engine continues the match instead of restarting it`() = runTest {
        // Regression test for: backgrounding the app cancels the active collector, foregrounding
        // it starts a new one on the *same* engine instance (AppContainer holds it as a
        // singleton) - the match must continue, not silently reset every player back to 0.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RandomScoreEngine(players, seed = 55L, dispatcher = dispatcher, clock = { 0L })

        val beforePause = collectN(engine, 15)
        val scoreBeforePause = beforePause
            .groupBy { it.userId }
            .mapValues { (_, events) -> events.last().newScore }

        // Simulate the pause (nobody collecting) by simply not collecting for a while, then start
        // a brand new collection - exactly what SharingStarted.WhileSubscribed does on resume.
        val afterResume = collectN(engine, 15)

        for (event in afterResume) {
            val priorScore = scoreBeforePause[event.userId] ?: continue // not touched before pause
            assertTrue(
                event.newScore > priorScore,
                "expected ${event.userId} to continue climbing from $priorScore after resuming, " +
                    "got ${event.newScore} - looks like the match reset instead of resuming",
            )
        }

        // The two 15-event windows must not be identical - if the engine had reset, the same
        // seed would deterministically replay the exact same first 15 events again.
        assertNotEquals(beforePause, afterResume)
    }
}
