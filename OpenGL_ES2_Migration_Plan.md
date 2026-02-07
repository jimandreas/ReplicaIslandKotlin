# Plan: Migrate OpenGL ES 1.0/1.1 to OpenGL ES 2.0

## Context

The game currently uses **OpenGL ES 1.0/1.1** with the fixed-function pipeline. This API has been deprecated for years -- modern Android devices all support ES 2.0+ (guaranteed since API 8), and ES 1.x lacks programmable shaders. With min SDK 24, ES 3.1 is guaranteed on all target devices, making ES 1.x unnecessary.

The migration replaces the fixed-function pipeline (matrix stack, client-state vertex arrays, `glDrawTexfOES`) with ES 2.0's programmable pipeline (vertex/fragment shaders, VBOs, uniform matrices). The game should look and behave identically after migration.

**Critical constraint:** ES 1.x and ES 2.0 cannot coexist in the same EGL context. The rendering switch must be atomic. The strategy is: prepare everything first (Phases 1-3), then switch atomically (Phase 4), then clean up (Phase 5).

---

## Files That Directly Use OpenGL (8 files)

| File                 | Lines | Role                                               |
|----------------------|-------|----------------------------------------------------|
| `GLSurfaceView.kt`   | ~1600 | Custom EGL lifecycle, threading, context mgmt      |
| `GameRenderer.kt`    | 303   | `Renderer` impl: surface init, viewport, draw loop |
| `OpenGLSystem.kt`    | 75    | Global GL10 holder, texture bind/crop caching      |
| `DrawableBitmap.kt`  | 176   | Sprite rendering via `glDrawTexfOES`               |
| `TextureLibrary.kt`  | 251   | Texture loading with crop rect (GL11Ext)           |
| `Grid.kt`            | 317   | Tiled mesh geometry, VBO/client-side paths         |
| `TiledVertexGrid.kt` | 181   | Tiled background rendering with matrix stack       |
| `GLErrorLogger.kt`   | 1190  | Debug wrapper (unused)                             |

All in `app/src/main/java/com/replica/replicaisland/` (rendering files in `rendering/` subpackage).

---

## Phase 1: Create ES 2.0 Shader Infrastructure (No GL changes)

**Goal:** Add new utility classes. The game continues running on ES 1.x unchanged.

### New files to create (in `rendering/`):

**1. `ShaderProgram.kt`** -- Compile, link, and manage GLES20 shader programs. Provides uniform/attribute locations for `uMVPMatrix`, `uColor`, `uTexture`, `aPosition`, `aTexCoord`.

**2. `MatrixHelper.kt`** -- Wraps `android.opengl.Matrix` to replace the fixed-function matrix stack. Provides `projectionMatrix`, `viewMatrix`, `modelMatrix`, and `computeMVP()` to multiply them.

**3. `SpriteQuad.kt`** -- A shared unit quad (4 vertices, 2 triangles) in a VBO. Replaces `glDrawTexfOES`. UV coordinates are updated per-draw to handle crop rectangles and sprite flipping. The model matrix positions/scales the quad to the correct screen location.

**Shader design (shared by sprites and tiles):**
```glsl
// Vertex
uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTexCoord = aTexCoord;
}

// Fragment
precision mediump float;
uniform sampler2D uTexture;
uniform vec4 uColor;
varying vec2 vTexCoord;
void main() {
    gl_FragColor = texture2D(uTexture, vTexCoord) * uColor;
}
```

The `uColor` uniform replaces `glColor4f(opacity, opacity, opacity, opacity)` for pre-multiplied alpha blending.

### Files modified: None
### Playtest: Game unchanged -- verify it still compiles and runs

---

## Phase 2: Add ES 2.0 Code Paths to Sprite Rendering

**Goal:** Add ES 2.0 draw methods alongside existing ES 1.x methods, guarded by a version flag. The flag stays on ES 1.x during this phase.

### Changes:

**`OpenGLSystem.kt`** -- Add `esVersion` flag (default 1), `spriteShader: ShaderProgram?` field, and `bindTexture2()` that calls `GLES20.glBindTexture`. The `setTextureCrop` becomes a no-op in ES 2.0 mode.

**`DrawableBitmap.kt`** -- Add parallel ES 2.0 methods:
- `beginDrawingES2(viewWidth, viewHeight)`: Activates shader, sets blend mode, sets orthographic projection via `MatrixHelper`, binds texture unit 0
- `drawES2(x, y, scaleX, scaleY)`: Converts crop array to UV coordinates, computes model matrix (translate + scale), sets MVP uniform, sets color uniform, calls `SpriteQuad.draw()`
- `endDrawingES2()`: Disables blending

**Crop-to-UV conversion** (the key math for replacing `glDrawTexfOES`):
```
u0 = crop[0] / textureWidth
u1 = (crop[0] + crop[2]) / textureWidth     // crop[2] negative = h-flip
v0 = (crop[1] + crop[3]) / textureHeight     // crop[3] is stored as -height
v1 = crop[1] / textureHeight
```
When `crop[2]` is negative (horizontal flip), `u0 > u1`, naturally flipping the UVs.

### Files modified: `OpenGLSystem.kt`, `DrawableBitmap.kt`
### Playtest: Game unchanged (ES 2.0 paths exist but are not called)

---

## Phase 3: Add ES 2.0 Code Paths to Tiled Backgrounds, Textures, and Renderer

**Goal:** Complete all dual-mode preparations so the atomic switch can happen.

### Changes:

**`Grid.kt`** -- Add ES 2.0 methods:
- `generateHardwareBuffersES2()`: Uses `GLES20.glGenBuffers/glBufferData` (VBOs are always available in ES 2.0)
- `beginDrawingStripsES2(shader)`: Uses `glVertexAttribPointer/glEnableVertexAttribArray` instead of `glVertexPointer/glEnableClientState`
- `drawStripES2(startIndex, indexCount)`: Uses `GLES20.glDrawElements`
- The fixed-point (GL_FIXED/IntBuffer) path is not needed for ES 2.0 (TiledVertexGrid already uses `useFixedPoint=false`)

**`TiledVertexGrid.kt`** -- Add `drawES2()`: Replaces `glPushMatrix/glTranslatef/glPopMatrix` with `MatrixHelper.setIdentityModel()/translateModel()/computeMVP()` and shader uniform setting.

**`TextureLibrary.kt`** -- Add `loadBitmapES2()`: Uses `GLES20.glGenTextures/glBindTexture/glTexParameteri` + `GLUtils.texImage2D`. Removes `GL_TEXTURE_CROP_RECT_OES` (crop is handled via UVs now).

**`BufferLibrary.kt`** -- Add `generateHardwareBuffersES2()` that calls each Grid's ES 2.0 buffer generation.

**`GameRenderer.kt`** -- Add ES 2.0 branches in:
- `onSurfaceCreated`: Compile shaders, init `SpriteQuad`, set GLES20 clear color/state
- `onSurfaceChanged`: Use `GLES20.glViewport`
- `onDrawFrame`: Call `beginDrawingES2/endDrawingES2` and route drawables through ES 2.0 paths
- `loadTextures/loadBuffers`: Call ES 2.0 variants

### Files modified: `Grid.kt`, `TiledVertexGrid.kt`, `TextureLibrary.kt`, `BufferLibrary.kt`, `GameRenderer.kt`
### Playtest: Game unchanged (flag still ES 1.x)

---

## Phase 4: The Atomic Switch -- Enable ES 2.0

**Goal:** Flip the flag to ES 2.0. This is the critical phase where all rendering changes at once.

### Changes:

**`AndouKun.kt`** -- Add `gLSurfaceView!!.setEGLContextClientVersion(2)` before setting the renderer. (Keep the custom GLSurfaceView for now -- it already supports ES 2.0 context creation via its `DefaultContextFactory`.)

**`OpenGLSystem.kt`** -- Set `esVersion = 2` as default.

**`GameRenderer.kt`** -- Activate all ES 2.0 branches. Set `contextParameters.supportsVBOs = true` (always in ES 2.0), `supportsDrawTexture = false` (not used).

**All rendering files** -- The `esVersion == 2` branches now execute. The ES 1.x branches are dormant.

### Files modified: `AndouKun.kt`, `OpenGLSystem.kt`, `GameRenderer.kt`, plus any routing changes in `DrawableBitmap.kt`, `Grid.kt`, `TiledVertexGrid.kt`, `TextureLibrary.kt`, `BufferLibrary.kt`

### Playtest: CRITICAL -- Full visual regression testing
1. Verify main menu and first level render correctly
2. Compare screenshots before/after for pixel accuracy
3. Verify sprite flipping (horizontal/vertical) works
4. Verify opacity/transparency (fade effects, HUD)
5. Verify tiled backgrounds scroll correctly with no seams
6. Play through 3+ levels end-to-end
7. Test pause/resume and sleep/wake cycles
8. Monitor frame rate for performance regressions

### Risk mitigation:
- Develop on a branch; revert by removing `setEGLContextClientVersion(2)` and setting `esVersion = 1`
- Add temporary `GLES20.glGetError()` calls to catch issues early
- Use `glGetShaderInfoLog/glGetProgramInfoLog` for shader debug

---

## Phase 5: Cleanup -- Remove ES 1.x Code and Custom GLSurfaceView

**Goal:** Remove all legacy code now that ES 2.0 is confirmed working.

### Sub-phase 5a: Remove ES 1.x code paths

- **`DrawableBitmap.kt`**: Remove `beginDrawing(GL10)`, `endDrawing(GL10)`, old `draw()` path. The ES 2.0 methods become the only path.
- **`Grid.kt`**: Remove client-side vertex array path, fixed-point (IntBuffer) path, `GL10`/`GL11` imports. VBOs are the only path.
- **`TiledVertexGrid.kt`**: Remove `GL10` draw path, matrix stack calls.
- **`TextureLibrary.kt`**: Remove `GL10` parameter from `loadBitmap/loadAll/deleteAll`. Remove crop rect setup.
- **`OpenGLSystem.kt`**: Remove `GL10` reference, `esVersion` flag, `setTextureCrop()`. Keep only GLES20-based texture bind caching.
- **`GameRenderer.kt`**: Remove ES 1.x branches, `hackBrokenDevices()`, device-specific workarounds. Remove `GL10` usage (parameter still accepted from interface but ignored).
- **`BufferLibrary.kt`**: Remove `GL10` parameter.
- **`ContextParameters`**: Remove `supportsDrawTexture`, `supportsVBOs`.
- **Delete `GLErrorLogger.kt`** (1190 lines, implements GL10/GL11/GL11Ext -- no longer relevant).

### Playtest: Full regression test

### Sub-phase 5b: Replace custom GLSurfaceView with Android's standard one

- **Delete `GLSurfaceView.kt`** (~1600 lines of custom EGL management)
- **Update `main.xml`**: Change to `android.opengl.GLSurfaceView`
- **Update `AndouKun.kt`**: Change `GLSurfaceView` type references
- **Update `Game.kt`**: Change `surfaceView` type. Replace `loadTextures/flushTextures/loadBuffers/flushBuffers` callback mechanism with `GLSurfaceView.queueEvent { }` approach
- **Update `GameRenderer.kt`**: Implement `android.opengl.GLSurfaceView.Renderer` instead of the custom interface. Move texture/buffer loading to `queueEvent` pattern.

### Playtest: Full regression test -- especially texture loading on level transitions and pause/resume lifecycle

---

## Summary

| Phase | Risk | Rendering Change | Files New/Modified/Deleted |
|-------|------|-----------------|---------------------------|
| 1: Shader infrastructure | Very Low | None | 3 new / 0 mod / 0 del |
| 2: Sprite dual-mode | Low | None | 0 new / 2 mod / 0 del |
| 3: Tiles+textures dual-mode | Low | None | 0 new / 5 mod / 0 del |
| 4: Atomic ES 2.0 switch | **High** | **All rendering** | 0 new / ~8 mod / 0 del |
| 5a: Remove ES 1.x code | Medium | None | 0 new / ~8 mod / 1 del |
| 5b: Replace GLSurfaceView | Medium | None | 0 new / ~4 mod / 1 del |

Total: ~2800 lines of legacy code removed (GLSurfaceView.kt + GLErrorLogger.kt), ~3 new files added (~200-300 lines total).

## Verification

After each phase, run:
```bash
./gradlew build
```
After Phase 4 and Phase 5, deploy to a device and:
1. Play through the tutorial level and at least 2 more levels
2. Verify all sprite animations, flipping, opacity
3. Verify tiled background scrolling
4. Test lifecycle: pause, resume, sleep, wake, rotate
5. Check logcat for GL errors
