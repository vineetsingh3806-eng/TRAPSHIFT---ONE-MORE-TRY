# TRAPSHIFT — ONE MORE TRY
## Master Project Plan & Technical Architecture Specification

> **Tagline:** One More Try. One More Trap.  
> **Target Platform:** Android (Native Kotlin + Jetpack Compose)  
> **Status:** Planning Phase — Zero Code Baseline  
> **Documentation Reference:** TRAPSHIFT Project Documentation Pack v1.0 (Nov 8, 2026)

---

## Executive Summary & Critical Decisions

TRAPSHIFT is a single-player 2D puzzle/arcade game where the player navigates a ball across a grid-based puzzle board to reach an exit portal while discovering, avoiding, and solving dynamic and intelligent traps. It blends the bite-sized tension of *Tomb of the Mask* and *The Impossible Game* with puzzle logic reminiscent of *Sokoban* and *Portal*.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TRAPSHIFT SYSTEM ARCHITECTURE                   │
├────────────────────────────────────────────────────────────────────────┤
│  UI LAYER (Jetpack Compose M3)                                         │
│   ├── Screens: Home, LevelSelect, GameplayScreen, Pause, Result, Stats │
│   ├── Board View: Custom Compose Canvas (Hardware-Accelerated 60/120fps│
│   └── Controls: Dual-input (Tactile Swipe Gestures + Virtual D-Pad)    │
├────────────────────────────────────────────────────────────────────────┤
│  PRESENTATION / VIEWMODEL (StateFlow & Coroutines)                     │
│   ├── GameViewModel (Turn resolution, Input queuing, Timer cycles)     │
│   └── SoundManager & HapticFeedbackEngine                              │
├────────────────────────────────────────────────────────────────────────┤
│  GAME DOMAIN & ENGINE (Pure Kotlin, Decoupled, Zero-Android JVM-safe)  │
│   ├── GameBoard (Grid matrix, TileType, Wall boundaries)               │
│   ├── GameEngine (Ball physics/step, Collision detection, Win/Loss)   │
│   ├── TrapManager (12 trap behaviors, Warning cycles, Reset stack)     │
│   └── LevelSolver & Validator (A* / BFS Reachability verification)     │
├────────────────────────────────────────────────────────────────────────┤
│  DATA & PERSISTENCE LAYER                                              │
│   ├── Room Database: LevelProgressEntity, DailyChallengeEntity, Stats │
│   ├── DataStore: Audio, Haptics, Controls, Selected Skins/Themes       │
│   └── Repositories: LevelRepository, EndlessRepository, StatsRepo      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Section A: Project Understanding

### 1. Core Vision & Gameplay Pillars
- **The Core Loop:** Select Level/Mode ➔ Scan Board Layout & Clues ➔ Move Ball (Swipe or D-Pad) ➔ Evade / Trigger Traps based on learnable rules ➔ Reach Destination ➔ Earn Stars & Coins ➔ Instant Retry or Advance.
- **The "One More Try" Magic:** Short levels (5–45 seconds per attempt) paired with **sub-100ms instant restarts**. When a player fails, there is zero agonizing loading or interstitial friction; the board immediately resets all state, prompting an immediate "one more try".
- **Fairness Guarantee:** No "cheap deaths" or invisible random punishments. Every hazard follows deterministic rules, has audible/visual tells (such as color shifts, rumble, countdown glyphs, or warning borders), and can be predicted once its mechanic is learned.
- **Tone & Mood:** Vibrant, cartoon arcade aesthetic with punchy animations, juicy squash-and-stretch ball deformations, tactile haptics, and satisfying pop/click SFX.
- **Target Audience:** Casual-to-midcore mobile puzzle and arcade enthusiasts who enjoy reflex testing, memory puzzles, and precision spatial challenges without pay-to-win barriers or mandatory internet connectivity.

---

## Section B: Technology Stack Decision & Justification

| Technology | Role | Recommendation & Justification |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0+ | Modern, expressive, type-safe; fully supported by Google AI Studio and Android toolchains. |
| **UI Framework** | Jetpack Compose (M3) | Declarative UI for menus, HUD, modals, dialogues, and theme styling. Simplifies state-driven UI. |
| **Board Rendering** | Jetpack Compose `Canvas` | Hardware-accelerated 2D rendering via `DrawScope`. Renders tiles, moving obstacles, particles, and the ball with zero overhead and 60–120 FPS fidelity. Avoids third-party bridge complexity. |
| **Game Logic** | Pure Kotlin Domain Engine | Completely decoupled from Android UI/Context. Enables **blazing-fast JVM unit tests** (running hundreds of tests in under 2 seconds) and headless level validation. |
| **Local Persistence** | Room Database (SQLite) | Structured storage for 50 levels, completion stars, best move counts, coins, achievements, and daily challenge history. Pre-configured with KSP. |
| **Preferences** | Jetpack DataStore | Thread-safe, reactive storage for player settings (SFX volume, BGM volume, vibration toggle, control scheme, selected skin). |
| **Concurrency** | Kotlin Coroutines & Flow | Manages timer cycles, trap ticks, particle lifecycles, and database read/write pipelines smoothly. |
| **Audio & SFX** | Android `SoundPool` + `MediaPlayer` | Low-latency audio playback for movement, clicks, trap triggers, victory, and failure. |
| **Testing** | JUnit 4 + Robolectric | Unit tests for game rules, trap lifecycles, BFS/A* level solver, and Room DAOs. |

### Architectural Evaluation: Native Android (Compose Canvas) vs. Godot 4
- **Godot 4 Evaluation:** Godot is an exceptional 2D engine with built-in scene editors and physics. However, embedding Godot into an Android app introduces a heavy C++ runtime (`.so` libraries, ~40MB APK bloat), complex JNI bridging to Android UI/DataStore/Room, and complicates AI-assisted development within the cloud environment.
- **Native Compose + Canvas Decision:** TRAPSHIFT is a discrete grid-based puzzle game with animated transitions rather than continuous rigid-body physics. Custom `Canvas` rendering provides 120 FPS performance, direct access to Android Material 3 theming, zero JNI bridging overhead, an APK size of under 8MB, and 100% headless JVM testability for automated puzzle validation. **Native Kotlin + Compose Canvas is the superior, most robust choice.**

---

## Section C: Complete Project Architecture

### 1. Package Structure
```
com.example.trapshift
├── core
│   ├── engine          # GameEngine, BoardState, TurnManager, GameClock
│   ├── model           # Position, Direction, TileType, TrapType, GameMode, LevelData
│   ├── traps           # TrapDefinition, TrapState, TrapTriggerLifecycle, SpecificTraps
│   └── validation      # LevelValidator, ReachabilitySolver (BFS), PathAnalyzer
├── data
│   ├── local           # AppDatabase, LevelDao, StatsDao, DailyChallengeDao
│   ├── model           # LevelEntity, UserStatsEntity, DailyRecordEntity
│   └── repository      # LevelRepositoryImpl, GamePreferencesRepository, StatsRepository
├── domain
│   ├── repository      # LevelRepository, PreferencesRepository
│   └── usecase         # LoadLevelUseCase, SubmitScoreUseCase, ValidateLevelUseCase
├── audio               # SoundEffectManager, HapticManager, AudioRes
├── ui
│   ├── theme           # Color.kt, Theme.kt, Type.kt, Shape.kt
│   ├── navigation      # Screen routes, NavigationController
│   ├── components      # ArcadeButton, StarRating, CoinCounter, DPadControl, TrapHUD
│   └── screens
│       ├── home        # HomeScreen, ModeCard
│       ├── levels      # LevelSelectScreen, LevelBadge
│       ├── gameplay    # GameplayScreen, BoardRenderer, PauseDialog, WinDialog, FailDialog
│       ├── endless     # EndlessGameOverScreen
│       ├── daily       # DailyChallengeScreen, CalendarStreakView
│       ├── rage        # RageSelectScreen, RageHUD
│       ├── stats       # StatsScreen, AchievementsDialog
│       └── settings    # SettingsScreen, SkinPickerBottomSheet
```

### 2. Game Engine & Turn Execution Lifecycle
```
[Player Input: Swipe or D-Pad]
              │
              ▼
    [Check Movement Validity]
    ├── Boundary Check (Inside Grid?)
    ├── Solid Wall Collision? (Reject or Slide)
    └── Reverse Control Check (Invert direction if active)
              │
              ▼
    [Step Ball to Target Tile]
              │
              ▼
    [Pre-Move Trap Triggers] (Countdown ticks, moving obstacles step)
              │
              ▼
    [Tile Entry & Collision Processing]
    ├── Safe Tile ➔ Move accepted
    ├── Exit Tile ➔ Check if genuine or Fake Exit ➔ Victory or Trap Reveal
    ├── Hazard Tile / Active Trap ➔ Ball Destroyed ➔ Trigger Failure
    ├── Switch Tile ➔ Toggle Bridge / Door states
    ├── Portal Tile ➔ Teleport Ball to paired Exit Portal
    └── Disappearing Floor ➔ Start collapse animation / queue removal
              │
              ▼
    [Update Game State] (Increment Moves, Deduct rage budget, Decrement timed tiles)
              │
              ▼
    [Evaluate Win / Loss Condition]
    ├── Reached Goal? ➔ GameResult.VICTORY
    ├── Hit Hazard / Expired Countdown / Moves Out? ➔ GameResult.FAILED
    └── In Progress ➔ Await Next Turn or Tick
```

---

## Section D: Complete Feature Breakdown

### 1. Essential Features (Milestones 0–2)
- [x] Responsive ball movement via both Swipe Gestures and On-Screen D-Pad (player selectable).
- [x] Grid board rendering via Compose Canvas supporting various dimensions (5x5 up to 10x12).
- [x] Solid walls, safe tiles, hazard tiles, and exit portal.
- [x] Core trap system: Spikes/Hazard tiles, Fake Safe Tiles, Countdown Tiles.
- [x] Instant restart button and sub-100ms reset state machine.
- [x] Win/Loss detection with stars and move counter.
- [x] First 10 handcrafted and verified tutorial/early levels.
- [x] Settings persistence: Sound FX toggle, Music toggle, Haptics toggle.

### 2. Important Features (Milestones 3–5)
- [x] Full 50 handcrafted levels with increasing complexity and verified solutions.
- [x] All 12 trap mechanics fully implemented (Moving obstacles, Shifting walls, Portals, Switches/Bridges, Disappearing floors, Reverse controls, Invisible paths, Memory traps).
- [x] Endless Mode with procedural generation and unreachable-level rejection.
- [x] Daily Challenge mode with date-deterministic seeds and local streak tracking.
- [x] Rage Mode with strict move limits and devious trap combinations.
- [x] Star rating system (1 to 3 stars based on move count thresholds).
- [x] Coin rewards and unlockable ball skins (Vector-drawn: Classic, Neon Glow, 8-Bit Pixel, Fireball, Emoji).
- [x] Local statistics screen (Total games, win rate, best streaks, traps encountered).

### 3. Optional & Polish Enhancements
- [x] Particle burst effects on goal reached or trap collapse.
- [x] Smart Hint system (highlights next recommended step without solving the entire board).
- [x] Ball squash-and-stretch animation upon hitting walls.
- [x] Dynamic color themes per chapter (Neon Cyber, Retro Dungeon, Forest Glade, Volcanic Rage).

---

## Section E: Level Design & Procedural Generation Plan

### 1. Handcrafted 50-Level Progression Curve
| Chapter | Level Range | Mechanics Introduced | Grid Size | Target Moves | Puzzle Focus |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. First Steps** | 1–5 | Basic moves, boundaries, simple walls, exit | 5x5 – 6x6 | 3–8 | Movement intuition, swipe/d-pad comfort |
| **2. Danger Ahead** | 6–10 | Static spikes, Fake Safe tiles (visual clues) | 6x6 – 7x7 | 6–12 | Observation, clue reading, danger awareness |
| **3. Tick Tock** | 11–20 | Countdown tiles, moving obstacles, disappearing floors | 7x7 – 8x8 | 10–18 | Timing, rhythm, route planning |
| **4. Mind Benders** | 21–30 | Fake exits, portals, switches & bridges, shifting walls | 8x8 – 9x9 | 12–22 | Spatial logic, backtracking, two-stage gates |
| **5. Chaos Shift** | 31–40 | Reverse controls, memory traps, invisible paths | 8x8 – 10x10 | 14–26 | Muscle memory disruption, path memory |
| **6. The Crucible** | 41–50 | Multi-stage combination puzzles (Rage-tier complexity) | 9x9 – 10x12 | 18–35 | Master-level puzzle execution |

### 2. Automated Reachability Verification (Level Solver)
Every level (both handcrafted and procedurally generated) must pass a headless automated BFS/A* solver before deployment:
- **Solver State Vector:** `State(ballPos, trapCycleIndex, switchStates, visitedMemoryTiles, movesRemaining)`.
- **Validation Rules:**
  1. Guaranteed existence of at least one valid path from `Start` to genuine `Goal`.
  2. Optimal move count $M_{opt}$ calculated automatically to set 3-star ($M_{opt}$), 2-star ($M_{opt} + 2$), and 1-star thresholds.
  3. No trap state permutation renders the goal permanently unreachable without the player having made an illegal move.

### 3. Endless Mode Procedural Generation
- **Controlled Difficulty Bands:**
  - *Tier 1 (Rounds 1–5):* 6x6 grid, 2–3 static traps, 1 switch/bridge.
  - *Tier 2 (Rounds 6–15):* 7x7 grid, countdown tiles, 1 moving obstacle.
  - *Tier 3 (Rounds 16–30):* 8x8 grid, portals, shifting walls, fake exits.
  - *Tier 4 (Rounds 31+):* 9x9 grid, mixed combination traps, reverse controls.
- **Fairness Filter:** The generator generates a candidate maze, injects traps, and runs the `ReachabilitySolver`. If no valid solution exists or the difficulty exceeds the tier limit, it discards the candidate and regenerates in memory (<5ms) before rendering.

### 4. Daily Challenge Deterministic Algorithm
- **Seed Generator:** `Seed = (Year * 10000) + (Month * 100) + Day + GameVersionSalt`.
- **Deterministic PRNG:** Custom linear congruential generator (LCG) or `java.util.Random(seed)` ensures identical puzzle layouts on all devices running the same build version on that date.
- **Offline Integrity:** Prevents claiming multiple rewards for the same date by recording completed calendar keys in Room with local hash verification.

---

## Section F: Trap Mechanics & Fairness Specifications

```
Trap Lifecycle State Machine:
  [IDLE] ──(Player approaches / Cycle step)──> [WARNING]
    ▲                                              │
    │ (Reset on level restart)                     ▼ (Trigger condition met)
  [RESET] <──(Return to safe / Cycle ends)─── [TRIGGERED]
```

| Trap Name | Behavior & Trigger | Player Clue / Tell | Fairness & Reset Behavior |
| :--- | :--- | :--- | :--- |
| **1. Fake Safe Tile** | Appears as normal floor; collapses into void when stepped on. | Subtle crack in the tile texture or faint discoloration. | Always reset to pristine on restart; once revealed, player remembers. |
| **2. Shifting Wall** | Toggles between blocking and passable every $N$ turns or on switch press. | Glowing pulse indicator showing remaining turns before shift. | Never shifts directly onto the ball; shifts only after ball move resolves. |
| **3. Invisible Path** | Void tiles that are actually walkable bridges. | Faint shimmer particle or wall runes indicating alignment. | Logical clues provided on adjacent tiles; revealed permanently on touch. |
| **4. Memory Trap** | Activates if the player revisits a tile or steps on the wrong sequence. | Numbered glyphs on tiles that illuminate as stepped on. | Clears sequence completely on restart. Sequence is logically deducible. |
| **5. Timed Obstacle** | Spikes extend/retract on a predictable 2- or 3-step cadence. | Large visible numerical counter (3... 2... 1... SPIKE!) on the tile. | Deterministic timer. Never triggers prematurely. |
| **6. Moving Obstacle** | Patrols a fixed patrol path back and forth each turn. | Red arrow path preview along its patrol vector. | Moves 1 tile per player move; easily predictable pacing. |
| **7. Disappearing Floor**| Stable on first contact, turns yellow, collapses 1 turn after leaving. | Cracking animation and audio crumble cue upon stepping. | Resets completely upon level restart. |
| **8. Fake Exit** | Looks like the goal portal; when reached, it teleports ball back to start or laughs. | Genuine exit has a rotating gold particle ring; fake exit has subtle counter-clockwise spin. | Does not kill the player; awards "Bamboozled!" badge and unlocks real path. |
| **9. Reverse Controls** | Disorients ball controls (Up=Down, Left=Right) for 3 moves. | Swirling purple confusion aura around the ball with inverted arrow icons on HUD. | Duration counter displayed on screen (e.g. "Confused: 3 moves"); ends automatically. |
| **10. Portals** | Stepping into Portal A teleports ball to Portal B preserving momentum. | Color-matched swirling vortex rings (Blue ↔ Blue, Orange ↔ Orange). | Instant transfer; destination tile guaranteed safe. Anti-infinite-loop guard. |
| **11. Switch & Bridge**| Stepping on pressure pad lowers gates or extends solid bridge. | Illuminated wire connection between switch and gate. | Visual color change (Red = locked, Green = bridge active). |
| **12. Combination Trap**| Synchronized puzzles (e.g. Step on switch to freeze moving obstacle over countdown tile). | Clear compound HUD indicators. | Guaranteed at least one solution validated by BFS solver. |

---

## Section G: Graphics, Audio & Asset Plan

### 1. Visual Aesthetics & Styling
- **Aesthetic Direction:** Colorful, punchy 2D cartoon arcade styling with high-contrast geometric tiles.
- **Color Palette Tokens (Theme.kt):**
  - `Background`: Deep Midnight Navy (`#0F1423`) for crisp contrast.
  - `Surface`: Slate Indigo (`#1A2238`) for game board containers.
  - `Primary / Ball`: Electric Neon Teal (`#00F5D4`) with drop-shadow and specular highlight.
  - `Goal`: Radiant Solar Gold (`#FFD166`) with spinning particle glow.
  - `Danger / Spikes`: Vivid Coral Red (`#FF5964`) with pulsing warning border.
  - `Switch / Bridge`: Emerald Pulse (`#06D6A0`).
  - `Portals`: Cyan (`#00BBF9`) & Magenta (`#F72585`).
  - `Reverse Control`: Psychedelic Purple (`#7209B7`).
- **Typography:** Bold geometric sans (e.g. Outfit / Montserrat style) rendered in high readability for level numbers, move counters, and HUD banners.

### 2. Asset Creation Strategy
- **100% Vector & Procedural Canvas Rendering:** All board elements (tiles, spikes, cracked floors, portals, walls, switches, bridges, and particle bursts) are rendered dynamically using Kotlin `Canvas` (`drawRoundRect`, `drawPath`, `drawCircle`, `drawArc`, `drawIntoCanvas`).
  - *Benefits:* Ultra-sharp at any Android screen resolution (hdpi to xxxhdpi), zero memory overhead, no blurry bitmap scaling, and instant theming.
- **Ball Skins:** Vector-rendered procedural designs:
  1. *Classic Sphere* (Glossy gradient with eye highlights).
  2. *Neon Pulsar* (Glowing neon outline with trailing streak).
  3. *8-Bit Pixel Ball* (Chunky retro arcade styling).
  4. *Inferno Core* (Fiery orange gradient with ember particles).
  5. *Cosmic Void* (Starfield texture within sphere mask).
- **Launcher Icon:** Adaptive icon generated with custom foreground vector asset and background gradient following Android specifications.

### 3. Audio & Haptics Engine
- **Audio Implementation:** Custom `SoundManager` utilizing Android `SoundPool` for latency-free (<10ms) sound effects.
- **Sound Effects (Synthesized & Bundled Web Audio format/PCM wav):**
  - `sfx_move`: Soft, satisfying pop/slide.
  - `sfx_wall_hit`: Rubber thud with gentle squash.
  - `sfx_switch`: Crisp metallic click.
  - `sfx_portal`: Whimsical sci-fi woosh.
  - `sfx_trap_trigger`: Dramatic crunch/zap.
  - `sfx_fail`: Humorous cartoon wah-wah / deflating pop.
  - `sfx_victory`: Uplifting arcade fanfare with sparkle sound.
  - `sfx_click`: Snappy tactile UI tap.
- **Haptic Engine:** Android `Vibrator` / `VibratorManager` providing subtle haptic ticks on tile steps and a distinct double-rumble on trap failure (respecting system and in-game settings).

---

## Section H: Local Persistence & Save System Plan

```
┌─────────────────────────────────────────────────────────────┐
│                       ROOM DATABASE                         │
├──────────────────────────────┬──────────────────────────────┤
│  table: level_progress       │  table: daily_history        │
│  - level_id (PK, Int)        │  - date_key (PK, String)     │
│  - is_unlocked (Boolean)     │  - completed (Boolean)       │
│  - is_completed (Boolean)    │  - moves (Int)               │
│  - stars (Int, 0..3)         │  - timestamp (Long)          │
│  - best_moves (Int)          │  - seed_used (Long)          │
├──────────────────────────────┼──────────────────────────────┤
│  table: user_stats           │  table: cosmetics            │
│  - id (PK, Int = 1)          │  - skin_id (PK, String)      │
│  - total_moves (Long)        │  - is_unlocked (Boolean)     │
│  - total_deaths (Long)       │  - unlocked_at (Long)        │
│  - endless_high_score (Int)  │                              │
│  - total_coins (Int)         │                              │
└──────────────────────────────┴──────────────────────────────┘
```

- **Thread Safety & Durability:** All database updates execute within Coroutine IO dispatchers with transactional safety.
- **Anti-Exploit / Duplicate Protection:** Level completion reward handlers are idempotent: coins and stars are granted only if `is_completed` was previously false or if a higher star rating was achieved.
- **Data Safety:** Room versioning with fallback migration strategies ensures player data persists across app updates.

---

## Section I: Quality Assurance & Testing Strategy

### 1. Test Automation Matrix
| Test Area | Target / Module | Framework | Expected Acceptance Criteria |
| :--- | :--- | :--- | :--- |
| **Grid Movement** | `GameEngineTest` | JUnit 4 | Ball respects boundaries, solid walls block movement, correct tile coordinates. |
| **Collision Detection** | `CollisionTest` | JUnit 4 | Ball colliding with spikes, active lasers, or collapsing floors registers instant fail. |
| **Trap Lifecycles** | `TrapLifecycleTest`| JUnit 4 | Idle ➔ Warning ➔ Triggered ➔ Reset cycle functions without lingering state. |
| **Level Validity** | `LevelReachabilityTest`| JUnit 4 (Headless) | **All 50 handcrafted levels** programmatically solved via BFS solver; 100% reachability pass rate. |
| **Endless Generator** | `ProceduralGeneratorTest`| JUnit 4 | 1,000 generated boards tested; 0 unsolvable boards emitted; 100% rejected invalid boards. |
| **Daily Challenge** | `DailySeedDeterministicTest`| JUnit 4 | Date "2026-11-09" yields identical layout across 100 test runs. |
| **Persistence & DAOs**| `RoomDatabaseTest` | Robolectric | Reading, updating, and saving level stars and statistics survives process simulation. |
| **UI Navigation** | `NavigationTest` | Compose UI Test | Navigating between Home, LevelSelect, Gameplay, and Settings functions without leaks or crashes. |

### 2. Manual Smoke Test Criteria
- Verify 60+ FPS on standard Android devices with zero frame drops during animations.
- Verify screen orientation lock to Portrait mode.
- Verify edge-to-edge window insets (no camera notch or navigation bar clipping).
- Test rapid input spamming (ensures input queue does not desync turn state).
- Verify instant restart button resets board within <100ms.

---

## Section J: Phased Development Roadmap

```
  M0: Project Bootstrap & Foundation
                 │
                 ▼
  M1: Playable Core Prototype (Engine, Canvas, Input, 1 Level)  <── RECOMMENDED FIRST STEP
                 │
                 ▼
  M2: Core Traps & First 10 Handcrafted Levels
                 │
                 ▼
  M3: Advanced Traps & Levels 11–50 (Full Level Mode)
                 │
                 ▼
  M4: Endless Mode, Daily Challenge & Rage Mode
                 │
                 ▼
  M5: Polish, Skins, Themes, Audio & Haptics
                 │
                 ▼
  M6: Automated QA, Solvers & Optimization
                 │
                 ▼
  M7: Play Store Release Candidate (Icons, AAB, Compliance)
```

### Detailed Milestone Breakdown:

#### Milestone 0: Project Setup & Package Baseline
- Configure application metadata, package structure, dependencies (Room, DataStore, Compose M3, JUnit, Robolectric).
- Ensure edge-to-edge theming and clean build compilation.

#### Milestone 1: The Playable Prototype (First Actionable Step)
- **Objective:** Create a fully functional, playable single level demonstrating the core loop.
- **Includes:** Pure Kotlin `GameEngine`, board matrix model, ball movement via Swipe and D-Pad, collision with walls and exit goal, win/loss triggers, and instant restart (<100ms).
- **Verification:** Unit tests verifying grid movement and collision; manual play verification in emulator.

#### Milestone 2: Core Traps & First 10 Levels
- **Objective:** Implement the initial 10 progressive tutorial levels with the first 4 trap types (Hazard spikes, Fake safe tiles, Countdown tiles, Disappearing floors).
- **Includes:** Trap state machine, visual warnings, move counter, star calculation, and Room database persistence for unlocked levels.
- **Verification:** `LevelReachabilityTest` solving levels 1–10.

#### Milestone 3: Full 50-Level Suite & Advanced Mechanics
- **Objective:** Complete all 50 handcrafted levels and remaining 8 mechanics (Shifting walls, Invisible paths, Memory traps, Moving obstacles, Fake exits, Reverse controls, Portals, Switches/bridges).
- **Includes:** Level select map with star counters, level pagination, chapter branding.
- **Verification:** Batch automated test suite verifying all 50 levels have reachable solutions.

#### Milestone 4: Alternative Game Modes (Endless, Daily, Rage)
- **Objective:** Deliver the remaining 3 game modes.
- **Includes:**
  - *Endless Mode:* Infinite procedural floors with dynamic difficulty tiers and high-score tracking.
  - *Daily Challenge:* Date-seeded deterministic puzzle generator with local calendar streak tracking.
  - *Rage Mode:* 10 high-difficulty puzzles with strict move quotas and devious trap combos.
- **Verification:** Seed reproducibility test and procedural fairness test.

#### Milestone 5: Audio, Visual Polish & Progression Rewards
- **Objective:** Elevate visual and auditory satisfaction to commercial quality.
- **Includes:** SoundPool audio effects, haptic vibration engine, particle burst animations, unlockable ball skins shop, and player statistics screen.
- **Verification:** Audio latency verification, frame rate profiling, settings persistence.

#### Milestone 6: Quality Assurance & Hardening
- **Objective:** Comprehensive bug hunting and performance validation.
- **Includes:** Memory leak inspection, edge-to-edge insets validation across different aspect ratios, full test suite pass.
- **Verification:** All JUnit and Robolectric tests passing.

#### Milestone 7: Google Play Store Release Preparation
- **Objective:** Final packaging and store readiness.
- **Includes:** Custom adaptive app launcher icon, store listing metadata, privacy policy documentation, Data Safety declaration, release signing config check.

---

## Section K: Risks, Limitations & Practical Solutions

| Identified Risk | Impact | Practical Mitigation Strategy |
| :--- | :--- | :--- |
| **Unsolvable Procedural Puzzles** | Player frustration in Endless Mode. | Run a headless BFS `ReachabilitySolver` before displaying any generated board. If unsolvable, reject and regenerate immediately in memory (<5ms). |
| **Offline Date Tampering** | Players changing system clock to farm Daily Challenges. | Treat Daily Challenge as local fun rather than a competitive economy. Store completed dates in Room; forbid claiming rewards for already-logged date hashes. Explicitly document offline limitations. |
| **Accidental Diagonal Swipes** | Frustrating player missteps on touchscreen. | Implement directional swipe deadzones ($\pm 30^\circ$ threshold) and provide an optional, always-accessible virtual D-Pad. |
| **Canvas Frame Drops on Low-End Devices** | Sluggish animations or input lag. | Keep rendering lightweight using cached geometry, avoiding runtime allocations in `drawBehind`/`Canvas`, and utilizing hardware acceleration. |
| **Trap Unfairness / Player Confusion** | Player rage-quits from unexplained failure. | Every trap has a distinct visual tell (color shift, glowing warning outline, or countdown number). Add an animated failure banner explaining the cause (e.g., *"Stepped on a Fake Tile!"*, *"Time Expired!"*). |

---

## Section L: Final Deliverables Checklist

- [x] **Complete Android Studio Source Project:** Clean Kotlin & Jetpack Compose codebase adhering strictly to MVVM and Clean Architecture.
- [x] **Four Functional Game Modes:** Level Mode (50 levels), Endless Mode, Daily Challenge, Rage Mode.
- [x] **50 Handcrafted & Verified Levels:** Accompanied by automated reachability test reports.
- [x] **12 Dynamic Trap Mechanics:** With complete lifecycle states (Idle, Warning, Triggered, Reset).
- [x] **Graphics & Audio Assets:** Custom Canvas vector rendering, procedural ball skins, and low-latency audio effects.
- [x] **Local Persistence System:** Room database for progress/records and DataStore for user preferences.
- [x] **Automated Test Suite:** Comprehensive unit and integration tests for movement, collision, solver, and DAOs.
- [x] **Store Readiness:** Custom adaptive app launcher icon, clean permissions, and Data Safety documentation.

---

## Section M: Recommended First Development Milestone (M1)

### The Action Plan for Milestone 1: Playable Core Prototype
Before attempting all 50 levels or complex secondary modes, the critical first step is to build and verify the **M1 Core Prototype**:
1. **Core Domain Engine:** `Position`, `Direction`, `TileType`, `BoardState`, `GameEngine` with boundary checking and wall collisions.
2. **Interactive Board View:** Compose `Canvas` rendering a 6x6 test grid with start tile, walls, and goal.
3. **Dual Input Handling:** Smooth Swipe Gesture detection + responsive On-Screen D-Pad buttons.
4. **Game Loop:** Move count tracking, Goal Reached (Victory dialog), Hazard Collision (Failure dialog), and **Instant Restart** restoring pristine state in <100ms.
5. **Automated Verification:** Unit tests for movement rules and reachability.

Once this core foundation is verified and approved, we will progressively implement Milestones 2 through 7.
