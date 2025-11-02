# Replica Island level tool

## Grok question

Supposedly "Chris Pruett" created a game level tool to help create "Replica Island" around 2009 / 2010 for Android.  I have heard that some other games were created using this tool.  Could you please find the source for this tool and any references to the other games created using it.

## Grok answer (Nov 2025, Expert mode ~level 3 or 4)

The level tool referenced in connection with Replica Island appears to be `ExtractPoints.js`, a JavaScript script designed for use in Photoshop. It processes closed paths in level artwork to generate collision data as line segments and normals, which are then compiled into a binary file (`collision.bin`) used by the game engine. This tool was created to aid in level design by extracting physics/collision information from hand-drawn paths in artwork layers, allowing for efficient integration into the tile-based levels of Replica Island. The script is described as rudimentary and not optimized, but it was key to the level creation workflow around 2009-2010.

The source code for this tool (and the full Replica Island game/engine) is available in open-source repositories under the Apache 2.0 license. The original project was hosted on Google Code (now archived at https://code.google.com/archive/p/replicaisland), with active forks/mirrors including:
- https://github.com/masokotanga/ReplicaIsland (includes the `tools/ExtractPoints.js` file).
- https://github.com/jimulabs/replica-island-gradle (a modernized Gradle version for Android Studio).

Levels themselves were primarily designed via artwork (e.g., in Photoshop) and structured using XML files like `leveltree.xml` for progression trees, rather than a full graphical editor. Collision and tile data were derived from the artwork using the script.

Regarding other games created using this tool/engine:
- A 2010 blog post on the Replica Island development diary lists several games built with the Replica Island code base, including Android Jump, Prototype, Greed Pirates, Super Treasure Rocket, and Project G.E.R.T.
- The archived Google Code project page references additional examples of games built with the engine, such as Alpha Company (side-scrolling shooter), Auto Traffic (traffic controller), Beat the Clown (whack-a-mole style), and Bounce Beta (mystery/puzzle game).

These games leveraged the open-source engine for Android development, including its level handling and collision systems derived from tools like ExtractPoints.js. Specific details on how extensively each used the level tool vary, but they demonstrate community adoption of the Replica Island framework shortly after its release.