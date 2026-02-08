---
title: "ExtractPoints JavaScript"
date: 2025-01-01
draft: false
---

# ExtractPoints java script

ExtractPoints.js is a JavaScript script designed for Adobe Photoshop, authored by Chris Pruett as part of the Replica Island game's development toolkit around 2009-2010. Its primary purpose is to automate the extraction of collision geometry from level artwork. Specifically, it processes closed paths drawn in Photoshop layers (representing level boundaries and obstacles) and converts them into a textual representation of line segments with associated normals. This data is organized by tile positions (assuming a grid-based level structure, such as 32x32 pixel tiles) and output to a new text layer in the document. The generated text can then be manually copied and compiled into a binary collision file (collision.bin) for use in the Replica Island game engine, facilitating efficient physics and collision detection in the tile-based levels.

The script is described by its creator as rudimentary, inefficient, and slow—taking a long time to execute due to its unoptimized implementation, particularly when handling complex paths or large documents. It was not intended as a polished tool but rather a quick utility to bridge artwork and game data during development. Despite its flaws, it played a key role in the level creation workflow for Replica Island and potentially other games built on the same engine.

### Key Variables and Structures (Based on Typical Implementation)
While the exact code varies slightly across forks, the script relies on Photoshop's ExtendScript API for document manipulation. Common elements include:
- **Document Access**: Uses `app.activeDocument` to reference the current Photoshop file.
- **Path Iteration**: Accesses `pathItems` collection to loop through paths.
- **Point and Segment Data**: Custom objects or arrays to store points (e.g., anchor coordinates like `[x, y]`), segments (pairs of points), and normals (perpendicular vectors).
- **Tile Organization**: Variables for tile size (e.g., a constant like `TILE_SIZE = 32;`) and maps or arrays to group data by tile coordinates (e.g., a 2D array or object keyed by `tileX_tileY`).
- **Output Text**: A string builder to concatenate formatted data.

### Step-by-Step Walkthrough of How the Code Works
The script executes as a single-run process in Photoshop (via File > Scripts > Browse or direct run). Here's a step-by-step breakdown of its logic, inferred from the script's described behavior and standard Photoshop scripting patterns. Note that the actual code uses loops and calculations that can be computationally intensive for dense artwork, contributing to its slowness.

1. **Initialization and Setup**:
    - The script starts by checking for an active document and alerting if none is open.
    - It defines helper functions or classes, such as a `Point` constructor for handling coordinates (e.g., `function Point(x, y) { this.x = x; this.y = y; }`).
    - Constants are set, like tile dimensions, precision for floating-point comparisons, and output format details.
    - A collection (e.g., an object or array) is prepared to store grouped segment data by tile.

2. **Path Collection and Iteration**:
    - It accesses all paths in the document via `var paths = app.activeDocument.pathItems;`.
    - Loops through each path: `for (var i = 0; i < paths.length; i++) { ... }`.
    - For each path, it checks if it's closed (using `subPathItems` properties). Only closed paths are processed, as they represent bounded collision areas.

3. **Extracting Points and Segments**:
    - For each closed subPath, it iterates over `pathPoints`: `for (var j = 0; j < subPath.pathPoints.length; j++) { ... }`.
    - Extracts anchor points (the main coordinates defining the path, ignoring control handles for Bezier curves to simplify to straight lines).
    - Creates line segments between consecutive points (and closes the loop by connecting the last to the first).
    - Example snippet logic:
      ```
      var p1 = subPath.pathPoints[j].anchor;
      var p2 = subPath.pathPoints[(j + 1) % subPath.pathPoints.length].anchor;
      var segment = { start: new Point(p1[0], p1[1]), end: new Point(p2[0], p2[1]) };
      ```

4. **Calculating Normals**:
    - For each segment, computes the direction vector (dx = end.x - start.x, dy = end.y - start.y).
    - Derives the normal vector by rotating 90 degrees (e.g., nx = -dy, ny = dx for right-hand normal, or vice versa for left).
    - Normalizes the vector to unit length: `var length = Math.sqrt(nx*nx + ny*ny); nx /= length; ny /= length;`.
    - Handles edge cases like zero-length segments by skipping or alerting.
    - The normal is crucial for game physics, indicating the "outward" direction for collision responses (e.g., bouncing off walls).

5. **Organizing by Tile**:
    - Determines which tiles the segment spans. Since segments may cross tile boundaries, it might approximate by assigning to the tile containing the midpoint or using a more complex bucketing.
    - Calculates tile indices: `var tileX = Math.floor((start.x + end.x) / 2 / TILE_SIZE); var tileY = Math.floor((start.y + end.y) / 2 / TILE_SIZE);`.
    - Groups segments into a structure like `tiles[tileX + "_" + tileY].push({ startX: start.x, startY: start.y, endX: end.x, endY: end.y, normX: nx, normY: ny });`.
    - This tile-based organization optimizes game performance by loading collision data per visible tile.

6. **Generating Output Text**:
    - After processing all paths, it builds a formatted string.
    - Loops through the grouped tiles, sorting them if needed (e.g., by X then Y).
    - Formats each group as readable text, e.g., "Tile 5 10: (100,200,150,250, -0.707,0.707) (150,250,200,200, 0.707,0.707)\n".
    - The full string is concatenated for all tiles.

7. **Creating the Text Layer**:
    - Creates a new text layer: `var textLayer = app.activeDocument.artLayers.add(); textLayer.kind = LayerKind.TEXT;`.
    - Sets the content: `textLayer.textItem.contents = outputString;`.
    - Positions or styles the layer for visibility (e.g., font size, color).

8. **Completion and Cleanup**:
    - Alerts the user on success or error (e.g., "Processing complete!").
    - No explicit cleanup, as it's a one-off script.

This process allows artists to draw arbitrary shapes for level geometry in Photoshop, run the script to get structured data, and integrate it into the game without manual coordinate entry. The output text is typically pasted into another tool or script for binary compilation.

The source for this script is in the ./tools directory.
