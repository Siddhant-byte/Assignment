package com.example.assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.assignment.ui.leaderboard.LeaderboardRoute
import com.example.assignment.ui.theme.AssignmentTheme

/**
 * Single screen required by Part 3 - hosts the live leaderboard. All real-time wiring lives in
 * [LeaderboardRoute] / [com.example.assignment.presentation.LeaderboardViewModel]; this Activity
 * only sets the theme and content.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AssignmentTheme {
                LeaderboardRoute()
            }
        }
    }
}
