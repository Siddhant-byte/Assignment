package com.example.assignment.core.model

/**
 * A single ranked row as shown on the leaderboard screen (Part 3: rank, username, live score).
 *
 * Deliberately built from only stable, primitive-ish fields ([Int], [String], [Long]) so the
 * Jetpack Compose compiler can treat this type as *stable* - meaning a row composable that reads
 * only its own [LeaderboardEntry] will skip recomposition when an unrelated row changes. This is
 * the Compose-layer half of Part 4's "avoid unnecessary recompositions" requirement (the other,
 * distinct half is the data-layer `distinctUntilChanged()` in the `:leaderboard` module - see
 * README "Performance & Lifecycle" section for why these are two separate mechanisms).
 *
 * @property rank 1-based competition rank. Ties share a rank, and the next distinct score's rank
 * skips ahead by the number of tied entries (standard "1,2,2,4" competition ranking).
 * @property userId stable identity, used as the Compose `LazyColumn` item key so reordering
 * animates in place instead of destroying/recreating row composables.
 * @property username display name.
 * @property score the player's current total score.
 */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val score: Long,
)
