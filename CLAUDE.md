# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Replica Island is a side-scrolling platformer game for Android, fully converted from Java to Kotlin. The game features the Android robot protagonist navigating levels to find a mysterious power source. This is a complete game with all source code, artwork, dialog, and level layouts.

## Build Commands

```bash
# Build debug APK
./gradlew build

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint check (integrated in build)
./gradlew lint

# Run level viewer desktop utility
./gradlew :levelviewer:run

# Run level viewer tests
./gradlew :levelviewer:test
```

## Build Configuration

- **Gradle**: Kotlin DSL with version catalog (`gradle/libs.versions.toml`)
- **Kotlin**: 2.2.21
- **AGP**: 8.13.1
- **Compile SDK**: 36 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **JVM Target**: Java 11

## Architecture

### Game Loop Hierarchy

```
AndouKun (Activity) - spins up game, handles input, manages level progression
    ↓
Game - abstraction layer, bootstraps game, manages GameThread
    ↓
GameThread - main game loop, manages MainLoop
    ↓
MainLoop - head of game graph, polls children each frame
    ↓
GameObjectManager - activates/deactivates GameObjects based on camera proximity
    ↓
GameObject - game entities composed of GameComponents
    ↓
GameComponent - individual features (collision, animation, rendering, etc.)
```

### Rendering

Rendering occurs on a separate thread. The game thread queues render commands which are handed to the render thread at a synchronization point. `GameRenderer.java` runs via `GLSurfaceView.java` using OpenGL 1.1.

### Package Structure

- **core/** - Foundation classes: `BaseObject`, `GameObject`, `GameObjectFactory`, `GameObjectManager`
- **mechanics/** - Physics and gameplay: `PhysicsComponent`, `CollisionSystem`, `HotSpotSystem`, `TimeSystem`
- **rendering/** - Graphics: `OpenGLSystem`, `RenderSystem`, `CameraSystem`, `SpriteComponent`, `TextureLibrary`
- **entities/** - Game object components (35+ files): player, enemies, NPCs, projectiles, puzzles
- **ui/** - Activities: `MainMenuActivity`, `LevelSelectActivity`, `GameOverActivity`, `HudSystem`
- **levels/** - Level management: `LevelBuilder`, `LevelTree`, `TiledWorld`
- **input/** - Input handling: `InputSystem`, `InputGameInterface`, touch/keyboard support
- **sound/** - Audio: `SoundSystem`
- **utils/** - Utilities: `Vector2`, `FixedSizeArray`, `TObjectPool`, `DifficultyConstants`

### Key Design Patterns

- **Component-Based Architecture**: GameObjects are containers of GameComponents
- **Manager Pattern**: Object hierarchies managed by ObjectManagers
- **Factory Pattern**: `GameObjectFactory` (303KB) instantiates all game objects
- **Object Pooling**: Uses pools (`VectorPool`, `HitPointPool`, etc.) for performance

## Key Files

- **AndouKun.kt** - Core game activity, entry point for gameplay
- **Game.kt** - Game system bootstrapper
- **GameObjectFactory.kt** - Central factory for all game objects (largest file)
- **CollisionSystem.kt** - Collision detection and response system
- **res/raw/collision.bin** - Binary collision data (generated via Photoshop tool)
- **res/xml/leveltree.xml** - Non-linear level progression tree
- **tools/ExtractPoints.js** - Photoshop script to extract collision data from paths

## Level Viewer Subproject (`levelviewer/`)

A standalone Kotlin/JVM + Swing desktop utility that parses the game's binary level files and tile atlases to render overhead level views. Launch with `./gradlew :levelviewer:run`.

### Structure
- **data/** - Binary parsers: `LevelFileParser` (signature 96), `TiledWorldParser` (signature 42), `CollisionParser` (signature 52), `LevelTreeParser` (XML), `BinaryUtils` (little-endian readers)
- **model/** - Data classes: `LevelData`, `TileLayer`, `CollisionData`, `LevelEntry`
- **rendering/** - `TileAtlasCache` (loads atlas PNGs, extracts 32x32 tiles), `LevelRenderer` (composites tile layers), `CollisionRenderer` (draws collision line segments)
- **ui/** - `LevelViewerFrame` (main window), `LevelListPanel` (JTree), `LevelCanvas` (scrollable/zoomable), `ToolBar` (zoom/toggles/export)

### Key Y-axis note
In the binary tile data, y=0 is the **top** of the world. The game flips Y when reading tiles in `TiledVertexGrid.kt:77` (`tilesPerWorldColumn - 1 - tileY`). The level viewer renders without a world Y-flip to match.

## Entry Points

- **Main Menu**: `com.replica.replicaisland.ui.MainMenuActivity` (defined in AndroidManifest.xml)
- **Game Activity**: `com.replica.replicaisland.AndouKun`
- **Level Viewer**: `com.replica.levelviewer.MainKt` (desktop JVM)
