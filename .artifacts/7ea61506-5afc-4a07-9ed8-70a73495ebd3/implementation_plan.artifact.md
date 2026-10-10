# Implementation Plan - Level Selection & Game Over Navigation

This plan adds a level selector to the main menu and navigation buttons to the Game Over screen for a better user flow.

## User Review Required

> [!IMPORTANT]
> - **Level Selector**: I'll add a new control in the main menu to choose your starting level (1-15).
> - **Game Over Flow**: Instead of just seeing "GAME OVER", you will now see "RESTART" and "MENU" buttons directly on the board overlay.

## Proposed Changes

### Game Logic

#### [MODIFY] [Game.kt](file:///C:/laragon/www/Eazy-tetris/app/src/main/java/com/retroblocks/Game.kt)
- Update `start()` to `start(initialLevel: Int = 1)`.

#### [MODIFY] [MainActivity.kt](file:///C:/laragon/www/Eazy-tetris/app/src/main/java/com/retroblocks/MainActivity.kt)
- Update `GameViewModel.start()` to accept the starting level.

### UI Improvements

#### [MODIFY] [MainActivity.kt](file:///C:/laragon/www/Eazy-tetris/app/src/main/java/com/retroblocks/MainActivity.kt)
- **MenuScreen**:
    - Add a `selectedLevel` state.
    - Add a horizontal selector ( [ - ] LEVEL 1 [ + ] ) above the PLAY button.
- **GameScreen**:
    - Update the `GAME_OVER` overlay to include "RESTART" and "MENU" buttons.
    - Style these buttons to be smaller than the main gameplay controls but easy to tap.

## Verification Plan

### Automated Tests
- Build verification with `:app:assembleDebug`.

### Manual Verification
- **Level Start**: Select Level 5 in the menu, tap PLAY, and verify the game starts at Level 5 with faster gravity.
- **Game Over**: Fail a game and verify the "RESTART" button works and the "MENU" button takes you back.
