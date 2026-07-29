package com.example.assignment.leaderboard

import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.core.model.ScoreEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default [LeaderboardEngine]: folds the incoming [ScoreEvent] stream into a running per-user
 * score map, re-derives ranks through the pure [RankingRules.rank], and only re-emits the ranked
 * list when it has *actually* changed.
 *
 * The per-user score map ([scoresByUserId]) lives on the **engine instance**, not inside a
 * transient `scan()` operator. That matters for the same reason it matters in
 * [com.example.assignment.engine.RandomScoreEngine]: this engine is a process-lifetime singleton
 * (see `AppContainer`), and its [observe] flow gets re-collected whenever the UI's
 * lifecycle-aware `StateFlow` re-attaches (e.g. coming back from the background). With a `scan()`,
 * that re-collection would restart the fold from an empty map and the leaderboard would appear to
 * "reset" and refill from scratch even though the underlying match never actually restarted. By
 * keeping the accumulated map on the instance instead, resuming collection immediately re-ranks
 * from the already-known standings and only changes as genuinely new events arrive.
 *
 * `distinctUntilChanged()` is the concrete, data-layer fix for the "no flickering or unnecessary
 * recomputation" business rule: a duplicate/no-op [ScoreEvent] (or any event that doesn't change
 * the derived ranking) produces an identical `List<LeaderboardEntry>`, which
 * `distinctUntilChanged()` swallows before it ever reaches the ViewModel/UI. This is distinct from
 * (and in addition to) the Compose-layer recomposition-avoidance mechanisms used in `:app` (stable
 * `LeaderboardEntry`, `ImmutableList`, `LazyColumn` keys) - see README.
 *
 * @param dispatcher where the fold + sort/rank computation runs. Defaults to [Dispatchers.Default]
 * (CPU-bound work, kept off the caller's/Main dispatcher); tests inject a `TestDispatcher`.
 */
class DefaultLeaderboardEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LeaderboardEngine {

    /** Guards [scoresByUserId] against concurrent collectors of the same instance. */
    private val mutex = Mutex()
    private val scoresByUserId = HashMap<String, ScoreSnapshot>()

    override fun observe(events: Flow<ScoreEvent>): Flow<List<LeaderboardEntry>> =
        events
            .map { event ->
                mutex.withLock {
                    scoresByUserId[event.userId] = ScoreSnapshot(
                        userId = event.userId,
                        username = event.username,
                        score = event.newScore,
                    )
                    RankingRules.rank(scoresByUserId.values)
                }
            }
            .distinctUntilChanged()
            .flowOn(dispatcher)
}
