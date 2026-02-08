---
title: "Emulator Rendering Problems"
date: 2025-01-01
draft: false
---

# Emulator Rendering Problems

## No tiles rendered

Q to Gemini:  When running this game in the emulator, the background renders and the sprites render, but the game tiles are not rendered.  Everything works on a physical device.  I have tried various versions of emulators but all don't display the game tiles.  Can you find the problem?

Hello! Dealing with rendering discrepancies between emulators and physical devices
can be quite challenging. Based on the GameRenderer.kt code you've provided and
common issues with older OpenGL ES 1.x code on modern Android emulators,
I believe I've pinpointed the likely cause of your problem.

The Problem: GL_OES_draw_texture Extension

The issue most likely stems from the use of the GL_OES_draw_texture extension.
Your code checks for its availability and uses it if the system reports support:

```
// in onSurfaceCreated()
val extensions = gl.glGetString(GL10.GL_EXTENSIONS)
val supportsDrawTexture = extensions.contains("draw_texture")
//...val params = BaseObject.sSystemRegistry.contextParameters
params!!.supportsDrawTexture = supportsDrawTexture

```
Here's why this is problematic:

1.Emulator Support is Buggy:

While modern Android emulators may report that they support the
GL_OES_draw_texture extension for OpenGL ES 1.0/1.1, the actual
implementation is often incomplete or buggy. This can lead to textures,
like your game tiles, simply not appearing.

2.Physical vs. Emulator:

Physical devices, especially older ones from the era
when this extension was common, have robust hardware-level support for it.
Modern devices running newer Android versions often use a compatibility
layer to run older OpenGL ES code, but emulators struggle to replicate this
perfectly. This explains why it works on a real device but fails in the
emulator.

3.Sprites vs. Tiles:

It's possible that your sprites and background are
rendered using a different mechanism (e.g., standard textured quads
with vertex buffers) while the tilemap rendering is specifically optimized
to use draw_texture when available. This would explain why only the tiles
are missing.The SolutionThe most effective solution is to force the renderer
to not use the draw_texture extension, especially on emulators. This will make
it fall back to a more standard and reliable rendering method (drawing
textured rectangles), which is well-supported everywhere.You can modify
the onSurfaceCreated method to disable this feature. A good approach is
to disable it if the code detects it's running on an emulator.

Recommended Code Change in GameRenderer.ktIn the onSurfaceCreated method,
locate the line where supportsDrawTexture is set and add logic to override it when
running on an emulator.

```
// in onSurfaceCreated()

// ... (after obtaining extensions, version, renderer strings)

val supportsDrawTexture = extensions.contains("draw_texture")
// VBOs are standard in GLES1.1
// No use using VBOs when software renderering, esp. since older versions of the software renderer
// had a crash bug related to freeing VBOs.
val supportsVBOs = !isSoftwareRenderer && (!isOpenGL10 || extensions.contains("vertex_buffer_object"))

val params = BaseObject.sSystemRegistry.contextParameters
params!!.supportsDrawTexture = supportsDrawTexture

// SOLUTION: Add this block to disable draw_texture on emulators
if (Build.FINGERPRINT.contains("generic")
    || Build.FINGERPRINT.startsWith("unknown")
    || Build.MODEL.contains("google_sdk")
    || Build.MODEL.contains("Emulator")
    || Build.MODEL.contains("Android SDK built for x86")
    || Build.MANUFACTURER.contains("Genymotion")
    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    || "google_sdk" == Build.PRODUCT) {

    params.supportsDrawTexture = false // Force disable on emulator
    DebugLog.i("Graphics Support", "Emulator detected. Forcing supportsDrawTexture to false.")
}


params.supportsVBOs = supportsVBOs
hackBrokenDevices()
DebugLog.i("Graphics Support", version + " (" + renderer + "): " + (if (params.supportsDrawTexture) "draw texture," else "") + if (supportsVBOs) "vbos" else "")
mGame.onSurfaceCreated()

```

By adding this check, you ensure that the more compatible rendering path
is used on emulators, which should resolve the issue of the missing tiles.

Gemini refs: https://github.com/gamblore/AndroidPunk
