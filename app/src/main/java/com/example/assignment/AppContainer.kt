package com.example.assignment

import com.example.assignment.core.model.Player
import com.example.assignment.engine.RandomScoreEngine
import com.example.assignment.engine.ScoreEventSource
import com.example.assignment.leaderboard.DefaultLeaderboardEngine
import com.example.assignment.leaderboard.LeaderboardEngine

/**
 * Composition root. This is the *only* place in the app that knows about the concrete
 * [RandomScoreEngine] and [DefaultLeaderboardEngine] implementations - everything downstream
 * ([com.example.assignment.presentation.LeaderboardViewModel]) depends only on the
 * [ScoreEventSource] / [LeaderboardEngine] interfaces.
 *
 * Swapping the fake match engine for a real backend (e.g. a WebSocket-backed
 * `ScoreEventSource`) later means writing one new class and changing the single line below -
 * no changes to the ViewModel, the `:leaderboard` module, or any Compose UI.
 *
 * No DI framework is used deliberately: for a single-screen, ~5-7h-scoped assignment, a
 * hand-rolled composition root is the simplest thing that could possibly satisfy "no tight
 * coupling" without pulling in Hilt/Koin machinery that would itself need justifying.
 */
object AppContainer {

    /**
     * Identifies this app process's simulated match session. Using the process start time as the
     * seed means: (a) it's stable for the lifetime of the app process - config changes/backgrounds
     * don't create a new session or reset scores, and (b) it is still fully reproducible - to
     * replay a specific session for debugging, hardcode this value instead of reading the clock.
     */
    private val sessionId: Long = System.currentTimeMillis()

    private val players: List<Player> = listOf(
        Player(id = "p1", username = "NovaStrike"),
        Player(id = "p2", username = "ByteCrusher"),
        Player(id = "p3", username = "PixelRogue"),
        Player(id = "p4", username = "ShadowVolt"),
        Player(id = "p5", username = "QuantumAce"),
        Player(id = "p6", username = "TurboFox"),
        Player(id = "p7", username = "CrimsonHawk"),
        Player(id = "p8", username = "GlitchWolf"),
        Player(id = "p9", username = "NeonViper"),
        Player(id = "p10", username = "FrostBlade"),
    )

    val scoreEventSource: ScoreEventSource by lazy {
        RandomScoreEngine(players = players, seed = sessionId, sessionId = sessionId)
    }

    val leaderboardEngine: LeaderboardEngine by lazy { DefaultLeaderboardEngine() }
}
