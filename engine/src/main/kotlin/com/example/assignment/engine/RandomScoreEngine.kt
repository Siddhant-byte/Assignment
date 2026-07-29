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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
 * - The match state (the [Random] generator and each player's running score) lives on the
 *   **engine instance**, not inside the `flow {}` builder. That is deliberate: [ScoreEventSource]
 *   is exposed as a singleton from the composition root for the lifetime of the app process (see
 *   `AppContainer`), and collection of [events] naturally starts/stops as the UI's lifecycle-aware
 *   `StateFlow` attaches/detaches (e.g. on backgrounding). If the state lived inside the `flow {}`
 *   body, every fresh `collect()` after a detach would silently replay the match from score 0 -
 *   which is exactly the bug this shape avoids: stopping collection pauses event generation
 *   (still saves the CPU/battery cost while nobody's watching), and a later `collect()` on the
 *   *same instance* simply continues the same match from wherever it left off.
 * - Two *different* engine instances constructed with the same [players]/[seed] still replay the
 *   exact same event sequence from the start (see [RandomScoreEngineTest]) - that guarantee is
 *   what makes the [seed] useful for reproducing/debugging a specific session, and is the building
 *   block for replay-based anti-cheat auditing (see README "Anti-cheat ideas").
 * - Mutating [random] and [scores] is guarded by [tickMutex] so that two *concurrent* collectors
 *   of the same instance (not expected in this app today, but cheap to guarantee) can't interleave
 *   reads/writes of the shared state.
 *
 * @param players the pool of players this session can generate events for. Must be non-empty.
 * @param seed seeds this instance's [Random] generator; two instances built with the same seed
 * produce the same event sequence from the start.
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

    /** Guards [random] and [scores] - see class doc for why this state outlives one collection. */
    private val tickMutex = Mutex()
    private val random = Random(seed)
    private val scores = HashMap<String, Long>(players.size).apply {
        players.forEach { put(it.id, 0L) }
    }

    override val events: Flow<ScoreEvent> = flow {
        while (true) {
            val delayMs = tickMutex.withLock { random.nextLong(minDelayMs, maxDelayMs + 1) }
            delay(delayMs)

            val scoreEvent = tickMutex.withLock {
                val player = players[random.nextInt(players.size)]
                val delta = random.nextLong(1L, 51L) // always positive => score only increases
                val newScore = scores.getValue(player.id) + delta
                scores[player.id] = newScore

                ScoreEvent(
                    userId = player.id,
                    username = player.username,
                    delta = delta,
                    newScore = newScore,
                    timestamp = clock(),
                    sessionId = sessionId,
                )
            }

            emit(scoreEvent)
        }
    }.flowOn(dispatcher)
}
