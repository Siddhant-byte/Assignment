package com.example.assignment.ui.leaderboard

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.assignment.AppContainer
import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.presentation.LeaderboardViewModel
import com.example.assignment.ui.theme.AvatarPalette
import com.example.assignment.ui.theme.LiveGreen
import com.example.assignment.ui.theme.OnRankBronze
import com.example.assignment.ui.theme.OnRankGold
import com.example.assignment.ui.theme.OnRankSilver
import com.example.assignment.ui.theme.RankBronze
import com.example.assignment.ui.theme.RankGold
import com.example.assignment.ui.theme.AssignmentTheme
import com.example.assignment.ui.theme.RankSilver
import com.example.assignment.ui.theme.TrendDown
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

/**
 * Screen-level entry point (Part 3). Wires the ViewModel to the stateless [LeaderboardScreen]
 * composable below. [collectAsStateWithLifecycle] is what makes collection lifecycle-aware: it
 * automatically stops observing [LeaderboardViewModel.uiState] when the screen drops below
 * STARTED (e.g. app backgrounded) and resumes on return, rather than collecting - and
 * recomposing - while invisible.
 */
@Composable
fun LeaderboardRoute(
    viewModel: LeaderboardViewModel = viewModel(
        factory = LeaderboardViewModel.Factory(
            scoreSource = AppContainer.scoreEventSource,
            leaderboardEngine = AppContainer.leaderboardEngine,
        ),
    ),
) {
    val entries by viewModel.uiState.collectAsStateWithLifecycle()
    LeaderboardScreen(entries = entries)
}

/**
 * Stateless and preview-friendly by design - takes plain data in, no ViewModel/DI reference -
 * so it is trivially unit/UI-testable in isolation from the real-time pipeline.
 */
@Composable
fun LeaderboardScreen(entries: ImmutableList<LeaderboardEntry>) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { LeaderboardHeader(playerCount = entries.size) },
    ) { contentPadding ->
        if (entries.isEmpty()) {
            EmptyState(modifier = Modifier.padding(contentPadding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = entries, key = { it.userId }) { entry ->
                // Modifier.animateItem() gives us the "rank movement animation" visual effect for
                // free: when an entry's position in the list changes (it overtakes/is overtaken),
                // Compose smoothly animates the row to its new slot instead of snapping.
                LeaderboardRow(entry = entry, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun LeaderboardHeader(playerCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            // Scaffold's topBar slot doesn't auto-inset a plain Column the way TopAppBar does -
            // without this, the header draws underneath the status bar on edge-to-edge devices.
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = "Live Leaderboard",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (playerCount == 0) {
                        "Waiting for the match to start"
                    } else {
                        "$playerCount players \u2022 updating live"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        )
    }
}

/** Small pulsing dot next to the "updating live" label - a lightweight, dependency-free way to
 * reinforce that this screen is a live stream, not a static snapshot. */
@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveDotAlpha",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color = LiveGreen.copy(alpha = alpha), shape = CircleShape),
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Waiting for the first score update\u2026",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Whether a player's rank improved, worsened, or stayed the same since this row's last frame. */
private enum class Trend { UP, DOWN, FLAT }

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, modifier: Modifier = Modifier) {
    var isFirstComposition by remember(entry.userId) { mutableStateOf(true) }
    var isFlashing by remember(entry.userId) { mutableStateOf(false) }
    var showDelta by remember(entry.userId) { mutableStateOf(false) }
    var scoreDelta by remember(entry.userId) { mutableStateOf(0L) }
    var previousScore by remember(entry.userId) { mutableStateOf(entry.score) }
    var previousRank by remember(entry.userId) { mutableStateOf(entry.rank) }

    val trend = when {
        entry.rank < previousRank -> Trend.UP
        entry.rank > previousRank -> Trend.DOWN
        else -> Trend.FLAT
    }
    // Recorded after composition, not during, so `trend` above still compares against the rank
    // this row had *before* this update - only then do we update our memory of "previous".
    SideEffect { previousRank = entry.rank }

    // "Highlight + '+N' pill on score update" visual effect: scoped/reset per userId (the
    // LazyColumn key) so it never bleeds across a reordered slot.
    LaunchedEffect(entry.score) {
        if (isFirstComposition) {
            // Don't flash on the very first time this row appears - only on real score changes.
            isFirstComposition = false
        } else if (entry.score != previousScore) {
            scoreDelta = entry.score - previousScore
            isFlashing = true
            showDelta = true
            delay(900)
            isFlashing = false
            showDelta = false
        }
        previousScore = entry.score
    }

    val cardColor by animateColorAsState(
        targetValue = if (isFlashing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "scoreHighlight",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (entry.rank == 1) {
                    Modifier.border(1.dp, RankGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(rank = entry.rank)
            Spacer(modifier = Modifier.width(6.dp))
            TrendIndicator(trend = trend)
            Spacer(modifier = Modifier.width(10.dp))
            PlayerAvatar(userId = entry.userId, username = entry.username)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = entry.username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.score.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AnimatedVisibility(visible = showDelta && scoreDelta > 0) {
                    Text(
                        text = "+$scoreDelta",
                        style = MaterialTheme.typography.labelSmall,
                        color = LiveGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val (background, contentColor) = when (rank) {
        1 -> RankGold to OnRankGold
        2 -> RankSilver to OnRankSilver
        3 -> RankBronze to OnRankBronze
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier.size(36.dp).background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun TrendIndicator(trend: Trend) {
    val (symbol, color) = when (trend) {
        Trend.UP -> "\u25B2" to LiveGreen
        Trend.DOWN -> "\u25BC" to TrendDown
        Trend.FLAT -> "\u2013" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Text(
        text = symbol,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(14.dp),
    )
}

@Composable
private fun PlayerAvatar(userId: String, username: String) {
    val background = AvatarPalette[abs(userId.hashCode()) % AvatarPalette.size]
    val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = Modifier.size(38.dp).background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}

private val previewEntries: ImmutableList<LeaderboardEntry> = persistentListOf(
    LeaderboardEntry(rank = 1, userId = "p1", username = "NovaStrike", score = 420),
    LeaderboardEntry(rank = 2, userId = "p2", username = "ByteCrusher", score = 310),
    LeaderboardEntry(rank = 2, userId = "p3", username = "PixelRogue", score = 310),
    LeaderboardEntry(rank = 4, userId = "p4", username = "ShadowVolt", score = 220),
    LeaderboardEntry(rank = 5, userId = "p5", username = "QuantumAce", score = 90),
)

@Preview(showBackground = true)
@Composable
private fun LeaderboardScreenPreview() {
    AssignmentTheme { LeaderboardScreen(entries = previewEntries) }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LeaderboardScreenPreviewDark() {
    AssignmentTheme { LeaderboardScreen(entries = previewEntries) }
}
