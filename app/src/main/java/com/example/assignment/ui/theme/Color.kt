package com.example.assignment.ui.theme

import androidx.compose.ui.graphics.Color

// --- Light scheme -----------------------------------------------------------------------------
val LightBackground = Color(0xFFF6F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEFF6)
val LightOnBackground = Color(0xFF15171C)
val LightOnSurfaceVariant = Color(0xFF5B616E)
val LightOutline = Color(0xFFDCE0EA)
val LightPrimary = Color(0xFF4A5CE0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE1E4FB)
val LightOnPrimaryContainer = Color(0xFF1E2A78)

// --- Dark scheme -------------------------------------------------------------------------------
val DarkBackground = Color(0xFF0E1015)
val DarkSurface = Color(0xFF181B22)
val DarkSurfaceVariant = Color(0xFF21252E)
val DarkOnBackground = Color(0xFFECEDF1)
val DarkOnSurfaceVariant = Color(0xFF9AA1AF)
val DarkOutline = Color(0xFF343943)
val DarkPrimary = Color(0xFF8B98FF)
val DarkOnPrimary = Color(0xFF131635)
val DarkPrimaryContainer = Color(0xFF2B2F63)
val DarkOnPrimaryContainer = Color(0xFFDCE0FF)

// --- Design tokens that live outside the Material role system ------------------------------
// These are used directly by the leaderboard row/rank/avatar UI rather than mapped onto
// Material3's ColorScheme roles - there's no "gold medal" role in Material, and forcing one in
// would mean fighting the palette generator for a handful of one-off colors.
val RankGold = Color(0xFFFFC53D)
val OnRankGold = Color(0xFF3A2900)
val RankSilver = Color(0xFFB0B8C4)
val OnRankSilver = Color(0xFF1D2229)
val RankBronze = Color(0xFFE0995A)
val OnRankBronze = Color(0xFF3A1F06)
val LiveGreen = Color(0xFF1DBD62)
val TrendDown = Color(0xFFE0554B)

/** Deterministic per-player avatar colors - same [android userId] always maps to the same tint. */
val AvatarPalette = listOf(
    Color(0xFFEF6C6C),
    Color(0xFFF59E0B),
    Color(0xFF4A5CE0),
    Color(0xFF17C964),
    Color(0xFF8B5CF6),
    Color(0xFF06B6D4),
    Color(0xFFE85DAB),
    Color(0xFFFB923C),
)
