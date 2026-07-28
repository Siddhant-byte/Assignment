# Real-Time Leaderboard

A small Android app that simulates a live gaming leaderboard — a fake match engine generates
score updates for a set of players, and a leaderboard screen shows everyone ranked in real time.

Built in Kotlin with Coroutines/Flow end-to-end and a Jetpack Compose UI, split across four
Gradle modules so the "engine" (generates scores) and the "leaderboard" (ranks them) are
genuinely independent of each other.

For the architecture reasoning, trade-offs, and the write-up on performance/lifecycle/leadership
questions, see **[docs/architecture-and-leadership-note.pdf](docs/architecture-and-leadership-note.pdf)**
(also available as [HTML](docs/architecture-and-leadership-note.html)).

## Running it

Requires JDK 17.

```bash
./gradlew assembleDebug   # build the app
./gradlew installDebug    # install on a connected device/emulator (API 24+)
./gradlew test            # run all unit tests
```

Or just open the project root in Android Studio and hit run. You'll land on a single screen —
ten players' scores start updating every 0.5-2 seconds, ranked live.

## How it's put together

| Module | What it does |
|---|---|
| `core` | Plain data classes shared by the other two (`ScoreEvent`, `Player`, `LeaderboardEntry`). No logic, no Android. |
| `engine` | The fake match engine. Picks a random player every 0.5-2s and bumps their score. Nothing else knows this is fake — it just emits a `Flow` of score events. |
| `leaderboard` | Takes any stream of score events and turns it into a ranked list (ties share a rank, standard "1, 2, 2, 4" style). Has no idea where the events come from. |
| `app` | The Android app — wires the two together, and shows the result in a Compose screen. |

`engine` and `leaderboard` don't depend on each other at all — only on the shared `core` models.
That's on purpose: swapping the fake engine for a real backend later shouldn't require touching
the ranking code (or vice versa).

## Tests

17 unit tests, all in `engine` and `leaderboard` (the parts with actual logic worth testing):
determinism of the fake engine, scores never decreasing, tie-breaking rules, and making sure the
leaderboard doesn't re-emit when nothing actually changed.
