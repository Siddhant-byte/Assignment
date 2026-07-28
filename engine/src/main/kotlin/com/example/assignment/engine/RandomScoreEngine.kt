package com.example.assignment.engine

import com.example.assignment.core.model.Player
import com.example.assignment.core.model.ScoreEvent
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Score Generator / Game Engine module (Part 1, Module 1).
 *
 * Simulates a match backend: maintains a list of players and continuously emits [ScoreEvent]s
 * for random players at random intervals. This class is:
 * - **UI-agnostic**: no Android/Compose/View imports anywhere in this module.
 * - **Reusable**: consumed only through the [ScoreEventSource] interface it implements.
 * - **Testable**: every source of non-determinism (delay timing, dispatcher, wall-clock time) is
 *   an injectable constructor parameter, so tests can fully control time via a `TestDispatcher`.
 *
 * Business rules implemented here:
 * - Scores update at a random interval between [minDelayMs] and [maxDelayMs] (default 500-2000ms).
 * - Each tick targets a random user from [players].
 * - A user's score only ever increases (`delta` is always a positive value).
 * - The sequence is **deterministic per session**: every collection of [events] seeds a fresh
 *   [Random] with the same [seed] and starts every player at score 0, so two collections (or two
 *   engines constructed with the same [players]/[seed]) always replay the exact same sequence of
 *   events. This also doubles as a natural building block for replay-based anti-cheat auditing
 *   (see README "Anti-cheat ideas").
 *
 * @param players the pool of players this session can generate events for. Must be non-empty.
 * @param seed seeds the per-collection [Random] instance; same seed => same event sequence.
 * @param sessionId tags every emitted [ScoreEvent] so consumers can distinguish sessions/replays.
 * Defaults to [seed] since, for this assignment, one seed == one session.
 * @param minDelayMs lower bound (inclusive) of the random inter-event delay, in milliseconds.
 * @param maxDelayMs upper bound (inclusive) of the random inter-event delay, in milliseconds.
 * @param dispatcher where the generation loop (`delay` + score bookkeeping) runs. Defaults to
 * [Dispatchers.Default] in production (keeps this CPU/timer-ish work off the caller's dispatcher);
 * tests inject a `TestDispatcher` tied to a `TestCoroutineScheduler` so `delay` advances virtual
 * time instantly instead of sleeping for real milliseconds.
 * @param clock supplies the wall-clock timestamp stamped on each event; injectable for tests.
 */
class RandomScoreEngine(
    private val players: List<Player>,
    private val seed: Long,
    private val sessionId: Long = seed,
    private val minDelayMs: Long = 500L,
    private val maxDelayMs: Long = 2000L,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) : ScoreEventSource {

    init {
        require(players.isNotEmpty()) { "RandomScoreEngine requires at least one player" }
        require(minDelayMs in 1..maxDelayMs) {
            "minDelayMs ($minDelayMs) must be positive and <= maxDelayMs ($maxDelayMs)"
        }
    }

    override val events: Flow<ScoreEvent> = flow {
        // Fresh Random + fresh score map per collection => deterministic per session and safe
        // for multiple independent collectors (e.g. a unit test collecting the same engine twice).
        val random = Random(seed)
        val scores = HashMap<String, Long>(players.size).apply {
            players.forEach { put(it.id, 0L) }
        }

        while (true) {
            val delayMs = random.nextLong(minDelayMs, maxDelayMs + 1)
            delay(delayMs)

            val player = players[random.nextInt(players.size)]
            val delta = random.nextLong(1L, 51L) // always positive => score only increases
            val newScore = scores.getValue(player.id) + delta
            scores[player.id] = newScore

            emit(
                ScoreEvent(
                    userId = player.id,
                    username = player.username,
                    delta = delta,
                    newScore = newScore,
                    timestamp = clock(),
                    sessionId = sessionId,
                ),
            )
        }
    }.flowOn(dispatcher)
}
