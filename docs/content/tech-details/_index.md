---
title: "Technical Details"
date: 2025-01-01
draft: false
---

# Replica Island Technical presentation information

Click on an image for an enlarged view. These
are screen captured images from the
presentations by Chris Pruett during Google I/O 2009 and 2010.
See the [Links](links/) for more information.

![Screenshot](images/01-GameEngineArchitecture.PNG)
<br>
![Screenshot](images/02-GameGraph.PNG)
<br>
![Screenshot](images/03-GameThreads.PNG)
<br>
![Screenshot](images/04-ChrisPruettViews.PNG)
<br>
![Screenshot](images/05-StepOne-MemoryManagement.PNG)
<br>
![Screenshot](images/06-SpriteMethodTest.PNG)
<br>
![Screenshot](images/07-StepTwo-DontCallFunctions.PNG)
<br>
![Screenshot](images/08-AndroidDrawingMethods.PNG)
<br>
![Screenshot](images/11-01-DrawingInReplicaIsland.PNG)
<br>
![Screenshot](images/11-02-DrawingInReplicaIsland.PNG)
<br>
![Screenshot](images/11-03-DualThreadApproach.PNG)
<br>
![Screenshot](images/11-04-UglyInputSystem.PNG)
<br>
![Screenshot](images/11-05-CurrentInputSystem.PNG)
<br>

## Level Viewer Desktop Utility

*Added by Claude Opus 4.6 (Anthropic)*

A standalone Kotlin/JVM desktop utility was added to the project that parses the game's binary level files and tile atlases to render overhead views of entire levels. This reproduces the kind of level visualizations that Chris Pruett created for the original Replica Island player metrics pages.

The viewer reads the same binary formats used by the game engine at runtime:

- **Level files** (signature 96) — contain multiple layers (background tiles, collision, objects, hot spots), each wrapping a TiledWorld block
- **TiledWorld blocks** (signature 42) — grid of tile indices with width/height headers, read in y-outer x-inner order
- **Collision data** (signature 52) — line segments and normals per collision tile, used for surface detection
- **Level tree XML** — defines the non-linear level progression with string resource references for display names

An important implementation detail: the binary tile data stores y=0 as the **top** of the world. The game's `TiledVertexGrid` flips the Y index when reading tiles for OpenGL rendering (`tilesPerWorldColumn - 1 - tileY`), so the desktop viewer renders tiles directly to screen coordinates without a world Y-flip.

The tile atlases (grass, island, sewage, cave, lab, tutorial) are standard PNG sprite sheets with 32x32 pixel tiles. Each background layer in a level file specifies a theme index that selects the appropriate atlas.

Launch with `./gradlew :levelviewer:run`. The screenshot below shows the first level (Memory #000) with the sewer tileset, the game's title art, and the original author credits rendered from tile data.

![Level Viewer Screenshot](images/LevelViewerScreenshot.png)
<br>
