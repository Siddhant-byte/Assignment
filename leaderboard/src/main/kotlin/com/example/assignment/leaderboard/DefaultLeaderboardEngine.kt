package com.example.assignment.leaderboard

import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.core.model.ScoreEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * Default [LeaderboardEngine]: folds the incoming [ScoreEvent] stream into a running per-user
 * score map, re-derives ranks through the pure [RankingRules.rank], and only re-emits the ranked
 * list when it has *actually* changed.
 *
 * That last part - `distinctUntilChanged()` - is the concrete, data-layer fix for the "no
 * flickering or unnecessary recomputation" business rule: a duplicate/no-op [ScoreEvent] (or any
 * event that doesn't change the derived ranking) produces an identical `List<LeaderboardEntry>`,
 * which `distinctUntilChanged()` swallows before it ever reaches the ViewModel/UI. This is
 * distinct from (and in addition to) the Compose-layer recomposition-avoidance mechanisms used in
 * `:app` (stable `LeaderboardEntry`, `ImmutableList`, `LazyColumn` keys) - see README.
 *
 * @param dispatcher where the fold + sort/rank computation runs. Defaults to [Dispatchers.Default]
 * (CPU-bound work, kept off the caller's/Main dispatcher); tests inject a `TestDispatcher`.
 */
class DefaultLeaderboardEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LeaderboardEngine {

    override fun observe(events: Flow<ScoreEvent>): Flow<List<LeaderboardEntry>> =
        events
            .scan(emptyMap<String, ScoreSnapshot>()) { scoresByUserId, event ->
                scoresByUserId + (
                    event.userId to ScoreSnapshot(
                        userId = event.userId,
                        username = event.username,
                        score = event.newScore,
                    )
                    )
            }
            .map { scoresByUserId -> RankingRules.rank(scoresByUserId.values) }
            .distinctUntilChanged()
            .flowOn(dispatcher)
}
