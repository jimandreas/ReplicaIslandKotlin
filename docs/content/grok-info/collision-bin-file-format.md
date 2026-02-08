---
title: "Collision.bin File Format"
date: 2025-11-01
draft: false
---

## Grok analysis (version 3 or 4 expert mode)

The collision.bin file is a binary data file used in the Replica Island game engine to store collision geometry for levels. It is generated from the text output of the ExtractPoints.js script (which processes Photoshop paths into line segments and normals grouped by tile) and compiled into a compact binary format for efficient loading on Android devices. The file contains no overall header; it is a continuous sequence of tile-based collision data blocks, written in little-endian byte order. The data is typically sorted by increasing tileX, then increasing tileY for each group, allowing the game engine to parse it sequentially.

### File Structure
The file consists of repeating blocks, one for each tile that has collision data. Each block follows this format:

- **tileX** (short, 2 bytes): The X-index of the tile in the level grid (0-based, assuming a tile size like 32x32 pixels).
- **tileY** (short, 2 bytes): The Y-index of the tile in the level grid (0-based).
- **numSegments** (short, 2 bytes): The number of line segments in this tile (typically a small number, e.g., 0-10).
- **Segment data** (repeats numSegments times, 24 bytes per segment):
    - **startX** (float, 4 bytes): The absolute X-coordinate (in pixels) of the line segment's starting point.
    - **startY** (float, 4 bytes): The absolute Y-coordinate (in pixels) of the line segment's starting point.
    - **endX** (float, 4 bytes): The absolute X-coordinate (in pixels) of the line segment's ending point.
    - **endY** (float, 4 bytes): The absolute Y-coordinate (in pixels) of the line segment's ending point.
    - **normX** (float, 4 bytes): The X-component of the unit normal vector (perpendicular to the segment, used for collision response like bouncing).
    - **normY** (float, 4 bytes): The Y-component of the unit normal vector.

The end of the file is reached when there are no more bytes to read. Coordinates are absolute in the level's pixel space (origin at top-left, Y increasing downward in the raw data, though the game engine may flip Y for rendering/physics). The normals point "outward" from the collision surface for proper physics handling.

### Example Binary Layout (Hypothetical Hex Dump for One Tile)
Suppose a tile at (5, 10) with 2 segments:
- Segment 1: start (100.0, 200.0), end (150.0, 250.0), normal (-0.707, 0.707)
- Segment 2: start (150.0, 250.0), end (200.0, 200.0), normal (0.707, 0.707)

The binary (little-endian) would include:
- tileX: 05 00 (short 5)
- tileY: 0A 00 (short 10)
- numSegments: 02 00 (short 2)
- startX1: 00 00 C8 42 (float 100.0)
- startY1: 00 00 48 43 (float 200.0)
- endX1: 00 00 96 43 (float 150.0)
- endY1: 00 00 FA 43 (float 250.0)
- normX1: B5 35 B7 BF (float -0.707)
- normY1: B5 35 B7 3F (float 0.707)
- ... (followed by the second segment's 24 bytes)

### Generation and Loading Notes
- **Generation**: The text output from ExtractPoints.js (e.g., "Tile 5 10: (100,200,150,250,-0.707,0.707) (150,250,200,200,0.707,0.707)") is parsed by a custom script (originally a PHP script mentioned in old forums, though details are scarce now) to write the binary in this format. The script loops through each tile group, converts strings to numbers, and uses binary packing (e.g., pack('s', tileX) in PHP for shorts, pack('f', value) for floats).
- **Loading in Game**: In the Replica Island source code (e.g., LevelBuilder.java), the file is read into a byte array, wrapped in a little-endian ByteBuffer, and parsed in a loop using getShort() for the tile indices and count, followed by getFloat() for each segment's data. The parsed lines are added to a sparse collision map for runtime queries, optimized by tile for performance.

This format is simple and efficient for the era's mobile hardware, avoiding complex headers or compression while allowing quick deserialization into game structures. For exact implementation, refer to open-source forks of Replica Island on GitHub, where the loading code can be inspected in LevelBuilder.java.
