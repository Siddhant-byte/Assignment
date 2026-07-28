package com.example.assignment.ui.leaderboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.assignment.AppContainer
import com.example.assignment.core.model.LeaderboardEntry
import com.example.assignment.presentation.LeaderboardViewModel
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(entries: ImmutableList<LeaderboardEntry>) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Live Leaderboard") }) },
    ) { contentPadding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Text(
                    text = "Waiting for the first score update…",
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            items(items = entries, key = { it.userId }) { entry ->
                // Modifier.animateItem() gives us the "rank movement animation" visual effect for
                // free: when an entry's position in the list changes (it overtakes/is overtaken),
                // Compose smoothly animates the row to its new slot instead of snapping.
                LeaderboardRow(entry = entry, modifier = Modifier.animateItem())
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, modifier: Modifier = Modifier) {
    // "Highlight user when score updates" visual effect: a short color pulse on this row,
    // scoped/reset per userId (LazyColumn's key) so it never bleeds across a reordered slot.
    var isFlashing by remember(entry.userId) { mutableStateOf(false) }
    var isFirstComposition by remember(entry.userId) { mutableStateOf(true) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isFlashing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "scoreHighlight",
    )

    LaunchedEffect(entry.score) {
        if (isFirstComposition) {
            // Don't flash on the very first time this row appears - only on real score changes.
            isFirstComposition = false
            return@LaunchedEffect
        }
        isFlashing = true
        delay(220)
        isFlashing = false
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${entry.rank}",
            modifier = Modifier.width(48.dp),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = entry.username,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Text(
            text = entry.score.toString(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val previewEntries: ImmutableList<LeaderboardEntry> = persistentListOf(
    LeaderboardEntry(rank = 1, userId = "p1", username = "NovaStrike", score = 420),
    LeaderboardEntry(rank = 2, userId = "p2", username = "ByteCrusher", score = 310),
    LeaderboardEntry(rank = 2, userId = "p3", username = "PixelRogue", score = 310),
)

@Preview(showBackground = true)
@Composable
private fun LeaderboardScreenPreview() {
    LeaderboardScreen(entries = previewEntries)
}
