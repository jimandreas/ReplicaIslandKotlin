/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused", "UNUSED_PARAMETER", "UNCHECKED_CAST", "ConvertTwoComparisonsToRangeCheck", "RemoveEmptySecondaryConstructorBody", "SENSELESS_COMPARISON")

package com.replica.replicaisland

import android.content.res.AssetManager.AssetInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.floor

/**
 * Collision detection system.  Provides a ray-based interface for finding surfaces in the collision
 * world.   This version is based on a collision world of line segments, organized into an array of
 * tiles.  The underlying detection algorithm isn't relevant to calling code, however, so this class
 * may be extended to provide a completely different collision detection scheme.
 *
 * This class also provides a system for runtime-generated collision segments.  These temporary
 * segments are cleared each frame, and consequently must be constantly re-submitted if they are
 * intended to persist.  Temporary segments are useful for dynamic solid objects, such as moving
 * platforms.
 *
 * CollisionSystem.TileVisitor is an interface for traversing individual collision tiles.  Ray casts
 * can be used to run user code over the collision world by passing different TileVisitor
 * implementations to executeRay.  Provided is TileTestVisitor, a visitor that compares the segments
 * of each tile visited with the ray and searches for points of intersection.
 *
 */
class CollisionSystem : BaseObject() {
    private var mWorld: TiledWorld? = null
    private var mCollisionTiles: Array<CollisionTile?>? = null
    private val mSegmentPool: LineSegmentPool
    private var mTileWidth = 0
    private var mTileHeight = 0
    private val mTileSegmentTester: TileTestVisitor
    private var mTemporarySegments: FixedSizeArray<LineSegment?>
    private var mPendingTemporarySegments: FixedSizeArray<LineSegment?>
    private val mWorkspaceBytes // Included here to avoid runtime allocation during file io.
            : ByteArray

    override fun reset() {
        mWorld = null
        mCollisionTiles = null
        val count = mTemporarySegments.count
        for (x in 0 until count) {
            mSegmentPool.release(mTemporarySegments[x]!!)
            mTemporarySegments[x] = null
        }
        mTemporarySegments.clear()
        val pendingCount = mPendingTemporarySegments.count
        for (x in 0 until pendingCount) {
            mSegmentPool.release(mPendingTemporarySegments[x]!!)
            mPendingTemporarySegments[x] = null
        }
        mPendingTemporarySegments.clear()
    }

    /* Sets the current collision world to the supplied tile world. */
    fun initialize(world: TiledWorld?, tileWidth: Int, tileHeight: Int) {
        mWorld = world
        mTileWidth = tileWidth
        mTileHeight = tileHeight
    }

    /**
     * Casts a ray into the collision world.  The ray is bound by the start and end points supplied.
     * The first intersecting segment that is found is returned; in the case where more than one
     * segment is found, the segment closest to the start point is returned.
     *
     * @param startPoint  The starting point for the ray in world units.
     * @param endPoint  The end point for the ray in world units.
     * @param movementDirection  If set, only segments with normals that oppose this direction will
     * be counted as valid intersections.  If null, all intersecting segments will be
     * considered valid.
     * @param hitPoint  The point of intersection between a ray and a surface, if one is found.
     * @param hitNormal  The normal of the intersecting surface if an intersection is found.
     * @param excludeObject If set, dynamic surfaces from this object will be ignored.
     * @return  true if a valid intersecting surface was found, false otherwise.
     */
    // TODO: switch to return data as a HitPoint.
    fun castRay(startPoint: Vector2, endPoint: Vector2, movementDirection: Vector2,
                hitPoint: Vector2, hitNormal: Vector2, excludeObject: GameObject): Boolean {
        var hit = false
        mTileSegmentTester.setup(movementDirection, mTileWidth, mTileHeight)
        if (mCollisionTiles != null &&
                executeRay(startPoint, endPoint, hitPoint, hitNormal, mTileSegmentTester) != -1) {
            hit = true
        }
        if (mTemporarySegments.count > 0) {
            val vectorPool = sSystemRegistry.vectorPool
            val tempHitPoint = vectorPool!!.allocate()
            val tempHitNormal = vectorPool.allocate()
            if (testSegmentAgainstList(mTemporarySegments, startPoint, endPoint, tempHitPoint,
                            tempHitNormal, movementDirection, excludeObject)) {
                if (hit) {
                    // Check to see whether this collision is closer to the one we already found or
                    // not.
                    val firstCollisionDistance = startPoint.distance2(hitPoint)
                    if (firstCollisionDistance > startPoint.distance2(tempHitPoint!!)) {
                        // The temporary surface is closer.
                        hitPoint.set(tempHitPoint)
                        hitNormal.set(tempHitNormal!!)
                    }
                } else {
                    hit = true
                    hitPoint.set(tempHitPoint!!)
                    hitNormal.set(tempHitNormal!!)
                }
            }
            vectorPool.release(tempHitPoint!!)
            vectorPool.release(tempHitNormal!!)
        }
        return hit
    }

    /**
     * testBox - test tool
     */
    fun testBox(left: Float, right: Float, top: Float, bottom: Float,
                movementDirection: Vector2?, hitPoints: FixedSizeArray<HitPoint?>,
                excludeObject: GameObject, testDynamicSurfacesOnly: Boolean): Boolean {
        var foundHit = false

        // Test against the background.
        if (!testDynamicSurfacesOnly) {
            var startX = left
            var endX = right
            var startY = bottom
            var endY = top
            var xIncrement = 1
            var yIncrement = 1
            if (movementDirection != null) {
                if (movementDirection.x < 0.0f) {
                    startX = right
                    endX = left
                    xIncrement = -1
                }
                if (movementDirection.y < 0.0f) {
                    startY = top
                    endY = bottom
                    yIncrement = -1
                }
            }
            val startTileX = Utils.clamp((startX / mTileWidth).toInt(), 0, mWorld!!.fetchWidth() - 1)
            val endTileX = Utils.clamp((endX / mTileWidth).toInt(), 0, mWorld!!.fetchWidth() - 1)
            val startTileY = Utils.clamp((startY / mTileHeight).toInt(), 0, mWorld!!.fetchHeight() - 1)
            val endTileY = Utils.clamp((endY / mTileHeight).toInt(), 0, mWorld!!.fetchHeight() - 1)
            val vectorPool = sSystemRegistry.vectorPool
            val worldTileOffset = vectorPool!!.allocate()
            val tileArray = mWorld!!.fetchTiles()
            val worldHeight = mWorld!!.fetchHeight() - 1
            var y = startTileY
            while (y != endTileY + yIncrement) {
                var x = startTileX
                while (x != endTileX + xIncrement) {
                    val tileIndex = tileArray[x][worldHeight - y]
                    if (tileIndex >= 0 && tileIndex < mCollisionTiles!!.size && mCollisionTiles!![tileIndex] != null) {
                        val xOffset = x * mTileWidth.toFloat()
                        val yOffset = y * mTileHeight.toFloat()
                        val tileSpaceLeft = left - xOffset
                        val tileSpaceRight = right - xOffset
                        val tileSpaceTop = top - yOffset
                        val tileSpaceBottom = bottom - yOffset
                        worldTileOffset!![xOffset] = yOffset
                        val hit = testBoxAgainstList(mCollisionTiles!![tileIndex]!!.segments,
                                tileSpaceLeft, tileSpaceRight, tileSpaceTop, tileSpaceBottom,
                                movementDirection, excludeObject, worldTileOffset, hitPoints)
                        if (hit) {
                            foundHit = true
                        }
                    }
                    x += xIncrement
                }
                y += yIncrement
            }
            vectorPool.release(worldTileOffset!!)
        }
        // temporary segments
        val tempHit = testBoxAgainstList(mTemporarySegments,
                left, right, top, bottom,
                movementDirection, excludeObject, Vector2.ZERO, hitPoints)
        if (tempHit) {
            foundHit = true
        }
        return foundHit
    }

    /* Inserts a temporary surface into the collision world.  It will persist for one frame. */
    fun addTemporarySurface(startPoint: Vector2?, endPoint: Vector2?, normal: Vector2?,
                            ownerObject: GameObject) {
        val newSegment = mSegmentPool.allocate()
        newSegment!![startPoint, endPoint] = normal
        newSegment.setTheOwner(ownerObject)
        mPendingTemporarySegments.add(newSegment)
    }

    override fun update(timeDelta: Float, parent: BaseObject?) {
        // Clear temporary surfaces
        val count = mTemporarySegments.count
        if (mCollisionTiles != null && count > 0) {
            for (x in 0 until count) {
                mSegmentPool.release(mTemporarySegments[x]!!)
                mTemporarySegments[x] = null
            }
            mTemporarySegments.clear()
        }

        // Temporary surfaces must persist for one frame in order to be reliable independent of
        // frame execution order.  So each frame we queue up inserted segments and then swap them
        // into activity when this system is updated.
        val swap = mTemporarySegments
        mTemporarySegments = mPendingTemporarySegments
        mPendingTemporarySegments = swap
    }

    /**
     * Shoots a ray through the collision world.  This function is similar to executeRay() below,
     * except that it is optimized for straight lines (either completely horizontal or completely
     * vertical).
     *
     * @param startPoint  The starting point for the ray, in world space.
     * @param endPoint  The ending point for the ray in world space.
     * @param hitPoint  Set to the intersection coordinates if an intersection is found.
     * @param hitNormal  Set to the normal of the intersecting surface if an intersection is found.
     * @param visitor  Class defining what work to perform at each tile step.
     * @return  The index of the tile that intersected the ray, or -1 if no intersection was found.
     */
    private fun executeStraigtRay(startPoint: Vector2?, endPoint: Vector2?,
                                    startTileX: Int, startTileY: Int, endTileX: Int, endTileY: Int,
                                    deltaX: Int, deltaY: Int,
                                    hitPoint: Vector2, hitNormal: Vector2?, visitor: TileVisitor): Int {
        var currentX = startTileX
        var currentY = startTileY
        var xIncrement = 0
        var yIncrement = 0
        var distance = 0
        if (deltaX != 0) {
            distance = abs(deltaX) + 1
            xIncrement = Utils.sign(deltaX.toFloat())
        } else if (deltaY != 0) {
            distance = abs(deltaY) + 1
            yIncrement = Utils.sign(deltaY.toFloat())
        }
        var hitTile = -1
        val worldHeight = mWorld!!.fetchHeight() - 1
        val tileArray = mWorld!!.fetchTiles()
        for (x in 0 until distance) {
            val tileIndex = tileArray[currentX][worldHeight - currentY]
            if (tileIndex >= 0 && tileIndex < mCollisionTiles!!.size && mCollisionTiles!![tileIndex] != null) {
                if (visitor.visit(mCollisionTiles!![tileIndex], startPoint, endPoint,
                                hitPoint, hitNormal, currentX, currentY)) {
                    hitTile = tileIndex
                    break
                }
            }
            currentX += xIncrement
            currentY += yIncrement
        }
        return hitTile
    }

    /**
     * Shoots a ray through the collision world.  Since the collision world is a 2D array of tiles,
     * this algorithm traces a line in tile space and tests against each non-empty tile it visits.
     * The Bresenham line algorithm is used for the actual traversal, but the action taken at each
     * tile is defined by the visitor class passed to this function.
     *
     * @param startPoint  The starting point for the ray, in world space.
     * @param endPoint  The ending point for the ray in world space.
     * @param hitPoint  Set to the intersection coordinates if an intersection is found.
     * @param hitNormal  Set to the normal of the intersecting surface if an intersection is found.
     * @param visitor  Class defining what work to perform at each tile step.
     * @return  The index of the tile that intersected the ray, or -1 if no intersection was found.
     */
    private fun executeRay(startPoint: Vector2, endPoint: Vector2,
                             hitPoint: Vector2, hitNormal: Vector2?, visitor: TileVisitor): Int {
        val worldHeight = mWorld!!.fetchHeight()
        val worldWidth = mWorld!!.fetchWidth()
        val startTileX = worldToTileColumn(startPoint.x, worldWidth)
        val startTileY = worldToTileRow(startPoint.y, worldHeight)
        val endTileX = worldToTileColumn(endPoint.x, worldWidth)
        val endTileY = worldToTileRow(endPoint.y, worldHeight)
        var currentX = startTileX
        var currentY = startTileY
        val deltaX = endTileX - startTileX
        val deltaY = endTileY - startTileY
        var hitTile = -1
        if (deltaX == 0 || deltaY == 0) {
            hitTile = executeStraigtRay(startPoint, endPoint, startTileX, startTileY,
                    endTileX, endTileY, deltaX, deltaY, hitPoint, hitNormal, visitor)
        } else {
            val xIncrement = if (deltaX != 0) Utils.sign(deltaX.toFloat()) else 0
            val yIncrement = if (deltaY != 0) Utils.sign(deltaY.toFloat()) else 0

            // Note: I'm deviating from the Bresenham algorithm here by adding one to force the end
            // tile to be visited.
            val lateralDelta = if (endTileX > 0 && endTileX < worldWidth - 1) abs(deltaX) + 1 else abs(deltaX)
            val verticalDelta = if (endTileY > 0 && endTileY < worldHeight - 1) abs(deltaY) + 1 else abs(deltaY)
            val deltaX2 = lateralDelta * 2
            val deltaY2 = verticalDelta * 2
            val worldHeightMinusOne = worldHeight - 1
            val tileArray = mWorld!!.fetchTiles()

            // Bresenham line algorithm in tile space.
            if (lateralDelta >= verticalDelta) {
                var error = deltaY2 - lateralDelta
                for (i in 0 until lateralDelta) {
                    val tileIndex = tileArray[currentX][worldHeightMinusOne - currentY]
                    if (tileIndex >= 0 && tileIndex < mCollisionTiles!!.size && mCollisionTiles!![tileIndex] != null) {
                        if (visitor.visit(mCollisionTiles!![tileIndex], startPoint, endPoint,
                                        hitPoint, hitNormal, currentX, currentY)) {
                            hitTile = tileIndex
                            break
                        }
                    }
                    if (error > 0) {
                        currentY += yIncrement
                        error -= deltaX2
                    }
                    error += deltaY2
                    currentX += xIncrement
                }
            } else if (verticalDelta >= lateralDelta) {
                var error = deltaX2 - verticalDelta
                for (i in 0 until verticalDelta) {
                    val tileIndex = tileArray[currentX][worldHeightMinusOne - currentY]
                    if (tileIndex >= 0 && tileIndex < mCollisionTiles!!.size && mCollisionTiles!![tileIndex] != null) {
                        if (visitor.visit(mCollisionTiles!![tileIndex], startPoint, endPoint,
                                        hitPoint, hitNormal, currentX, currentY)) {
                            hitTile = tileIndex
                            break
                        }
                    }
                    if (error > 0) {
                        currentX += xIncrement
                        error -= deltaY2
                    }
                    error += deltaX2
                    currentY += yIncrement
                }
            }
        }
        return hitTile
    }

    private fun worldToTileColumn(x: Float, width: Int): Int {
        return Utils.clamp(floor(x / mTileWidth.toDouble()).toInt(), 0, width - 1)
    }

    private fun worldToTileRow(y: Float, height: Int): Int {
        return Utils.clamp(floor(y / mTileHeight.toDouble()).toInt(), 0, height - 1)
    }

    /*
     * Loads line segments from a binary file and builds the tiled collision database
     * accordingly.
     */
    fun loadCollisionTiles(stream: InputStream): Boolean {
        val success = false
        val byteStream = stream as AssetInputStream
        val signature: Int

        // TODO: this is a hack.  I really should only allocate an array that is the size of the
        // tileset, but at this point I don't actually know that size, so I allocate a buffer that's
        // probably large enough.
        mCollisionTiles = arrayOfNulls(256)
        try {
            signature = byteStream.read()
            if (signature == 52) {
                // This file has the following deserialization format:
                //   read the number of tiles
                //   for each tile
                //     read the tile id
                //     read the number of segments
                //     for each segment
                //       read startx, starty, endx, endy, normalx, normaly
                val tileCount = byteStream.read()
                val size = (1 + 1 + 4 + 4 + 4 + 4 + 4 + 4) * tileCount
                if (byteStream.available() >= size) {
                    for (x in 0 until tileCount) {
                        val tileIndex = byteStream.read()
                        val segmentCount = byteStream.read()
                        if (mCollisionTiles!![tileIndex] == null && segmentCount > 0) {
                            mCollisionTiles!![tileIndex] = CollisionTile(segmentCount)
                        }
                        for (y in 0 until segmentCount) {
                            byteStream.read(mWorkspaceBytes, 0, 4)
                            val startX = Utils.byteArrayToFloat(mWorkspaceBytes)
                            byteStream.read(mWorkspaceBytes, 0, 4)
                            val startY = Utils.byteArrayToFloat(mWorkspaceBytes)
                            byteStream.read(mWorkspaceBytes, 0, 4)
                            val endX = Utils.byteArrayToFloat(mWorkspaceBytes)
                            byteStream.read(mWorkspaceBytes, 0, 4)
                            val endY = Utils.byteArrayToFloat(mWorkspaceBytes)
                            byteStream.read(mWorkspaceBytes, 0, 4)
                            val normalX = Utils.byteArrayToFloat(mWorkspaceBytes)
                            byteStream.read(mWorkspaceBytes, 0, 4)
                            val normalY = Utils.byteArrayToFloat(mWorkspaceBytes)

                            // TODO: it might be wise to pool line segments.  I don't think that
                            // this data will be loaded very often though, so this is ok for now.
                            val newSegment = LineSegment()
                            newSegment.mStartPoint[startX] = startY
                            newSegment.mEndPoint[endX] = endY
                            newSegment.mNormal[normalX] = normalY
                            mCollisionTiles!![tileIndex]!!.addSegment(newSegment)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            //TODO: figure out the best way to deal with this.  Assert?
        }
        return success
    }

    /**
     * An interface for visiting tiles during a ray cast.  Implementations of TileVisitor
     * can be passed to executeRay(); the visit() function will be invoked for each tile touched by
     * the ray until the traversal is completed or visit() returns false.
     */
    abstract inner class TileVisitor : AllocationGuard() {

        // If true is returned, tile scanning continues.  Otherwise it stops.

        abstract fun visit(tile: CollisionTile?, startPoint: Vector2?, endPoint: Vector2?,
                           hitPoint: Vector2, hitNormal: Vector2?, tileX: Int, tileY: Int): Boolean
    }

    /**
     * TileTestVisitor tests the ray against a list of segments assigned to each tile.  If any
     * segment in any tile of the ray's path is found to be intersecting with the ray, traversal
     * stops and intersection information is recorded.
     */
    private inner class TileTestVisitor : TileVisitor() {
        // These vectors are all temporary storage variables allocated as class members to avoid
        // runtime allocation.
        private val mDelta: Vector2 = Vector2()
        private val mTileSpaceStart: Vector2 = Vector2()
        private val mTileSpaceEnd: Vector2 = Vector2()
        private val mTileSpaceOffset: Vector2 = Vector2()
        private var mTileHeight = 0
        private var mTileWidth = 0

        /**
         * Sets the visitor up for a ray test.  Initializes the size of the tiles and the direction
         * of movement by which intersections should be filtered.
         */
        fun setup(movementDirection: Vector2?, tileWidth: Int, tileHeight: Int) {
            if (movementDirection != null) {
                mDelta.set(movementDirection)
                mDelta.normalize()
            } else {
                mDelta.zero()
            }
            mTileWidth = tileWidth
            mTileHeight = tileHeight
        }

        /**
         * Converts the ray into tile space and then compares it to the segments
         * stored in the current tile.
         */
        override fun visit(tile: CollisionTile?, startPoint: Vector2?, endPoint: Vector2?,
                           hitPoint: Vector2, hitNormal: Vector2?, tileX: Int, tileY: Int): Boolean {
            mTileSpaceOffset[tileX * mTileWidth.toFloat()] = tileY * mTileHeight.toFloat()
            mTileSpaceStart.set(startPoint!!)
            mTileSpaceStart.subtract(mTileSpaceOffset)
            mTileSpaceEnd.set(endPoint!!)
            mTileSpaceEnd.subtract(mTileSpaceOffset)
            // find all the hits in the tile and pick the closest to the start point.
            val foundHit = testSegmentAgainstList(
                    tile!!.segments, mTileSpaceStart, mTileSpaceEnd,
                    hitPoint, hitNormal, mDelta, null)
            if (foundHit) {
                // The hitPoint is in tile space, so convert it back to world space.
                hitPoint.add(mTileSpaceOffset)
            }
            return foundHit
        }

    }

    /**
     * A class describing a single surface in the collision world.  Surfaces are stored as a line
     * segment and a normal. The normal must be normalized (its length must be 1.0) and should
     * describe the direction that the segment "pushes against" in a collision.
     */
    class LineSegment : AllocationGuard() {
        val mStartPoint: Vector2 = Vector2()
        val mEndPoint: Vector2 = Vector2()
        var mNormal: Vector2 = Vector2()
        var owner: GameObject? = null

        /* Sets up the line segment.  Values are copied to local storage. */
        operator fun set(start: Vector2?, end: Vector2?, norm: Vector2?) {
            mStartPoint.set(start!!)
            mEndPoint.set(end!!)
            mNormal.set(norm!!)
        }

        fun setTheOwner(ownerObject: GameObject) {
            owner = ownerObject
        }

        /**
         * Checks to see if these lines intersect by projecting one onto the other and then
         * assuring that the collision point is within the range of each segment.
         */
        fun calculateIntersection(otherStart: Vector2, otherEnd: Vector2,
                                  hitPoint: Vector2?): Boolean {
            var intersecting = false

            // Reference: http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
            val x1 = mStartPoint.x
            val x2 = mEndPoint.x
            val x3 = otherStart.x
            val x4 = otherEnd.x
            val y1 = mStartPoint.y
            val y2 = mEndPoint.y
            val y3 = otherStart.y
            val y4 = otherEnd.y
            val denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
            if (denom != 0f) {
                val uA = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom
                val uB = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom
                if (uA >= 0.0f && uA <= 1.0f && uB >= 0.0f && uB <= 1.0f) {
                    val hitX = x1 + uA * (x2 - x1)
                    val hitY = y1 + uA * (y2 - y1)
                    hitPoint!![hitX] = hitY
                    intersecting = true
                }
            }
            return intersecting
        }

        // Based on http://www.garagegames.com/community/resources/view/309
        fun calculateIntersectionBox(left: Float, right: Float, top: Float, bottom: Float,
                                     hitPoint: Vector2?): Boolean {
            val x1 = mStartPoint.x
            val x2 = mEndPoint.x
            val y1 = mStartPoint.y
            val y2 = mEndPoint.y
            var startIntersect: Float
            var endIntersect: Float
            var intersectTimeStart = 0.0f
            var intersectTimeEnd = 1.0f
            if (x1 < x2) {
                if (x1 > right || x2 < left) {
                    return false
                }
                val deltaX = x2 - x1
                startIntersect = if (x1 < left) (left - x1) / deltaX else 0.0f
                endIntersect = if (x2 > right) (right - x1) / deltaX else 1.0f
            } else {
                if (x2 > right || x1 < left) {
                    return false
                }
                val deltaX = x2 - x1
                startIntersect = if (x1 > right) (right - x1) / deltaX else 0.0f
                endIntersect = if (x2 < left) (left - x1) / deltaX else 1.0f
            }
            if (startIntersect > intersectTimeStart) {
                intersectTimeStart = startIntersect
            }
            if (endIntersect < intersectTimeEnd) {
                intersectTimeEnd = endIntersect
            }
            if (intersectTimeEnd < intersectTimeStart) {
                return false
            }

            // y
            if (y1 < y2) {
                if (y1 > top || y2 < bottom) {
                    return false
                }
                val deltaY = y2 - y1
                startIntersect = if (y1 < bottom) (bottom - y1) / deltaY else 0.0f
                endIntersect = if (y2 > top) (top - y1) / deltaY else 1.0f
            } else {
                if (y2 > top || y1 < bottom) {
                    return false
                }
                val deltaY = y2 - y1
                startIntersect = if (y1 > top) (top - y1) / deltaY else 0.0f
                endIntersect = if (y2 < bottom) (bottom - y1) / deltaY else 1.0f
            }
            if (startIntersect > intersectTimeStart) {
                intersectTimeStart = startIntersect
            }
            if (endIntersect < intersectTimeEnd) {
                intersectTimeEnd = endIntersect
            }
            if (intersectTimeEnd < intersectTimeStart) {
                return false
            }
            hitPoint!!.set(mEndPoint)
            hitPoint.subtract(mStartPoint)
            hitPoint.multiply(intersectTimeStart)
            hitPoint.add(mStartPoint)
            return true
        }

    }

    /**
     * A pool of line segments.
     */
    private inner class LineSegmentPool : TObjectPool<LineSegment?> {
        constructor() : super() {}
        constructor(count: Int) : super(count) {}

        override fun reset() {}
        override fun fill() {
            for (x in 0 until fetchSize()) {
                fetchAvailable()!!.add(LineSegment())
            }
        }

        override fun release(entry: Any) {
            (entry as LineSegment).owner = null
            super.release(entry)
        }
    }

    /**
     * A single collision tile.  Manages a list of line segments.
     */
    inner class CollisionTile(maxSegments: Int) : AllocationGuard() {
        var segments: FixedSizeArray<LineSegment?> = FixedSizeArray(maxSegments)
        fun addSegment(segment: LineSegment?): Boolean {
            var success = false
            if (segments.count < segments.getCapacity()) {
                success = true
            }
            segments.add(segment)
            return success
        }

    }

    companion object {
        private const val MAX_TEMPORARY_SEGMENTS = 256

        /*
         * Given a list of segments and a ray, this function performs an intersection search and
         * returns the closest intersecting segment, if any exists.
         */
        private fun testSegmentAgainstList(
                segments: FixedSizeArray<LineSegment?>,
                 startPoint: Vector2, endPoint: Vector2, hitPoint: Vector2?, hitNormal: Vector2?,
                 movementDirection: Vector2, excludeObject: GameObject?): Boolean {
            var foundHit = false
            var closestDistance = -1f
            var hitX = 0f
            var hitY = 0f
            var normalX = 0f
            var normalY = 0f
            val count = segments.count
            val segmentArray: Array<Any?> = segments.array as Array<Any?>
            for (x in 0 until count) {
                val segment = segmentArray[x] as LineSegment?
                // If a movement direction has been passed, filter out invalid surfaces by ignoring
                // those that do not oppose movement.  If no direction has been passed, accept all
                // surfaces.
                val dot = if (movementDirection.length2() > 0.0f) movementDirection.dot(segment!!.mNormal) else -1.0f
                if (dot < 0.0f &&
                        (excludeObject == null || segment!!.owner !== excludeObject) &&
                        segment!!.calculateIntersection(startPoint, endPoint, hitPoint)) {
                    val distance = hitPoint!!.distance2(startPoint)
                    if (!foundHit || closestDistance > distance) {
                        closestDistance = distance
                        foundHit = true
                        normalX = segment.mNormal.x
                        normalY = segment.mNormal.y
                        // Store the components on their own so we don't have to allocate a vector
                        // in this loop.
                        hitX = hitPoint.x
                        hitY = hitPoint.y
                    }
                }
            }
            if (foundHit) {
                hitPoint!![hitX] = hitY
                hitNormal!![normalX] = normalY
            }
            return foundHit
        }

        private fun testBoxAgainstList(segments: FixedSizeArray<LineSegment?>,
                                         left: Float, right: Float, top: Float, bottom: Float,
                                         movementDirection: Vector2?, excludeObject: GameObject, outputOffset: Vector2?,
                                         outputHitPoints: FixedSizeArray<HitPoint?>): Boolean {
            var hitCount = 0
            val maxSegments = outputHitPoints.getCapacity() - outputHitPoints.count
            val count = segments.count
            val segmentArray: Array<Any?> = segments.array as Array<Any?>
            val vectorPool = sSystemRegistry.vectorPool
            val hitPool = sSystemRegistry.hitPointPool
            val tempHitPoint = vectorPool!!.allocate()
            var x = 0
            while (x < count && hitCount < maxSegments) {
                val segment = segmentArray[x] as LineSegment?
                // If a movement direction has been passed, filter out invalid surfaces by ignoring
                // those that do not oppose movement.  If no direction has been passed, accept all
                // surfaces.
                val dot = if (movementDirection!!.length2() > 0.0f) movementDirection.dot(segment!!.mNormal) else -1.0f
                if (dot < 0.0f &&
                        (excludeObject == null || segment!!.owner !== excludeObject) &&
                        segment!!.calculateIntersectionBox(left, right, top, bottom, tempHitPoint)) {
                    val hitPoint = vectorPool.allocate(tempHitPoint)
                    val hitNormal = vectorPool.allocate(segment.mNormal)
                    hitPoint.add(outputOffset!!)
                    val hit = hitPool!!.allocate()
                    hit!!.hitPoint = hitPoint
                    hit.hitNormal = hitNormal
                    outputHitPoints.add(hit)
                    hitCount++
                }
                x++
            }
            vectorPool.release(tempHitPoint!!)
            return hitCount > 0
        }
    }

    init {
        mTileSegmentTester = TileTestVisitor()
        mSegmentPool = LineSegmentPool(MAX_TEMPORARY_SEGMENTS)
        mTemporarySegments = FixedSizeArray(MAX_TEMPORARY_SEGMENTS)
        mPendingTemporarySegments = FixedSizeArray(MAX_TEMPORARY_SEGMENTS)
        mWorkspaceBytes = ByteArray(4)
    }
}