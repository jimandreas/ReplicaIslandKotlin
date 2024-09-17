/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2024 Jim Andreas kotlin conversion and updating
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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.replica.replicaisland

import android.opengl.GLU
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.*

// unused so far 
class GLErrorLogger : GLSurfaceView.GLWrapper {
    override fun wrap(gl: GL?): GL? {
        return ErrorLoggingGL(gl)
    }

    internal class ErrorLoggingGL(private val mGL: GL?) : GL, GL10, GL10Ext, GL11, GL11Ext {
        fun checkError() {
            val error = (mGL as GL10?)!!.glGetError()
            if (error != GL10.GL_NO_ERROR) {
                val method = Thread.currentThread().stackTrace[3].methodName
                DebugLog.d("GL ERROR", "Error: " + error + " (" + GLU.gluErrorString(error) + "): " + method)
            }
            //TODO 2 fix: assert(error == GL10.GL_NO_ERROR)
        }

        override fun glActiveTexture(texture: Int) {
            (mGL as GL10?)!!.glActiveTexture(texture)
            checkError()
        }

        override fun glAlphaFunc(func: Int, ref: Float) {
            (mGL as GL10?)!!.glAlphaFunc(func, ref)
            checkError()
        }

        override fun glAlphaFuncx(func: Int, ref: Int) {
            (mGL as GL10?)!!.glAlphaFuncx(func, ref)
            checkError()
        }

        override fun glBindTexture(target: Int, texture: Int) {
            (mGL as GL10?)!!.glBindTexture(target, texture)
            checkError()
        }

        override fun glBlendFunc(sfactor: Int, dfactor: Int) {
            (mGL as GL10?)!!.glBlendFunc(sfactor, dfactor)
            checkError()
        }

        override fun glClear(mask: Int) {
            (mGL as GL10?)!!.glClear(mask)
            checkError()
        }

        override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
            (mGL as GL10?)!!.glClearColor(red, green, blue, alpha)
            checkError()
        }

        override fun glClearColorx(red: Int, green: Int, blue: Int, alpha: Int) {
            (mGL as GL10?)!!.glClearColorx(red, green, blue, alpha)
            checkError()
        }

        override fun glClearDepthf(depth: Float) {
            (mGL as GL10?)!!.glClearDepthf(depth)
            checkError()
        }

        override fun glClearDepthx(depth: Int) {
            (mGL as GL10?)!!.glClearDepthx(depth)
            checkError()
        }

        override fun glClearStencil(s: Int) {
            (mGL as GL10?)!!.glClearStencil(s)
            checkError()
        }

        override fun glClientActiveTexture(texture: Int) {
            (mGL as GL10?)!!.glClientActiveTexture(texture)
            checkError()
        }

        override fun glColor4f(red: Float, green: Float, blue: Float, alpha: Float) {
            (mGL as GL10?)!!.glColor4f(red, green, blue, alpha)
            checkError()
        }

        override fun glColor4x(red: Int, green: Int, blue: Int, alpha: Int) {
            (mGL as GL10?)!!.glColor4x(red, green, blue, alpha)
            checkError()
        }

        override fun glColorMask(red: Boolean, green: Boolean, blue: Boolean,
                                 alpha: Boolean) {
            (mGL as GL10?)!!.glColorMask(red, green, blue, alpha)
            checkError()
        }

        override fun glColorPointer(size: Int, type: Int, stride: Int,
                                    pointer: Buffer) {
            (mGL as GL10?)!!.glColorPointer(size, type, stride, pointer)
            checkError()
        }

        override fun glCompressedTexImage2D(target: Int, level: Int,
                                            internalformat: Int, width: Int, height: Int, border: Int,
                                            imageSize: Int, data: Buffer) {
            (mGL as GL10?)!!.glCompressedTexImage2D(target, level,
                    internalformat, width, height, border, imageSize, data)
            checkError()
        }

        override fun glCompressedTexSubImage2D(target: Int, level: Int,
                                               xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int,
                                               imageSize: Int, data: Buffer) {
            (mGL as GL10?)!!.glCompressedTexSubImage2D(target, level,
                    xoffset, yoffset, width, height, format,
                    imageSize, data)
            checkError()
        }

        override fun glCopyTexImage2D(target: Int, level: Int, internalformat: Int,
                                      x: Int, y: Int, width: Int, height: Int, border: Int) {
            (mGL as GL10?)!!.glCopyTexImage2D(target, level, internalformat,
                    x, y, width, height, border)
            checkError()
        }

        override fun glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int,
                                         yoffset: Int, x: Int, y: Int, width: Int, height: Int) {
            (mGL as GL10?)!!.glCopyTexSubImage2D(target, level, xoffset,
                    yoffset, x, y, width, height)
            checkError()
        }

        override fun glCullFace(mode: Int) {
            (mGL as GL10?)!!.glCullFace(mode)
            checkError()
        }

        override fun glDeleteTextures(n: Int, textures: IntBuffer) {
            (mGL as GL10?)!!.glDeleteTextures(n, textures)
            checkError()
        }

        override fun glDeleteTextures(n: Int, textures: IntArray, offset: Int) {
            (mGL as GL10?)!!.glDeleteTextures(n, textures, offset)
            checkError()
        }

        override fun glDepthFunc(func: Int) {
            (mGL as GL10?)!!.glDepthFunc(func)
            checkError()
        }

        override fun glDepthMask(flag: Boolean) {
            (mGL as GL10?)!!.glDepthMask(flag)
            checkError()
        }

        override fun glDepthRangef(zNear: Float, zFar: Float) {
            (mGL as GL10?)!!.glDepthRangef(zNear, zFar)
            checkError()
        }

        override fun glDepthRangex(zNear: Int, zFar: Int) {
            (mGL as GL10?)!!.glDepthRangex(zNear, zFar)
            checkError()
        }

        override fun glDisable(cap: Int) {
            (mGL as GL10?)!!.glDisable(cap)
            checkError()
        }

        override fun glDisableClientState(array: Int) {
            (mGL as GL10?)!!.glDisableClientState(array)
            checkError()
        }

        override fun glDrawArrays(mode: Int, first: Int, count: Int) {
            (mGL as GL10?)!!.glDrawArrays(mode, first, count)
            checkError()
        }

        override fun glDrawElements(mode: Int, count: Int, type: Int, indices: Buffer) {
            (mGL as GL10?)!!.glDrawElements(mode, count, type, indices)
            checkError()
        }

        override fun glEnable(cap: Int) {
            (mGL as GL10?)!!.glEnable(cap)
            checkError()
        }

        override fun glEnableClientState(array: Int) {
            (mGL as GL10?)!!.glEnableClientState(array)
            checkError()
        }

        override fun glFinish() {
            (mGL as GL10?)!!.glFinish()
            checkError()
        }

        override fun glFlush() {
            (mGL as GL10?)!!.glFlush()
            checkError()
        }

        override fun glFogf(pname: Int, param: Float) {
            (mGL as GL10?)!!.glFogf(pname, param)
            checkError()
        }

        override fun glFogfv(pname: Int, params: FloatBuffer) {
            (mGL as GL10?)!!.glFogfv(pname, params)
            checkError()
        }

        override fun glFogfv(pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glFogfv(pname, params, offset)
            checkError()
        }

        override fun glFogx(pname: Int, param: Int) {
            (mGL as GL10?)!!.glFogx(pname, param)
            checkError()
        }

        override fun glFogxv(pname: Int, params: IntBuffer) {
            (mGL as GL10?)!!.glFogxv(pname, params)
            checkError()
        }

        override fun glFogxv(pname: Int, params: IntArray, offset: Int) {
            (mGL as GL10?)!!.glFogxv(pname, params, offset)
            checkError()
        }

        override fun glFrontFace(mode: Int) {
            (mGL as GL10?)!!.glFrontFace(mode)
            checkError()
        }

        override fun glFrustumf(left: Float, right: Float, bottom: Float,
                                top: Float, zNear: Float, zFar: Float) {
            (mGL as GL10?)!!.glFrustumf(left, right, bottom,
                    top, zNear, zFar)
            checkError()
        }

        override fun glFrustumx(left: Int, right: Int, bottom: Int, top: Int,
                                zNear: Int, zFar: Int) {
            (mGL as GL10?)!!.glFrustumx(left, right, bottom, top,
                    zNear, zFar)
            checkError()
        }

        override fun glGenTextures(n: Int, textures: IntBuffer) {
            (mGL as GL10?)!!.glGenTextures(n, textures)
            checkError()
        }

        override fun glGenTextures(n: Int, textures: IntArray, offset: Int) {
            (mGL as GL10?)!!.glGenTextures(n, textures, offset)
            checkError()
        }

        override fun glGetError(): Int {
            return (mGL as GL10?)!!.glGetError()
        }

        override fun glGetIntegerv(pname: Int, params: IntBuffer) {
            (mGL as GL10?)!!.glGetIntegerv(pname, params)
            checkError()
        }

        override fun glGetIntegerv(pname: Int, params: IntArray, offset: Int) {
            (mGL as GL10?)!!.glGetIntegerv(pname, params, offset)
            checkError()
        }

        override fun glGetString(name: Int): String {
            val result = (mGL as GL10?)!!.glGetString(name)
            checkError()
            return result
        }

        override fun glHint(target: Int, mode: Int) {
            (mGL as GL10?)!!.glHint(target, mode)
            checkError()
        }

        override fun glLightModelf(pname: Int, param: Float) {
            (mGL as GL10?)!!.glLightModelf(pname, param)
            checkError()
        }

        override fun glLightModelfv(pname: Int, params: FloatBuffer) {
            (mGL as GL10?)!!.glLightModelfv(pname, params)
            checkError()
        }

        override fun glLightModelfv(pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glLightModelfv(pname, params, offset)
            checkError()
        }

        override fun glLightModelx(pname: Int, param: Int) {
            (mGL as GL10?)!!.glLightModelx(pname, param)
            checkError()
        }

        override fun glLightModelxv(pname: Int, params: IntBuffer) {
            (mGL as GL10?)!!.glLightModelxv(pname, params)
            checkError()
        }

        override fun glLightModelxv(pname: Int, params: IntArray, offset: Int) {
            (mGL as GL10?)!!.glLightModelxv(pname, params, offset)
            checkError()
        }

        override fun glLightf(light: Int, pname: Int, param: Float) {
            (mGL as GL10?)!!.glLightf(light, pname, param)
            checkError()
        }

        override fun glLightfv(light: Int, pname: Int, params: FloatBuffer) {
            (mGL as GL10?)!!.glLightfv(light, pname, params)
            checkError()
        }

        override fun glLightfv(light: Int, pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glLightfv(light, pname, params, offset)
            checkError()
        }

        override fun glLightx(light: Int, pname: Int, param: Int) {
            (mGL as GL10?)!!.glLightx(light, pname, param)
            checkError()
        }

        override fun glLightxv(light: Int, pname: Int, params: IntBuffer) {
            (mGL as GL10?)!!.glLightxv(light, pname, params)
            checkError()
        }

        override fun glLightxv(light: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL10?)!!.glLightxv(light, pname, params, offset)
            checkError()
        }

        override fun glLineWidth(width: Float) {
            (mGL as GL10?)!!.glLineWidth(width)
            checkError()
        }

        override fun glLineWidthx(width: Int) {
            (mGL as GL10?)!!.glLineWidthx(width)
            checkError()
        }

        override fun glLoadIdentity() {
            (mGL as GL10?)!!.glLoadIdentity()
            checkError()
        }

        override fun glLoadMatrixf(m: FloatBuffer) {
            (mGL as GL10?)!!.glLoadMatrixf(m)
            checkError()
        }

        override fun glLoadMatrixf(m: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glLoadMatrixf(m, offset)
            checkError()
        }

        override fun glLoadMatrixx(m: IntBuffer) {
            (mGL as GL10?)!!.glLoadMatrixx(m)
            checkError()
        }

        override fun glLoadMatrixx(m: IntArray, offset: Int) {
            (mGL as GL10?)!!.glLoadMatrixx(m, offset)
            checkError()
        }

        override fun glLogicOp(opcode: Int) {
            (mGL as GL10?)!!.glLogicOp(opcode)
            checkError()
        }

        override fun glMaterialf(face: Int, pname: Int, param: Float) {
            (mGL as GL10?)!!.glMaterialf(face, pname, param)
            checkError()
        }

        override fun glMaterialfv(face: Int, pname: Int, params: FloatBuffer) {
            (mGL as GL10?)!!.glMaterialfv(face, pname, params)
            checkError()
        }

        override fun glMaterialfv(face: Int, pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glMaterialfv(face, pname, params, offset)
            checkError()
        }

        override fun glMaterialx(face: Int, pname: Int, param: Int) {
            (mGL as GL10?)!!.glMaterialx(face, pname, param)
            checkError()
        }

        override fun glMaterialxv(face: Int, pname: Int, params: IntBuffer) {
            (mGL as GL10?)!!.glMaterialxv(face, pname, params)
            checkError()
        }

        override fun glMaterialxv(face: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL10?)!!.glMaterialxv(face, pname, params, offset)
            checkError()
        }

        override fun glMatrixMode(mode: Int) {
            (mGL as GL10?)!!.glMatrixMode(mode)
            checkError()
        }

        override fun glMultMatrixf(m: FloatBuffer) {
            (mGL as GL10?)!!.glMultMatrixf(m)
            checkError()
        }

        override fun glMultMatrixf(m: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glMultMatrixf(m, offset)
            checkError()
        }

        override fun glMultMatrixx(m: IntBuffer) {
            (mGL as GL10?)!!.glMultMatrixx(m)
            checkError()
        }

        override fun glMultMatrixx(m: IntArray, offset: Int) {
            (mGL as GL10?)!!.glMultMatrixx(m, offset)
            checkError()
        }

        override fun glMultiTexCoord4f(target: Int, s: Float, t: Float, r: Float,
                                       q: Float) {
            (mGL as GL10?)!!.glMultiTexCoord4f(target, s, t, r, q)
            checkError()
        }

        override fun glMultiTexCoord4x(target: Int, s: Int, t: Int, r: Int, q: Int) {
            (mGL as GL10?)!!.glMultiTexCoord4x(target, s, t, r, q)
            checkError()
        }

        override fun glNormal3f(nx: Float, ny: Float, nz: Float) {
            (mGL as GL10?)!!.glNormal3f(nx, ny, nz)
            checkError()
        }

        override fun glNormal3x(nx: Int, ny: Int, nz: Int) {
            (mGL as GL10?)!!.glNormal3x(nx, ny, nz)
            checkError()
        }

        override fun glNormalPointer(type: Int, stride: Int, pointer: Buffer) {
            (mGL as GL10?)!!.glNormalPointer(type, stride, pointer)
            checkError()
        }

        override fun glOrthof(left: Float, right: Float, bottom: Float, top: Float,
                              zNear: Float, zFar: Float) {
            (mGL as GL10?)!!.glOrthof(left, right, bottom, top,
                    zNear, zFar)
            checkError()
        }

        override fun glOrthox(left: Int, right: Int, bottom: Int, top: Int,
                              zNear: Int, zFar: Int) {
            (mGL as GL10?)!!.glOrthox(left, right, bottom, top,
                    zNear, zFar)
            checkError()
        }

        override fun glPixelStorei(pname: Int, param: Int) {
            (mGL as GL10?)!!.glPixelStorei(pname, param)
            checkError()
        }

        override fun glPointSize(size: Float) {
            (mGL as GL10?)!!.glPointSize(size)
            checkError()
        }

        override fun glPointSizex(size: Int) {
            (mGL as GL10?)!!.glPointSizex(size)
            checkError()
        }

        override fun glPolygonOffset(factor: Float, units: Float) {
            (mGL as GL10?)!!.glPolygonOffset(factor, units)
            checkError()
        }

        override fun glPolygonOffsetx(factor: Int, units: Int) {
            (mGL as GL10?)!!.glPolygonOffsetx(factor, units)
            checkError()
        }

        override fun glPopMatrix() {
            (mGL as GL10?)!!.glPopMatrix()
            checkError()
        }

        override fun glPushMatrix() {
            (mGL as GL10?)!!.glPushMatrix()
            checkError()
        }

        override fun glReadPixels(x: Int, y: Int, width: Int, height: Int,
                                  format: Int, type: Int, pixels: Buffer) {
            (mGL as GL10?)!!.glReadPixels(x, y, width, height,
                    format, type, pixels)
            checkError()
        }

        override fun glRotatef(angle: Float, x: Float, y: Float, z: Float) {
            (mGL as GL10?)!!.glRotatef(angle, x, y, z)
            checkError()
        }

        override fun glRotatex(angle: Int, x: Int, y: Int, z: Int) {
            (mGL as GL10?)!!.glRotatex(angle, x, y, z)
            checkError()
        }

        override fun glSampleCoverage(value: Float, invert: Boolean) {
            (mGL as GL10?)!!.glSampleCoverage(value, invert)
            checkError()
        }

        override fun glSampleCoveragex(value: Int, invert: Boolean) {
            (mGL as GL10?)!!.glSampleCoveragex(value, invert)
            checkError()
        }

        override fun glScalef(x: Float, y: Float, z: Float) {
            (mGL as GL10?)!!.glScalef(x, y, z)
            checkError()
        }

        override fun glScalex(x: Int, y: Int, z: Int) {
            (mGL as GL10?)!!.glScalex(x, y, z)
            checkError()
        }

        override fun glScissor(x: Int, y: Int, width: Int, height: Int) {
            (mGL as GL10?)!!.glScissor(x, y, width, height)
            checkError()
        }

        override fun glShadeModel(mode: Int) {
            (mGL as GL10?)!!.glShadeModel(mode)
            checkError()
        }

        override fun glStencilFunc(func: Int, ref: Int, mask: Int) {
            (mGL as GL10?)!!.glStencilFunc(func, ref, mask)
            checkError()
        }

        override fun glStencilMask(mask: Int) {
            (mGL as GL10?)!!.glStencilMask(mask)
            checkError()
        }

        override fun glStencilOp(fail: Int, zfail: Int, zpass: Int) {
            (mGL as GL10?)!!.glStencilOp(fail, zfail, zpass)
            checkError()
        }

        override fun glTexCoordPointer(size: Int, type: Int, stride: Int,
                                       pointer: Buffer) {
            (mGL as GL10?)!!.glTexCoordPointer(size, type, stride,
                    pointer)
            checkError()
        }

        override fun glTexEnvf(target: Int, pname: Int, param: Float) {
            (mGL as GL10?)!!.glTexEnvf(target, pname, param)
            checkError()
        }

        override fun glTexEnvfv(target: Int, pname: Int, params: FloatBuffer) {
            (mGL as GL10?)!!.glTexEnvfv(target, pname, params)
            checkError()
        }

        override fun glTexEnvfv(target: Int, pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL10?)!!.glTexEnvfv(target, pname, params, offset)
            checkError()
        }

        override fun glTexEnvx(target: Int, pname: Int, param: Int) {
            (mGL as GL10?)!!.glTexEnvx(target, pname, param)
            checkError()
        }

        override fun glTexEnvxv(target: Int, pname: Int, params: IntBuffer) {
            (mGL as GL10?)!!.glTexEnvxv(target, pname, params)
            checkError()
        }

        override fun glTexEnvxv(target: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL10?)!!.glTexEnvxv(target, pname, params, offset)
            checkError()
        }

        override fun glTexImage2D(target: Int, level: Int, internalformat: Int,
                                  width: Int, height: Int, border: Int, format: Int, type: Int,
                                  pixels: Buffer) {
            (mGL as GL10?)!!.glTexImage2D(target, level, internalformat,
                    width, height, border, format, type,
                    pixels)
            checkError()
        }

        override fun glTexParameterf(target: Int, pname: Int, param: Float) {
            (mGL as GL10?)!!.glTexParameterf(target, pname, param)
            checkError()
        }

        override fun glTexParameterx(target: Int, pname: Int, param: Int) {
            (mGL as GL10?)!!.glTexParameterx(target, pname, param)
            checkError()
        }

        override fun glTexSubImage2D(target: Int, level: Int, xoffset: Int,
                                     yoffset: Int, width: Int, height: Int, format: Int, type: Int,
                                     pixels: Buffer) {
            (mGL as GL10?)!!.glTexSubImage2D(target, level, xoffset,
                    yoffset, width, height, format, type,
                    pixels)
            checkError()
        }

        override fun glTranslatef(x: Float, y: Float, z: Float) {
            (mGL as GL10?)!!.glTranslatef(x, y, z)
            checkError()
        }

        override fun glTranslatex(x: Int, y: Int, z: Int) {
            (mGL as GL10?)!!.glTranslatex(x, y, z)
            checkError()
        }

        override fun glVertexPointer(size: Int, type: Int, stride: Int,
                                     pointer: Buffer) {
            (mGL as GL10?)!!.glVertexPointer(size, type, stride,
                    pointer)
            checkError()
        }

        override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
            (mGL as GL10?)!!.glViewport(x, y, width, height)
            checkError()
        }

        override fun glBindBuffer(arg0: Int, arg1: Int) {
            (mGL as GL11?)!!.glBindBuffer(arg0, arg1)
            checkError()
        }

        override fun glBufferData(arg0: Int, arg1: Int, arg2: Buffer, arg3: Int) {
            (mGL as GL11?)!!.glBufferData(arg0, arg1, arg2, arg3)
            checkError()
        }

        override fun glBufferSubData(arg0: Int, arg1: Int, arg2: Int, arg3: Buffer) {
            (mGL as GL11?)!!.glBufferSubData(arg0, arg1, arg2, arg3)
            checkError()
        }

        override fun glClipPlanef(arg0: Int, arg1: FloatBuffer) {
            (mGL as GL11?)!!.glClipPlanef(arg0, arg1)
            checkError()
        }

        override fun glClipPlanef(arg0: Int, arg1: FloatArray, arg2: Int) {
            (mGL as GL11?)!!.glClipPlanef(arg0, arg1, arg2)
            checkError()
        }

        override fun glClipPlanex(arg0: Int, arg1: IntBuffer) {
            (mGL as GL11?)!!.glClipPlanex(arg0, arg1)
            checkError()
        }

        override fun glClipPlanex(arg0: Int, arg1: IntArray, arg2: Int) {
            (mGL as GL11?)!!.glClipPlanex(arg0, arg1, arg2)
            checkError()
        }

        override fun glColor4ub(arg0: Byte, arg1: Byte, arg2: Byte, arg3: Byte) {
            (mGL as GL11?)!!.glColor4ub(arg0, arg1, arg2, arg3)
            checkError()
        }

        override fun glColorPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Int) {
            (mGL as GL11?)!!.glColorPointer(arg0, arg1, arg2, arg3)
            checkError()
        }

        override fun glDeleteBuffers(n: Int, buffers: IntBuffer) {
            (mGL as GL11?)!!.glDeleteBuffers(n, buffers)
            checkError()
        }

        override fun glDeleteBuffers(n: Int, buffers: IntArray, offset: Int) {
            (mGL as GL11?)!!.glDeleteBuffers(n, buffers, offset)
            checkError()
        }

        override fun glDrawElements(mode: Int, count: Int, type: Int, offset: Int) {
            (mGL as GL11?)!!.glDrawElements(mode, count, type, offset)
            checkError()
        }

        override fun glGenBuffers(n: Int, buffers: IntBuffer) {
            (mGL as GL11?)!!.glGenBuffers(n, buffers)
            checkError()
        }

        override fun glGenBuffers(n: Int, buffers: IntArray, offset: Int) {
            (mGL as GL11?)!!.glGenBuffers(n, buffers, offset)
            checkError()
        }

        override fun glGetBooleanv(pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetBooleanv(pname, params)
            checkError()
        }

        override fun glGetBooleanv(pname: Int, params: BooleanArray, offset: Int) {
            (mGL as GL11?)!!.glGetBooleanv(pname, params, offset)
            checkError()
        }

        override fun glGetBufferParameteriv(target: Int, pname: Int,
                                            params: IntBuffer) {
            (mGL as GL11?)!!.glGetBufferParameteriv(target, pname,
                    params)
            checkError()
        }

        override fun glGetBufferParameteriv(target: Int, pname: Int, params: IntArray,
                                            offset: Int) {
            (mGL as GL11?)!!.glGetBufferParameteriv(target, pname, params,
                    offset)
            checkError()
        }

        override fun glGetClipPlanef(pname: Int, eqn: FloatBuffer) {
            (mGL as GL11?)!!.glGetClipPlanef(pname, eqn)
            checkError()
        }

        override fun glGetClipPlanef(pname: Int, eqn: FloatArray, offset: Int) {
            (mGL as GL11?)!!.glGetClipPlanef(pname, eqn, offset)
            checkError()
        }

        override fun glGetClipPlanex(pname: Int, eqn: IntBuffer) {
            (mGL as GL11?)!!.glGetClipPlanex(pname, eqn)
            checkError()
        }

        override fun glGetClipPlanex(pname: Int, eqn: IntArray, offset: Int) {
            (mGL as GL11?)!!.glGetClipPlanex(pname, eqn, offset)
            checkError()
        }

        override fun glGetFixedv(pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetFixedv(pname, params)
            checkError()
        }

        override fun glGetFixedv(pname: Int, params: IntArray, offset: Int) {
            (mGL as GL11?)!!.glGetFixedv(pname, params, offset)
            checkError()
        }

        override fun glGetFloatv(pname: Int, params: FloatBuffer) {
            (mGL as GL11?)!!.glGetFloatv(pname, params)
            checkError()
        }

        override fun glGetFloatv(pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL11?)!!.glGetFloatv(pname, params, offset)
            checkError()
        }

        override fun glGetLightfv(light: Int, pname: Int, params: FloatBuffer) {
            (mGL as GL11?)!!.glGetLightfv(light, pname, params)
            checkError()
        }

        override fun glGetLightfv(light: Int, pname: Int, params: FloatArray,
                                  offset: Int) {
            (mGL as GL11?)!!.glGetLightfv(light, pname, params,
                    offset)
            checkError()
        }

        override fun glGetLightxv(light: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetLightxv(light, pname, params)
            checkError()
        }

        override fun glGetLightxv(light: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL11?)!!.glGetLightxv(light, pname, params, offset)
            checkError()
        }

        override fun glGetMaterialfv(face: Int, pname: Int, params: FloatBuffer) {
            (mGL as GL11?)!!.glGetMaterialfv(face, pname, params)
            checkError()
        }

        override fun glGetMaterialfv(face: Int, pname: Int, params: FloatArray,
                                     offset: Int) {
            (mGL as GL11?)!!.glGetMaterialfv(face, pname, params,
                    offset)
            checkError()
        }

        override fun glGetMaterialxv(face: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetMaterialxv(face, pname, params)
            checkError()
        }

        override fun glGetMaterialxv(face: Int, pname: Int, params: IntArray,
                                     offset: Int) {
            (mGL as GL11?)!!.glGetMaterialxv(face, pname, params,
                    offset)
            checkError()
        }

        override fun glGetPointerv(pname: Int, params: Array<Buffer>) {
            (mGL as GL11?)!!.glGetPointerv(pname, params)
            checkError()
        }

        override fun glGetTexEnviv(env: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetTexEnviv(env, pname, params)
            checkError()
        }

        override fun glGetTexEnviv(env: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL11?)!!.glGetTexEnviv(env, pname, params, offset)
            checkError()
        }

        override fun glGetTexEnvxv(env: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetTexEnvxv(env, pname, params)
            checkError()
        }

        override fun glGetTexEnvxv(env: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL11?)!!.glGetTexEnvxv(env, pname, params, offset)
            checkError()
        }

        override fun glGetTexParameterfv(target: Int, pname: Int,
                                         params: FloatBuffer) {
            (mGL as GL11?)!!.glGetTexParameterfv(target, pname,
                    params)
            checkError()
        }

        override fun glGetTexParameterfv(target: Int, pname: Int, params: FloatArray,
                                         offset: Int) {
            (mGL as GL11?)!!.glGetTexParameterfv(target, pname, params,
                    offset)
            checkError()
        }

        override fun glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetTexParameteriv(target, pname, params)
            checkError()
        }

        override fun glGetTexParameteriv(target: Int, pname: Int, params: IntArray,
                                         offset: Int) {
            (mGL as GL11?)!!.glGetTexParameteriv(target, pname, params,
                    offset)
            checkError()
        }

        override fun glGetTexParameterxv(target: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glGetTexParameterxv(target, pname, params)
            checkError()
        }

        override fun glGetTexParameterxv(target: Int, pname: Int, params: IntArray,
                                         offset: Int) {
            (mGL as GL11?)!!.glGetTexParameterxv(target, pname, params,
                    offset)
            checkError()
        }

        override fun glIsBuffer(buffer: Int): Boolean {
            val result = (mGL as GL11?)!!.glIsBuffer(buffer)
            checkError()
            return result
        }

        override fun glIsEnabled(cap: Int): Boolean {
            val result = (mGL as GL11?)!!.glIsEnabled(cap)
            checkError()
            return result
        }

        override fun glIsTexture(texture: Int): Boolean {
            val result = (mGL as GL11?)!!.glIsTexture(texture)
            checkError()
            return result
        }

        override fun glNormalPointer(type: Int, stride: Int, offset: Int) {
            (mGL as GL11?)!!.glNormalPointer(type, stride, offset)
            checkError()
        }

        override fun glPointParameterf(pname: Int, param: Float) {
            (mGL as GL11?)!!.glPointParameterf(pname, param)
            checkError()
        }

        override fun glPointParameterfv(pname: Int, params: FloatBuffer) {
            (mGL as GL11?)!!.glPointParameterfv(pname, params)
            checkError()
        }

        override fun glPointParameterfv(pname: Int, params: FloatArray, offset: Int) {
            (mGL as GL11?)!!.glPointParameterfv(pname, params, offset)
            checkError()
        }

        override fun glPointParameterx(pname: Int, param: Int) {
            (mGL as GL11?)!!.glPointParameterx(pname, param)
            checkError()
        }

        override fun glPointParameterxv(pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glPointParameterxv(pname, params)
            checkError()
        }

        override fun glPointParameterxv(pname: Int, params: IntArray, offset: Int) {
            (mGL as GL11?)!!.glPointParameterxv(pname, params, offset)
            checkError()
        }

        override fun glPointSizePointerOES(type: Int, stride: Int, pointer: Buffer) {
            (mGL as GL11?)!!.glPointSizePointerOES(type, stride, pointer)
            checkError()
        }

        override fun glTexCoordPointer(size: Int, type: Int, stride: Int, offset: Int) {
            (mGL as GL11?)!!.glTexCoordPointer(size, type, stride, offset)
            checkError()
        }

        override fun glTexEnvi(target: Int, pname: Int, param: Int) {
            (mGL as GL11?)!!.glTexEnvi(target, pname, param)
            checkError()
        }

        override fun glTexEnviv(target: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glTexEnviv(target, pname, params)
            checkError()
        }

        override fun glTexEnviv(target: Int, pname: Int, params: IntArray, offset: Int) {
            (mGL as GL11?)!!.glTexEnviv(target, pname, params, offset)
            checkError()
        }

        override fun glTexParameterfv(target: Int, pname: Int, params: FloatBuffer) {
            (mGL as GL11?)!!.glTexParameterfv(target, pname, params)
            checkError()
        }

        override fun glTexParameterfv(target: Int, pname: Int, params: FloatArray,
                                      offset: Int) {
            (mGL as GL11?)!!.glTexParameterfv(target, pname, params,
                    offset)
            checkError()
        }

        override fun glTexParameteri(target: Int, pname: Int, param: Int) {
            (mGL as GL11?)!!.glTexParameteri(target, pname, param)
            checkError()
        }

        override fun glTexParameteriv(target: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glTexParameteriv(target, pname, params)
            checkError()
        }

        override fun glTexParameteriv(target: Int, pname: Int, params: IntArray,
                                      offset: Int) {
        }

        override fun glTexParameterxv(target: Int, pname: Int, params: IntBuffer) {
            (mGL as GL11?)!!.glTexParameterxv(target, pname, params)
            checkError()
        }

        override fun glTexParameterxv(target: Int, pname: Int, params: IntArray,
                                      offset: Int) {
            (mGL as GL11?)!!.glTexParameterxv(target, pname, params,
                    offset)
            checkError()
        }

        override fun glVertexPointer(size: Int, type: Int, stride: Int, offset: Int) {
            (mGL as GL11?)!!.glVertexPointer(size, type, stride, offset)
            checkError()
        }

        override fun glCurrentPaletteMatrixOES(matrixpaletteindex: Int) {
            (mGL as GL11Ext?)!!.glCurrentPaletteMatrixOES(matrixpaletteindex)
            checkError()
        }

        override fun glDrawTexfOES(x: Float, y: Float, z: Float, width: Float,
                                   height: Float) {
            (mGL as GL11Ext?)!!.glDrawTexfOES(x, y, z, width,
                    height)
            checkError()
        }

        override fun glDrawTexfvOES(coords: FloatBuffer) {
            (mGL as GL11Ext?)!!.glDrawTexfvOES(coords)
            checkError()
        }

        override fun glDrawTexfvOES(coords: FloatArray, offset: Int) {
            (mGL as GL11Ext?)!!.glDrawTexfvOES(coords, offset)
            checkError()
        }

        override fun glDrawTexiOES(x: Int, y: Int, z: Int, width: Int, height: Int) {
            (mGL as GL11Ext?)!!.glDrawTexiOES(x, y, z, width, height)
            checkError()
        }

        override fun glDrawTexivOES(coords: IntBuffer) {
            (mGL as GL11Ext?)!!.glDrawTexivOES(coords)
            checkError()
        }

        override fun glDrawTexivOES(coords: IntArray, offset: Int) {
            (mGL as GL11Ext?)!!.glDrawTexivOES(coords, offset)
            checkError()
        }

        override fun glDrawTexsOES(x: Short, y: Short, z: Short, width: Short,
                                   height: Short) {
            (mGL as GL11Ext?)!!.glDrawTexsOES(x, y, z, width,
                    height)
            checkError()
        }

        override fun glDrawTexsvOES(coords: ShortBuffer) {
            (mGL as GL11Ext?)!!.glDrawTexsvOES(coords)
            checkError()
        }

        override fun glDrawTexsvOES(coords: ShortArray, offset: Int) {
            (mGL as GL11Ext?)!!.glDrawTexsvOES(coords, offset)
            checkError()
        }

        override fun glDrawTexxOES(x: Int, y: Int, z: Int, width: Int, height: Int) {
            (mGL as GL11Ext?)!!.glDrawTexxOES(x, y, z, width, height)
            checkError()
        }

        override fun glDrawTexxvOES(coords: IntBuffer) {
            (mGL as GL11Ext?)!!.glDrawTexxvOES(coords)
            checkError()
        }

        override fun glDrawTexxvOES(coords: IntArray, offset: Int) {
            (mGL as GL11Ext?)!!.glDrawTexxvOES(coords, offset)
            checkError()
        }

        override fun glLoadPaletteFromModelViewMatrixOES() {
            (mGL as GL11Ext?)!!.glLoadPaletteFromModelViewMatrixOES()
            checkError()
        }

        override fun glMatrixIndexPointerOES(size: Int, type: Int, stride: Int,
                                             pointer: Buffer) {
            (mGL as GL11Ext?)!!.glMatrixIndexPointerOES(size, type, stride,
                    pointer)
            checkError()
        }

        override fun glMatrixIndexPointerOES(size: Int, type: Int, stride: Int,
                                             offset: Int) {
            (mGL as GL11Ext?)!!.glMatrixIndexPointerOES(size, type, stride,
                    offset)
            checkError()
        }

        override fun glWeightPointerOES(size: Int, type: Int, stride: Int,
                                        pointer: Buffer) {
            (mGL as GL11Ext?)!!.glWeightPointerOES(size, type, stride,
                    pointer)
            checkError()
        }

        override fun glWeightPointerOES(size: Int, type: Int, stride: Int,
                                        offset: Int) {
            (mGL as GL11Ext?)!!.glWeightPointerOES(size, type, stride,
                    offset)
            checkError()
        }

        override fun glQueryMatrixxOES(arg0: IntBuffer, arg1: IntBuffer): Int {
            val result = (mGL as GL10Ext?)!!.glQueryMatrixxOES(arg0, arg1)
            checkError()
            return result
        }

        override fun glQueryMatrixxOES(arg0: IntArray, arg1: Int, arg2: IntArray, arg3: Int): Int {
            val result = (mGL as GL10Ext?)!!.glQueryMatrixxOES(arg0, arg1, arg2, arg3)
            checkError()
            return result
        }
    }
}