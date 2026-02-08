---
title: "Level Viewer Desktop Utility"
date: 2025-06-01
draft: false
---

# Level Viewer Desktop Utility

*Built by Claude Opus 4.6 (Anthropic)*

A standalone Kotlin/JVM + Swing desktop application that parses Replica Island's binary level files and tile atlases to render overhead views of entire game levels. This reproduces the kind of level visualizations that Chris Pruett created for the original Replica Island player metrics pages, where heat maps of player deaths were overlaid on bird's-eye level renders.

![Level Viewer Screenshot](images/LevelViewerScreenshot.png)

The screenshot above shows Memory #000 — the game's first level — rendered with the sewer tileset. The title art and original author credits are visible, composed entirely from tile data.

## Motivation

The game's 40+ levels are stored as opaque binary files and can only be "seen" by running them on an Android device or emulator with the full game engine. Having a desktop viewer makes it possible to:

- Inspect level layouts without launching the Android app
- Visualize collision geometry overlaid on tile art
- See object spawn positions (player, enemies, collectibles)
- Export high-resolution PNG images of any level
- Understand the binary file formats used by the engine

## How to Run

```bash
./gradlew :levelviewer:run
```

The viewer auto-discovers the project root by walking up from the current directory looking for `app/src/main/res/raw/`. It can also accept an explicit path as a command-line argument.

## Architecture

The subproject lives in `levelviewer/` and is organized into four packages:

### Data Parsers (`data/`)

Each parser handles one of the game's binary formats, all using little-endian byte order:

| Parser | Signature | Purpose |
|--------|-----------|---------|
| `LevelFileParser` | 96 | Top-level level container — reads layer count, background index, then delegates each layer to `TiledWorldParser` |
| `TiledWorldParser` | 42 | Tile grid — reads width/height headers, then a 2D grid of tile indices in y-outer, x-inner order |
| `CollisionParser` | 52 | Collision geometry — reads tile coordinates and line segments with normals from `collision.bin` |
| `LevelTreeParser` | (XML) | Parses `level_tree.xml` and resolves string resource references from `strings.xml` for display names |

`BinaryUtils` provides the low-level little-endian readers (`readLEShort`, `readLEInt`, `readLEFloat`).

### Data Model (`model/`)

Simple Kotlin data classes:

- `LevelData` — holds the background index and list of `TileLayer`s, with convenience properties for `backgroundLayers`, `collisionLayer`, and `objectLayer`
- `TileLayer` — type, theme index, scroll speed, dimensions, and 2D tile array
- `CollisionData` — map of `(tileX, tileY)` to lists of line segments with normals
- `LevelEntry` — display name, filename, and sort index for the level tree

### Rendering (`rendering/`)

- `TileAtlasCache` — loads tile atlas PNGs (grass, island, sewage, cave, lab, tutorial) from the drawable resources and extracts individual 32x32 pixel tiles on demand
- `LevelRenderer` — composites background tile layers onto a `BufferedImage`, optionally overlaying object spawn positions as colored dots
- `CollisionRenderer` — draws collision line segments in green on top of the rendered level image

### UI (`ui/`)

Built with Swing:

- `LevelViewerFrame` — main window with a split pane: level tree on the left, scrollable canvas on the right
- `LevelListPanel` — `JTree` populated from the parsed level tree, click to load
- `LevelCanvas` — scrollable and zoomable panel that displays the rendered `BufferedImage`, supports mouse-wheel zoom
- `ToolBar` — zoom slider (10%–400%), collision toggle, objects toggle, and PNG export button

## Key Y-Axis Detail

In the binary tile data, y=0 is the **top** of the world. The game's `TiledVertexGrid` flips the Y index when reading tiles for OpenGL rendering:

```kotlin
tilesPerWorldColumn - 1 - tileY
```

The desktop viewer renders tiles directly to screen coordinates **without** this flip, since screen Y also increases downward. This means the viewer output matches how the data is stored, not how the game camera sees it (the game camera looks upward from the bottom).

## Tests

The parsers have unit tests that verify correct reading of the binary formats:

```bash
./gradlew :levelviewer:test
```

Tests cover `BinaryUtils`, `CollisionParser`, `LevelFileParser`, and `LevelTreeParser` using synthetic binary data to validate signatures, dimensions, and segment parsing.
