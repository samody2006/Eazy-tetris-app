# Eazy Tetris — Classic Nintendo-Inspired Android Edition

## 1. Product Overview

Build a classic Tetris game for Android inspired by the gameplay simplicity and visual character of the Nintendo NES-era Tetris.

The game should combine:

- Classic Tetris gameplay
- Colorful tetrominoes
- Retro arcade presentation
- Responsive Android touch controls
- Local high-score leaderboard
- Simple, fast gameplay
- Offline operation

The objective is to create a game that feels nostalgic and immediately familiar while still being comfortable to play on modern Android phones.

The application should not become a modern social/mobile gaming platform.

---

# 2. Design Philosophy

The game should follow these principles:

1. Gameplay comes first.
2. The board should dominate the screen.
3. Controls must be responsive.
4. Rules must be deterministic and testable.
5. No internet connection should be required.
6. No account should be required.
7. No backend should be required.
8. No unnecessary game mechanics should be introduced.
9. The game should be playable within seconds of launching.
10. The player should be motivated primarily by improving their score.

Target feeling:

> "Classic Tetris, but properly adapted for an Android phone."

---

# 3. Platform

Target:

- Android phones
- Portrait orientation
- Android native application
- Offline-first

Recommended technology:

- Kotlin
- Jetpack Compose
- Android Canvas for board rendering
- Room for high scores
- Kotlin Coroutines
- ViewModel
- StateFlow

Do not use:

- Unity
- Unreal Engine
- React Native
- Flutter
- Backend APIs

---

# 4. Application Architecture

Use a layered architecture.

```text
app/
├── core/
│   ├── game/
│   ├── model/
│   └── util/
│
├── data/
│   ├── local/
│   │   ├── database/
│   │   ├── dao/
│   │   └── entity/
│   └── repository/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── presentation/
│   ├── navigation/
│   ├── menu/
│   ├── game/
│   ├── leaderboard/
│   ├── settings/
│   └── components/
│
└── MainActivity.kt
```

The exact package structure may be adjusted if the implementation benefits from a better organization.

Do not over-engineer.

---

# 5. Core Game Engine

The game engine must be independent of Android UI.

The core game should be capable of running without Compose.

Suggested components:

```text
TetrisGame
GameBoard
Tetromino
TetrominoType
PieceGenerator
CollisionDetector
RotationSystem
LineClearSystem
ScoreSystem
LevelSystem
GameState
```

The UI observes game state and sends player actions to the game engine.

Do not place game rules inside Composable functions.

---

# 6. Game States

Use an explicit game state.

```kotlin
enum class GameState {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}
```

The state controls:

- Game timer
- Input
- Rendering
- Pause overlay
- Game-over overlay

---

# 7. Board

Use a standard Tetris board:

```text
10 columns × 20 visible rows
```

Coordinates:

```text
x = 0..9
y = 0..19
```

The top of the board is:

```text
y = 0
```

The bottom is:

```text
y = 19
```

Each cell can contain:

```text
EMPTY
I
O
T
S
Z
J
L
```

The active falling piece should not permanently modify the board until it locks.

---

# 8. Tetrominoes

Implement exactly seven tetromino types:

```text
I
O
T
S
Z
J
L
```

Each consists of four blocks.

Use integer grid coordinates.

---

# 9. Tetromino Color Mapping

Use a colorful classic palette.

```text
I → Cyan
O → Yellow
T → Purple
S → Green
Z → Red
J → Blue
L → Orange
```

The color is associated with the tetromino type rather than individual blocks.

Do not use random colors.

---

# 10. Tetromino Rotation Data

Represent each piece using predefined rotation states.

Do not calculate arbitrary floating-point rotations.

All coordinates must remain integer grid coordinates.

Each rotatable piece should have four states:

```text
0° 
90°
180°
270°
```

The O piece should effectively remain unchanged.

Example T piece:

```text
State 0:

. T .
T T T
. . .

State 1:

. T .
. T T
. T .

State 2:

. . .
T T T
. T .

State 3:

. T .
T T .
. T .
```

The exact implementation may use a pivot-based coordinate representation, but the resulting positions must be deterministic.

---

# 11. Rotation System

Use a classic rotation behavior rather than blindly applying the modern Tetris Guideline.

Rotation must:

1. Determine the next rotation state.
2. Test whether the new position is valid.
3. If the rotation causes a collision, attempt predefined small offsets.
4. If no valid position exists, reject the rotation.

The player must never be allowed to rotate through occupied blocks or outside the board.

Do not implement T-spin detection.

Do not award T-spin bonuses.

---

# 12. Piece Spawning

Pieces spawn near the horizontal center of the board.

The spawning system must ensure that pieces appear in a predictable location.

When a piece is spawned:

```text
x ≈ center of board
y = top spawn area
```

The active piece should initially use its default rotation state.

If the newly spawned piece immediately collides with the existing board:

```text
GAME OVER
```

---

# 13. Piece Randomization

Use a fair seven-piece bag randomizer.

Each bag contains:

```text
I
O
T
S
Z
J
L
```

Shuffle the bag.

Use every piece once before generating another complete bag.

Example:

```text
Bag 1:
T Z L O I S J

Bag 2:
O I Z T J L S
```

This reduces extremely unfair sequences while maintaining randomness.

The sequence must be injectable during testing.

Example:

```kotlin
PieceGenerator(sequence = listOf(I, T, O, ...))
```

---

# 14. Next Piece

Always maintain a next-piece preview.

When the current piece is active:

```text
CURRENT → falling piece
NEXT    → preview piece
```

When the current piece locks:

```text
NEXT becomes CURRENT
A new piece becomes NEXT
```

The next piece should be visible on the game screen.

---

# 15. Hold Piece

Do NOT implement Hold in the first version.

The game should remain closer to the classic experience.

Future versions may add Hold, but the core implementation should not depend on it.

---

# 16. Ghost Piece

Do NOT implement a ghost/falling-shadow piece in the first version.

This is intentionally excluded to preserve the classic feel.

---

# 17. Automatic Gravity

The piece automatically moves down according to the current level.

Use a timing loop independent of Compose recomposition.

Conceptually:

```text
every gravityInterval:
    attemptMoveDown()
```

If the piece cannot move down:

```text
lockPiece()
```

The timing system must not depend on frame rate.

---

# 18. Gravity / Level Speed

Implement configurable gravity.

Use the following initial progression. The increase is intentionally gradual so
the game remains playable as the level rises:

| Level | Approx. gravity interval |
|---:|---:|
| 1 | 1.200 sec |
| 2 | 1.100 sec |
| 3 | 1.000 sec |
| 4 | 0.900 sec |
| 5 | 0.820 sec |
| 6 | 0.750 sec |
| 7 | 0.680 sec |
| 8 | 0.610 sec |
| 9 | 0.550 sec |
| 10 | 0.500 sec |
| 11 | 0.450 sec |
| 12 | 0.400 sec |
| 13 | 0.360 sec |
| 14 | 0.320 sec |
| 15 | 0.280 sec |
| 16 | 0.250 sec |
| 17+ | 0.220 sec |

These values are configuration, not scattered constants.

The implementation should make it easy to tune the speed after gameplay testing.

---

# 19. Level Progression

Start:

```text
Level = 1
```

Each level has a line target. The target increases in a predictable sequence:

```text
Level 1 target: 20 lines
Level 2 target: 25 lines
Level 3 target: 30 lines
Level 4 target: 35 lines
Level 5 target: 40 lines
Level 6 target: 45 lines
Level 7 target: 50 lines
...
```

The target for level N is:

```text
20 + ((N - 1) * 5)
```

Track both `linesInLevel` and `totalLinesCleared`.

When the current level target is reached:

1. Finish scoring the cleared lines.
2. Increase the level by one.
3. Reset `linesInLevel` to zero.
4. Clear every occupied cell from the board.
5. Apply the new gravity interval.
6. Spawn the next piece on the empty board.

`totalLinesCleared` and `score` are never reset during a game.

```text
0–9 lines   → Level 1
10–19       → Level 2
20–29       → Level 3
30–39       → Level 4
...
```

The level must increase automatically using the targets defined above. Any
earlier 10-line progression example in this document is obsolete and must not
be implemented.

The current level affects gravity and score.

---

# 20. Movement

The player can move the active piece:

```text
LEFT
RIGHT
DOWN
```

### Left

Move one cell left if valid.

### Right

Move one cell right if valid.

### Down

Move one cell down if valid.

If DOWN is not possible:

```text
lock piece
```

---

# 21. Horizontal Auto-Repeat

Touch controls should support holding LEFT or RIGHT.

When the player holds a directional button:

1. Move immediately.
2. Wait for a short initial delay.
3. Repeat movement at a consistent interval.

Suggested configuration:

```text
Initial delay: 150 ms
Repeat interval: 70 ms
```

These values should be configurable.

---

# 22. Soft Drop

Holding DOWN accelerates the active piece downward.

The piece continues to respect collision detection.

Do not allow it to pass through blocks.

Soft drop should award a small score bonus per successfully descended cell if enabled.

Recommended:

```text
+1 point per soft-dropped cell
```

---

# 23. Hard Drop

Hard drop should NOT be the primary control.

Because the target is classic gameplay, make Hard Drop optional.

If enabled:

```text
Calculate lowest valid Y
Move piece there
Lock immediately
```

Recommended bonus:

```text
+2 points per dropped cell
```

The setting can be:

```text
Hard Drop: ON/OFF
```

Default:

```text
OFF
```

---

# 24. Locking

When the active piece can no longer move downward:

1. Add its four blocks to the board.
2. Scan for complete lines.
3. Remove complete lines.
4. Update score.
5. Update line count.
6. Update level.
7. Spawn the next piece.
8. Check game-over condition.

The lock process must be atomic from the perspective of game state.

---

# 25. Line Clearing

A row is complete when all 10 cells are occupied.

Example:

```text
██████████
```

When a line is completed:

1. Remove the row.
2. Move all rows above it downward.
3. Insert an empty row at the top.

Support:

```text
1 line
2 lines
3 lines
4 lines
```

A four-line clear is a Tetris.

---

# 26. Scoring

Use classic scoring:

| Lines | Base |
|---|---:|
| Single | 100 |
| Double | 300 |
| Triple | 500 |
| Tetris | 800 |

Final points:

```text
points = baseScore × currentLevel
```

Examples:

```text
Level 1 Tetris = 800
Level 2 Tetris = 1600
Level 5 Tetris = 4000
Level 10 Tetris = 8000
```

Do not implement:

- Combo multiplier
- Back-to-back multiplier
- T-spin scoring
- Perfect-clear bonus

---

# 27. Lines Counter

Maintain:

```text
totalLinesCleared
```

This is displayed during gameplay.

Example:

```text
SCORE
18,450

LEVEL
12

LINES
121
```

---

# 28. Game Over

Game over occurs when the next piece cannot be legally spawned.

When this happens:

```text
gameState = GAME_OVER
```

Stop the game loop.

Disable gameplay controls.

Display:

```text
GAME OVER

SCORE
18,450

LEVEL
12

LINES
121
```

Then determine whether the score qualifies for the leaderboard.

---

# 29. Player Name

The application should maintain a local player name.

On first launch:

```text
ENTER YOUR NAME

[ SAMSON________ ]

       CONTINUE
```

Requirements:

- Maximum 15 characters.
- Trim whitespace.
- Do not allow empty names.
- Store locally.
- Allow editing later in Settings.

Do not ask for the name before every game.

---

# 30. High Score System

Maintain a local Top-10 leaderboard.

Each record contains:

```text
id
playerName
score
level
linesCleared
achievedAt
```

Use Room.

Suggested entity:

```kotlin
@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val playerName: String,

    val score: Int,

    val level: Int,

    val linesCleared: Int,

    val achievedAt: Long
)
```

---

# 31. High Score DAO

Provide operations equivalent to:

```text
insertScore()
getTopScores()
getLowestScore()
deleteScore()
deleteAllScores()
```

Primary leaderboard query:

```sql
SELECT *
FROM high_scores
ORDER BY score DESC, level DESC, achieved_at ASC
LIMIT 10
```

The repository must enforce the Top-10 limit.

---

# 32. High Score Qualification

When the game ends:

```text
if leaderboard has fewer than 10 entries:
    qualify

else if score > lowest leaderboard score:
    qualify

else:
    do not qualify
```

If the score qualifies:

```text
NEW HIGH SCORE
```

should be shown.

---

# 33. Tie Breaking

For equal scores:

1. Higher level ranks higher.
2. If level is equal, earlier achieved score ranks higher.

The ordering must be deterministic.

---

# 34. Leaderboard Screen

Display:

```text
HIGH SCORES

1  SAMSON    18,450
2  TUNDE     14,200
3  DAVID     11,850
4  AYO        9,400
5  MIKE       7,850
6  JOHN       6,900
7  PAUL       5,800
8  JAMES      4,700
9  EMMA       3,950
10 BEN        2,850
```

Selecting a score can display:

```text
PLAYER
SAMSON

SCORE
18,450

LEVEL
12

LINES
121

DATE
September 5, 2026
```

---

# 35. Personal Best

Display the current player's best score separately where useful.

Example:

```text
PERSONAL BEST
18,450
```

This should be calculated from locally stored scores.

---

# 36. Main Menu

The main menu should be minimal.

```text
TETRIS

PLAY

HIGH SCORES

SETTINGS

ABOUT
```

The PLAY button should immediately start a new game.

---

# 37. Gameplay UI

Primary layout:

```text
┌──────────────────────────────┐
│                              │
│ SCORE             LEVEL      │
│ 18,450              12       │
│                              │
│                 NEXT         │
│                 ┌────┐       │
│                 │    │       │
│                 │ ██ │       │
│                 │████│       │
│                 └────┘       │
│                              │
│        ┌────────────┐        │
│        │            │        │
│        │            │        │
│        │   TETRIS   │        │
│        │   BOARD    │        │
│        │            │        │
│        │            │        │
│        │            │        │
│        │            │        │
│        │            │        │
│        │            │        │
│        └────────────┘        │
│                              │
│  ◀        ↻        ▶        │
│                              │
│       ▼          DROP        │
│      DOWN                    │
│                              │
└──────────────────────────────┘
```

The actual implementation must dynamically calculate dimensions.

Never assume a fixed phone resolution.

---

# 38. Board Rendering

Use Android Canvas/Compose Canvas.

The renderer should:

1. Calculate board dimensions.
2. Calculate cell size.
3. Draw background.
4. Draw grid.
5. Draw locked blocks.
6. Draw active piece.
7. Draw borders/highlights.

Each cell should be square.

Do not stretch the board.

---

# 39. Retro Block Style

Blocks should have a slightly retro appearance.

Each block can contain:

- Main color
- Thin dark border
- Small highlight

Avoid:

- Realistic 3D
- Excessive gradients
- Glass effects
- Modern card UI around every block

The tetrominoes should look like physical arcade blocks.

---

# 40. Visual Theme

Use:

- Dark background
- Colorful blocks
- High contrast
- Simple typography
- Retro-inspired headings
- Minimal UI chrome

The aesthetic should be inspired by classic Nintendo-era Tetris without directly copying proprietary artwork, logos, fonts, or sprites.

Do not use the Nintendo logo.

Do not use copyrighted NES screenshots as application assets.

Create original graphics inspired by the era.

---

# 41. Grid

The board should have a subtle grid.

The grid must be visible enough to understand cell boundaries but should not overpower the blocks.

The blocks remain visually dominant.

---

# 42. Touch Controls

Provide large controls:

```text
LEFT
ROTATE
RIGHT

DOWN
DROP
```

Buttons must have sufficient touch targets for phones.

Recommended minimum touch target:

```text
48dp
```

Prefer larger controls where screen space allows.

---

# 43. Rotation Button

The rotate button performs one clockwise rotation.

Optional:

Long press may continuously rotate, but this should not be enabled initially.

A normal tap should perform exactly one rotation.

---

# 44. Gesture Controls

Gestures may be supported in addition to buttons.

Optional:

```text
Swipe left  → LEFT
Swipe right → RIGHT
Swipe down  → DOWN
Tap         → ROTATE
```

Buttons must remain available.

Gesture support must never make button controls unavailable.

---

# 45. Pause

Display a pause button during gameplay.

When pressed:

```text
GAME STATE → PAUSED
```

Stop gravity.

Stop movement timers.

Ignore gameplay input.

Display:

```text
PAUSED

RESUME

RESTART

QUIT
```

---

# 46. Restart

Restart must:

1. End current game.
2. Reset board.
3. Reset score.
4. Reset lines.
5. Reset level.
6. Generate new pieces.
7. Set state to PLAYING.

Ask for confirmation if the player is currently in an active game.

---

# 47. Sound Effects

Use short retro-inspired sound effects.

Suggested events:

```text
Piece move
Piece rotate
Piece lock
Line clear
Tetris
Level up
Game over
Button click
```

Keep sounds short.

Do not use copyrighted audio from Nintendo or existing Tetris releases.

Create or use appropriately licensed original sound effects.

---

# 48. Music

Music is optional.

If implemented:

- Use original or appropriately licensed retro-inspired music.
- Provide Music ON/OFF.
- Do not use copyrighted Nintendo/Tetris soundtrack recordings.

Gameplay must remain enjoyable without music.

---

# 49. Vibration

Optional haptic feedback:

- Piece lock
- Line clear
- Tetris
- Game over

Provide:

```text
Vibration ON/OFF
```

Do not continuously vibrate during gameplay.

---

# 50. Settings

Settings may include:

```text
Player Name
Sound Effects
Music
Vibration
Hard Drop
```

Keep Settings simple.

Do not introduce unnecessary configuration.

---

# 51. Persistence

Persist:

```text
Player Name
High Scores
Settings
```

Use:

- Room for high scores.
- DataStore for simple preferences/settings.

Do not store settings in the Room database unless there is a strong reason.

---

# 52. Offline Requirement

The entire application must work without internet.

There must be no dependency on:

- API
- Login
- Cloud
- Remote database
- Authentication
- Internet connectivity

The application should be fully functional in airplane mode.

---

# 53. Lifecycle Handling

The game must handle Android lifecycle events correctly.

When the application goes into the background:

```text
PLAYING → PAUSED
```

Do not allow the game to continue running invisibly.

When the player returns, show the paused state.

The player explicitly resumes the game.

This prevents unfair gameplay caused by backgrounding the application.

---

# 54. Screen Orientation

Use portrait orientation initially.

Do not dynamically rotate the game board based on device orientation.

---

# 55. Accessibility

Provide:

- Large touch targets
- Sufficient contrast
- Content descriptions for controls
- Scalable text where practical

Color must not be the only way to communicate UI state.

However, tetromino colors can remain part of gameplay because their shapes are also distinguishable.

---

# 56. Unit Testing

The game engine must have extensive unit tests.

Test:

## Board

- Empty board
- Occupied cell
- Valid position
- Invalid position

## Movement

- Left movement
- Right movement
- Down movement
- Left wall
- Right wall
- Bottom collision

## Rotation

- All tetrominoes
- Four rotations
- Rotation near walls
- Rotation near blocks
- O piece behavior

## Line Clearing

- Single
- Double
- Triple
- Tetris
- Multiple separated completed lines

## Scoring

Verify:

```text
Single = 100 × level
Double = 300 × level
Triple = 500 × level
Tetris = 800 × level
```

## Level

Verify:

```text
10 lines → level 2
20 lines → level 3
30 lines → level 4
```

## Game Over

Verify blocked spawn position.

## Piece Generator

Verify:

- All seven pieces occur.
- Seven-bag behavior.
- No duplicate piece inside one bag.

## High Scores

Verify:

- Insert score
- Sort scores
- Top 10
- Score qualification
- Tie breaking
- Delete scores

---

# 57. Deterministic Testing

The game engine must allow the piece generator to be replaced with a deterministic sequence during tests.

Example:

```kotlin
FakePieceGenerator(
    listOf(
        TetrominoType.I,
        TetrominoType.O,
        TetrominoType.T
    )
)
```

This makes gameplay tests repeatable.

Do not rely on random values in unit tests.

---

# 58. ViewModel

Create a game ViewModel responsible for connecting the engine to Compose.

Conceptually:

```text
User Input
    ↓
GameViewModel
    ↓
TetrisGame
    ↓
Game State
    ↓
StateFlow
    ↓
Compose UI
```

The ViewModel should not contain the low-level collision algorithms.

---

# 59. State Model

Expose immutable UI state.

Conceptually:

```kotlin
data class GameUiState(
    val gameState: GameState,
    val board: Board,
    val activePiece: ActivePiece?,
    val nextPiece: TetrominoType?,
    val score: Int,
    val level: Int,
    val linesCleared: Int
)
```

Compose should observe this state.

---

# 60. Game Loop

Do not use recursive delayed functions.

Use a coroutine-based game loop or equivalent controlled timing mechanism.

Conceptually:

```text
while game is PLAYING:

    wait(gravityInterval)

    if still PLAYING:
        move active piece down

    if movement impossible:
        lock piece
        clear lines
        update score
        update level
        spawn next piece
```

Pause must suspend gravity cleanly.

Avoid multiple simultaneous game loops.

There must only ever be one active gravity loop for a game session.

---

# 61. Threading

UI operations remain on the appropriate Android/UI context.

Game calculations should be lightweight.

Database operations must not block the UI thread.

Room operations should use appropriate coroutine dispatching.

---

# 62. Performance

Target smooth gameplay on low-end Android devices.

Avoid:

- Creating large objects every frame.
- Rebuilding the entire game engine during recomposition.
- Unnecessary database queries.
- Excessive animations.
- Excessive Canvas allocations.

The board contains only 200 cells, so prioritize simplicity and correctness over premature optimization.

---

# 63. Error Handling

The application must gracefully handle:

- Database errors
- Invalid persisted settings
- Corrupt/invalid high-score data
- Lifecycle interruptions

Never allow a high-score database problem to crash the entire game.

If persistence fails, gameplay should still be protected as much as practical.

---

# 64. Navigation

Use simple navigation:

```text
Main Menu
   │
   ├── Game
   │
   ├── High Scores
   │
   ├── Settings
   │
   └── About
```

Avoid unnecessary navigation complexity.

---

# 65. About Screen

Provide a simple About screen.

Example:

```text
CLASSIC TETRIS

A retro-inspired falling block game
for Android.

Version 1.0.0
```

Do not claim to be the official Nintendo or Tetris application.

---

# 66. Legal / Branding Constraint

The application is inspired by classic Tetris gameplay.

Do not copy:

- Nintendo logos
- Nintendo trademarks
- Nintendo sprites
- NES screenshots
- Original Tetris sound recordings
- Proprietary artwork
- Proprietary fonts

Create original assets with a retro-inspired aesthetic.

The application name should avoid implying official affiliation.

---

# 67. Recommended Application Name

Temporary development name:

```text
Classic Blocks
```

Alternative:

```text
Eazy Tetris
```

Final product naming can be decided separately.

Do not use "Nintendo Tetris" as the application name.

---

# 68. Build Requirements

The final project must:

- Open in Android Studio.
- Compile successfully.
- Pass unit tests.
- Generate a debug APK.
- Run on a physical Android device.
- Run without internet.

Use a current stable Android Gradle Plugin/Kotlin/Compose combination compatible with the development environment.

Do not unnecessarily pin obsolete dependency versions.

---

# 69. Development Phases

The coding agent MUST implement the project incrementally.

## Phase 1 — Project Setup

Create:

- Android project
- Kotlin
- Compose
- Package structure
- Basic MainActivity

Verify:

```text
./gradlew build
```

passes.

---

## Phase 2 — Core Models

Implement:

- Board
- Cell
- Tetromino
- TetrominoType
- RotationState
- ActivePiece
- GameState

Write tests.

Build.

---

## Phase 3 — Game Engine

Implement:

- Spawn
- Movement
- Collision
- Rotation
- Locking
- Line clearing

Write tests.

Build.

Do not continue if core tests fail.

---

## Phase 4 — Piece Generation

Implement:

- Seven-bag generator
- Next piece

Write deterministic tests.

Build.

---

## Phase 5 — Scoring and Levels

Implement:

- Score
- Lines
- Levels
- Gravity configuration

Write tests.

Build.

---

## Phase 6 — Compose Board

Implement:

- Game screen
- Canvas
- Board rendering
- Tetromino rendering
- Score
- Level
- Lines
- Next preview

Build and run on Android.

---

## Phase 7 — Controls

Implement:

- Left
- Right
- Rotate
- Down
- Optional hard drop

Test touch interaction.

---

## Phase 8 — Game States

Implement:

- Ready
- Playing
- Paused
- Game over
- Restart

Test lifecycle behavior.

---

## Phase 9 — High Scores

Implement:

- Room
- HighScoreEntity
- DAO
- Repository
- Player name
- Top 10
- Qualification
- Personal best

Write database tests.

---

## Phase 10 — Menus and Settings

Implement:

- Main menu
- High scores
- Settings
- About

Keep UI minimal.

---

## Phase 11 — Sound and Haptics

Implement optional:

- Sound effects
- Music
- Vibration

Add settings.

---

## Phase 12 — Visual Polish

Improve:

- Typography
- Block rendering
- Button design
- Spacing
- Animations
- Retro aesthetic

Do not modify game rules during visual polish without updating tests.

---

# 70. AI Agent Rules

The coding agent must follow these rules:

### Rule 1

Do not invent gameplay mechanics that are not specified.

### Rule 2

Do not introduce Hold, T-spins, combos, garbage lines, multiplayer, or online features.

### Rule 3

Do not put game logic inside Compose UI.

### Rule 4

Do not skip unit tests for the core game engine.

### Rule 5

Compile after each major phase.

### Rule 6

Fix compilation and test failures before moving to the next phase.

### Rule 7

Do not replace the core architecture simply because a faster implementation is possible.

### Rule 8

Avoid unnecessary dependencies.

### Rule 9

Do not use copyrighted Nintendo/Tetris assets.

### Rule 10

Do not require an internet connection.

### Rule 11

Keep the game playable even if optional audio/haptic features fail.

### Rule 12

Do not consider the project complete until an APK can be successfully generated.

# 73. Build & Release Status (2026-09-17)

### Current Progress:
- [x] Project Setup
- [x] Core Models
- [x] Game Engine
- [x] Scoring & Levels
- [x] Compose UI & Controls
- [x] Local Persistence (SharedPreferences implementation)
- [x] Build Fixes (JVM 17 alignment)
- [x] Custom Launcher Icon (`eazy-tetris.png`) scaled for adaptive display
- [x] Renamed to Eazy Tetris
- [x] Startup crash fix
- [x] Phase 10: Menus and Settings (Settings & About screens)
- [x] Phase 11: Sound and Haptics (Infrastructure)
- [x] Phase 12: Visual Polish (Animations & Retro Blocks)
- [x] UI Improvements: Icon scaling & Layout overflow fix
- [x] Edge-to-Edge display & Safe padding support
- [x] Touch Gestures: Tap to rotate, Swipe to move/drop

### Build Configuration:
- **JVM Target:** 17
- **Compile SDK:** 35
- **Kotlin Version:** 1.9.24
- **Compose:** Material 3

### Release Artifacts:
- `app-debug.apk` is the primary testing target.

The project is complete only when all of the following are true:

- [ ] Android project builds.
- [ ] APK can be generated.
- [ ] Application launches.
- [ ] Main menu works.
- [ ] New game works.
- [ ] Board is 10 × 20.
- [ ] Seven tetrominoes work.
- [ ] Tetromino colors are correct.
- [ ] Piece movement works.
- [ ] Rotation works.
- [ ] Collision detection works.
- [ ] Pieces lock correctly.
- [ ] Lines clear correctly.
- [ ] Four-line Tetris works.
- [ ] Score calculation is correct.
- [ ] Level progression is correct.
- [ ] Gravity increases with level.
- [ ] Next-piece preview works.
- [ ] Seven-bag generator works.
- [ ] Pause works.
- [ ] Resume works.
- [ ] Restart works.
- [ ] Game over works.
- [ ] Touch controls work.
- [ ] Player name is persisted.
- [ ] Top-10 leaderboard works.
- [ ] Personal best works.
- [ ] High scores survive application restart.
- [ ] Settings persist.
- [ ] Game works offline.
- [ ] Unit tests pass.
- [ ] No critical runtime crashes.
- [ ] Application works on a physical Android device.

---

# 72. Definition of Done

The game should feel like a complete small arcade game rather than a technical demonstration.

A new player should be able to:

```text
Open application
      ↓
Tap PLAY
      ↓
Immediately understand the board
      ↓
Move / rotate pieces
      ↓
Clear lines
      ↓
Increase level
      ↓
Eventually lose
      ↓
See score
      ↓
Enter/save name if they achieved a Top-10 score
      ↓
See leaderboard
      ↓
Tap PLAY AGAIN
```

The complete gameplay loop should be fast, simple and satisfying.

The core principle is:

> **Classic gameplay. Colorful blocks. Modern Android controls. No unnecessary complexity.**

# 73. Build & Release Status (2026-09-17)

### Current Progress:
- [x] Project Setup
- [x] Core Models
- [x] Game Engine
- [x] Scoring & Levels
- [x] Compose UI & Controls
- [x] Local Persistence (SharedPreferences implementation)
- [x] Build Fixes (JVM 17 alignment)
- [x] Custom Launcher Icon (`eazy-tetris.png`) scaled for adaptive display
- [x] Renamed to Eazy Tetris
- [x] Startup crash fix
- [x] Phase 10: Menus and Settings (Settings & About screens)
- [x] Phase 11: Sound and Haptics (Infrastructure)
- [x] Phase 12: Visual Polish (Animations & Retro Blocks)
- [x] UI Improvements: Icon scaling & Layout overflow fix
- [x] Edge-to-Edge display & Safe padding support
- [x] Touch Gestures: Tap to rotate, Swipe to move/drop

### Build Configuration:
- **JVM Target:** 17
- **Compile SDK:** 35
- **Kotlin Version:** 1.9.24
- **Compose:** Material 3

### Release Artifacts:
- `app-debug.apk` is the primary testing target.
