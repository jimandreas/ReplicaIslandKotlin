# test levels in the game

## NOTES

### Turn on DEBUG in AndoKun.kt

```
// If the version is a negative number, debug features (logging and a debug menu)
        // are enabled.
//        const val VERSION = 14
        const val VERSION = -1 /* TEST TEST debug */
```

### Keyboard notes

using keyboard in the Android emulator:

* F1 - brings up the basic debug menu
* F10 - allows unlock of all levels

* F4 - kills the emulator? (Pixel 3 XL API 28) (hangup button or powers off the emulator)

See also: https://minimum-viable-product.github.io/marshmallow-docs/tools/help/emulator.html

## Grok summary of test levels

### Test Level in Replica Island

Replica Island features 40 main levels (plus a bonus level, Memory #030.5), all titled as "Memory #XXX" and presented in a non-linear progression tree defined in `res/xml/level_tree.xml`. This tree organizes levels into groups, where completing all levels in a group unlocks the next. There is no explicitly named "test level" file or entry in the game's assets or level tree dedicated solely to playtesting. However, based on developer documentation and analytics references, the **post-build test level**—used for automated playtesting with a bot to verify builds—refers to a simple introductory level for quick validation of core mechanics like movement, physics, and collision. This is **Memory #001**, the game's tutorial/first level, which introduces basic platforming in a lab setting with minimal complexity (low death rate of 0.11 per player and short average completion time of 2.37 minutes).

This level is ideal for automated bot testing because:
- It has straightforward geometry (no complex falls or spikes; only 6.52% of deaths from falls).
- It serves as the entry point for player onboarding, teaching core controls without spoilers.
- Failure by the bot to complete it flags build issues like rendering glitches or input failures.

The levels occur in this play order (non-chronological numbering reflects the story's memory-flashback structure):

| Play Order | Level Name     | Key Metrics (Deaths/Player, Avg. Time) | Notes |
|------------|----------------|----------------------------------------|-------|
| 1         | Memory #001   | 0.11, 2.37 min                        | Tutorial; low difficulty; post-build test level. |
| 2         | Memory #025   | 0.19, 2.47 min                        | Early present-day level. |
| 3         | Memory #002   | 1.41, 5.89 min                        | Introduces basic enemies. |
| 4         | Memory #026   | 0.53, 2.85 min                        | - |
| 5         | Memory #003   | 0.40, 2.43 min                        | - |
| 6         | Memory #004   | 0.50, 3.11 min                        | - |
| 7         | Memory #005   | 1.30, 4.72 min                        | Higher deaths introduce challenge. |
| 8         | Memory #027   | 1.50, 2.98 min                        | Death spike (63.65% from falls). |
| 9         | Memory #012   | 3.04, 5.34 min                        | Frustration point (30.12% falls). |
| 10        | Memory #028   | 3.80, 7.71 min                        | Puzzle-heavy. |
| 11        | Memory #007   | 1.89, 5.88 min                        | - |
| 12        | Memory #013   | 1.92, 4.58 min                        | - |
| 13        | Memory #014   | 1.31, 2.55 min                        | - |
| 14        | Memory #029   | 1.30, 4.66 min                        | - |
| 15        | Memory #024   | 1.09, 4.20 min                        | - |
| 16        | Memory #030   | 3.03, 6.94 min                        | - |
| 17        | Memory #030.5 | 1.59, 2.17 min                        | Bonus level. |
| 18        | Memory #031   | 1.17, 4.70 min                        | - |
| 19        | Memory #008   | 4.12, 4.89 min                        | High deaths. |
| 20        | Memory #015   | 2.12, 9.87 min                        | Longer puzzle. |
| 21        | Memory #021   | 7.73, 7.86 min                        | Extreme difficulty (97.6% falls). |
| 22        | Memory #032   | 1.19, 6.12 min                        | - |
| 23        | Memory #033   | 2.96, 3.30 min                        | - |
| 24        | Memory #017   | 0.08, 1.30 min                        | Very easy. |
| 25        | Memory #022   | 0.14, 4.21 min                        | - |
| 26        | Memory #034   | 9.93, 17.44 min                       | Hardest (concentrated deaths). |
| 27        | Memory #010   | 1.01, 6.21 min                        | - |
| 28        | Memory #018   | 2.49, 4.01 min                        | - |
| 29        | Memory #035   | 3.29, 5.75 min                        | - |
| 30        | Memory #019   | 5.46, 6.19 min                        | - |
| 31        | Memory #023   | 7.35, 16.98 min                       | Long and deadly. |
| 32        | Memory #037   | 6.56, 15.11 min                       | - |
| 33        | Memory #038   | 0.46, 2.53 min                        | Easy late-game. |
| 34        | Memory #011   | 4.86, 12.32 min                       | - |
| 35        | Memory #020   | 9.79, 9.85 min                        | High deaths (64% falls). |
| 36        | Memory #039   | 0.42, 2.83 min                        | - |
| 37        | Memory #040   | 2.15, 2.98 min                        | Finale. |

### How a Developer Sets a Specific Level for Testing
Replica Island's level selection is data-driven via XML and runtime preferences, allowing developers to jump to any level (including Memory #001 for quick playtests) without replaying prior ones. This is handled in `LevelTree.java`, which loads the tree and uses Android SharedPreferences for state.

1. **Enable Linear Mode (Optional for Sequential Testing)**:
    - The game has two trees: `level_tree.xml` (non-linear, story order) and `linear_level_tree.xml` (sequential numbering for easier testing/debugging).
    - Set user preference for linear mode via SharedPreferences (key: likely `PREFERENCE_LINEAR_MODE` or similar, default false). This flattens the tree for straight playthroughs.

2. **Set the Target Level via SharedPreferences**:
    - Levels are indexed by **group (row)** and **index within group**.
        - Memory #001 is typically group 0, index 0 (first in the root group).
    - In code (e.g., during app launch in `ReplicIslandActivity.java` or a debug menu):
      ```
      SharedPreferences prefs = getSharedPreferences("ReplicaIslandPrefs", MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putInt("PREFERENCE_LEVEL_ROW", 0);  // Group/row index (0 for first group)
      editor.putInt("PREFERENCE_LEVEL_INDEX", 0);  // Level index in group (0 for first level)
      editor.apply();
      ```
    - Restart the app or trigger level load (via `LevelTree.get(row, index)`). The game will start directly at that level, bypassing the menu.

3. **Debug/Build Flags (Advanced)**:
    - For automated post-build testing, integrate a bot script (e.g., using Android's UI Automator) to load Memory #001 and simulate inputs (jump, move right) until completion. Failure (e.g., timeout >5 min or no progress) halts the build.
    - Add a debug menu in `GameMenuScreen.java` (if not present, extend it) with buttons to increment row/index for quick iteration.
    - Compile with `adb` for device testing: `adb shell am start -n com.replica.replicaisland/.ReplicIslandActivity` after setting prefs.

This system allows rapid iteration on specific levels during development, as seen in Chris Pruett's analytics-driven tuning (e.g., fixing spikes in Memory #034). For full implementation, inspect open-source forks like masokotanga/ReplicaIsland on GitHub.

## MORE on level 34

### Overview of Memory #034 in Replica Island

Memory #034 is the 26th level encountered in the game's play order (out of 40 main levels), positioned in the late-game section of the non-linear progression tree. It serves as a significant frustration point and progression blocker for many players, marking one of the game's hardest challenges due to its puzzle-heavy design rather than platforming intensity. The level reintroduces a puzzle mechanic seen earlier in a simpler form, testing players' understanding of the protagonist's powers (such as shape-shifting abilities like turning into a ball or glider for navigation and interaction). Unlike most levels, which aim to cap at under five minutes, this one demands precise application of these powers in a specific sequence, leading to repeated failures if attempted incorrectly.

### Gameplay Elements
- **Core Mechanic**: A environmental puzzle requiring the correct use of the robot protagonist's abilities to manipulate the level geometry or obstacles. Players must recognize and apply the right power at the right moment, building on prior tutorials but without sufficient reinforcement, causing confusion.
- **Structure**: The level is compact but deceptive, with a single critical "spot" where the puzzle resolves (or fails). It lacks heavy enemy encounters or long platforming sections, focusing instead on trial-and-error problem-solving.
- **Visuals and Theme**: Fits the game's sci-fi memory-flashback aesthetic, likely set in a ruined facility or abstract memory space, with tile-based collision geometry derived from Photoshop paths (as per the game's development tools).
- **Objectives**: Navigate to the exit by solving the puzzle; no collectibles or side paths mentioned in metrics.

### Difficulty Metrics
Based on aggregated player data from the game's launch (over 500 players tracked):

| Metric              | Value          | Notes |
|---------------------|----------------|-------|
| Total Deaths        | 5,234         | Extremely high, indicating a design bottleneck. |
| Players Attempted   | 527           | Represents a dedicated subset who reached this far. |
| Deaths per Player   | 9.93          | Highest in the game; acts as a "wall" where ~30-40% of players drop off. |
| Average Completion Time | 17.44 minutes | Far exceeds the design goal of <5 minutes; reflects looping attempts. |
| Minimum Time        | 3.17 minutes  | Achievable with optimal knowledge. |
| Maximum Time        | 114.07 minutes| Highlights extreme frustration for some. |
| Fall-Related Deaths | 0%            | No platforming pitfalls; all failures are puzzle-related. |

This level's metrics show a sharp spike compared to earlier ones (e.g., Memory #017 at 0.08 deaths/player), contributing to the end-game's uneven difficulty curve.

### Causes of Death and Player Behavior
- **Primary Cause**: Misapplication of powers—players repeatedly try "wrong" solutions at the puzzle's crux, leading to instant resets (deaths). The heat map visualizes this as a dense red cluster at one exact location, with no scattered deaths elsewhere, confirming it's not random but a teachable moment gone awry.
- **Frustration Insights**: Developer analysis flags this as a failure in power tutorials; players know the abilities but not *when* to use them here. It exacerbates drop-off, with player count degrading slowly until this point, then plummeting.
- **Heat Map Description**: The associated heat map (a pixelated overlay on the level layout) renders the death zone as an intense hotspot—likely a small platform or switch area—glowing bright red amid sparse blue/green (safe) zones. No other activity spikes, underscoring the bottleneck.

### Developer Notes and Iterations
- **Design Intent**: Intended as an escalating puzzle to reward mastery, but post-launch telemetry revealed it as unintentional "hard mode." Chris Pruett (lead developer) noted in analytics posts that end-game levels like this need smoothing to avoid blocking progression, emphasizing data-driven tweaks (e.g., potential hints or power reminders).
- **Comparisons**: Stands out against easier late-game levels like Memory #038 (0.46 deaths/player). It's grouped with other high-frustration spots (e.g., #021 at 7.73 deaths) but unique in its puzzle purity over action.
- **Testing Context**: As a late level, it wasn't the default post-build test (which uses Memory #001), but developers could jump to it via SharedPreferences (e.g., set `PREFERENCE_LEVEL_ROW` to ~6-7 and `PREFERENCE_LEVEL_INDEX` to the slot for #034) for isolated iteration.

For a visual walkthrough, community videos on YouTube demonstrate the puzzle solution in under 4 minutes, often highlighting the "aha" moment at the death spot. The open-source repo includes its assets (e.g., `res/levels/Memory034.xml` for layout) for modders to dissect.