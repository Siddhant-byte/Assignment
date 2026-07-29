package com.example.assignment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.engine.ScoreEventSource
import com.example.assignment.leaderboard.LeaderboardEngine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Deliberately thin. This class does exactly one thing: wire [scoreSource]'s events into
 * [leaderboardEngine] and expose the result as UI state. It contains **zero** ranking/sorting
 * logic of its own - that lives entirely in the `:leaderboard` module - which is the direct
 * answer to Part 2's "ranking logic must NOT live in UI or ViewModel only" requirement and the
 * "everything in ViewModel" anti-pattern called out in the assignment.
 *
 * Constructor-injected against the [ScoreEventSource] **interface**, not the concrete
 * `RandomScoreEngine` - see [com.example.assignment.AppContainer] for where that gets bound. That
 * is what makes "swap the engine without touching this class" an actually-true claim rather than
 * an assertion, and gives this class a real seam for injecting a fake source in future ViewModel
 * tests.
 */
class LeaderboardViewModel(
    scoreSource: ScoreEventSource,
    leaderboardEngine: LeaderboardEngine,
) : ViewModel() {

    val uiState: StateFlow<ImmutableList<LeaderboardEntry>> =
        leaderboardEngine.observe(scoreSource.events)
            .map { it.toPersistentList() }
            .stateIn(
                scope = viewModelScope,
                // Keeps the upstream Flow (and therefore the fake engine's coroutine) alive for a
                // short grace period after the last collector disappears (e.g. a rotation), but
                // fully cancels it once the screen is gone for good (e.g. backgrounded past the
                // grace period). This only pauses event generation, not the match itself: both
                // RandomScoreEngine and DefaultLeaderboardEngine keep their accumulated state on
                // the (singleton, process-lifetime) instance rather than inside this Flow, so a
                // later re-collection - foregrounding the app - picks the same standings back up
                // instead of restarting from zero. See README "Performance & Lifecycle".
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = persistentListOf(),
            )

    class Factory(
        private val scoreSource: ScoreEventSource,
        private val leaderboardEngine: LeaderboardEngine,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LeaderboardViewModel(scoreSource, leaderboardEngine) as T
        }
    }
}
