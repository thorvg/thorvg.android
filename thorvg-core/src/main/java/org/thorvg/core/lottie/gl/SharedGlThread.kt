/*
 * Copyright (c) 2026 ThorVG project. All rights reserved.

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.thorvg.core.lottie.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import androidx.annotation.RestrictTo

/**
 * Singleton shared GL thread that owns a single EGL context.
 * All rendering clients (TextureView and HardwareBuffer paths) render
 * sequentially on this thread, avoiding ThorVG's thread-safety issues.
 *
 * Reference:
 * https://github.com/LottieFiles/dotlottie-android/blob/0.13.7/dotlottie/src/main/java/com/lottiefiles/dotlottie/core/util/SharedGlThread.kt
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SharedGlThread private constructor() {
    private val thread = HandlerThread("ThorVG-Lottie-SharedGL").also { it.start() }
    val handler = Handler(thread.looper)

    var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private set
    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private set

    private var eglConfig: EGLConfig? = null
    private var eglPbuffer: EGLSurface = EGL14.EGL_NO_SURFACE
    private val clients = mutableListOf<RenderClient>()
    private var choreographer: Choreographer? = null
    private var choreographerRunning = false

    /** Tracks which client last rendered, so clients can skip redundant setGlTarget calls. */
    var lastRenderedClient: RenderClient? = null
        private set

    val clientCount: Int
        get() = clients.size

    interface RenderClient {
        /** Whether this client needs rendering. Checked each frame. */
        fun shouldRender(): Boolean
        /**
         * Called on the shared GL thread during each choreographer frame.
         *
         * [frameTimeNanos] is the vsync timestamp from [Choreographer.FrameCallback.doFrame],
         * on the [System.nanoTime] timebase. Clients should derive playback position from
         * this timestamp so playback stays time-accurate even if frames are dropped.
         */
        fun onRenderFrame(frameTimeNanos: Long): Boolean
    }

    init {
        handler.post {
            initEgl()
            choreographer = Choreographer.getInstance()
        }
    }

    fun createWindowSurface(surface: SurfaceTexture): EGLSurface {
        val config = eglConfig ?: return EGL14.EGL_NO_SURFACE
        val eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            config,
            surface,
            intArrayOf(EGL14.EGL_NONE),
            0
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.w(TAG, "eglCreateWindowSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
        return eglSurface
    }

    fun destroyWindowSurface(surface: EGLSurface) {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY && surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, surface)
        }
    }

    fun makeCurrent(surface: EGLSurface): Boolean {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY ||
            eglContext == EGL14.EGL_NO_CONTEXT ||
            surface == EGL14.EGL_NO_SURFACE
        ) {
            return false
        }
        return EGL14.eglMakeCurrent(eglDisplay, surface, surface, eglContext)
    }

    /** Make the internal PBuffer surface current. For clients that render to FBOs only. */
    fun makeDefaultCurrent(): Boolean = makeCurrent(eglPbuffer)

    fun swapBuffers(surface: EGLSurface): Boolean {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || surface == EGL14.EGL_NO_SURFACE) return false
        return EGL14.eglSwapBuffers(eglDisplay, surface)
    }

    fun register(client: RenderClient) {
        handler.post {
            if (!clients.contains(client)) {
                clients.add(client)
            }
            startChoreographerIfNeeded()
        }
    }

    /** Remove client from the render list. Must be called on the GL thread. */
    fun unregisterOnGlThread(client: RenderClient) {
        clients.remove(client)
        if (lastRenderedClient == client) {
            lastRenderedClient = null
        }
        if (clients.isEmpty()) {
            stopChoreographer()
            makeDefaultCurrent()
        }
    }

    /** Kick the choreographer if there are active clients. */
    fun requestRender() {
        handler.post { startChoreographerIfNeeded() }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!choreographerRunning) return

            var anyActive = false
            for (client in clients.toList()) {
                if (client.shouldRender()) {
                    anyActive = true
                    if (client.onRenderFrame(frameTimeNanos)) {
                        lastRenderedClient = client
                    }
                }
            }

            if (anyActive || clients.any { it.shouldRender() }) {
                choreographer?.postFrameCallback(this)
            } else {
                choreographerRunning = false
            }
        }
    }

    private fun startChoreographerIfNeeded() {
        if (!choreographerRunning && clients.any { it.shouldRender() }) {
            choreographerRunning = true
            choreographer?.postFrameCallback(frameCallback)
        }
    }

    private fun stopChoreographer() {
        choreographerRunning = false
        choreographer?.removeFrameCallback(frameCallback)
    }

    private fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.w(TAG, "eglGetDisplay failed")
            return
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.w(TAG, "eglInitialize failed")
            return
        }

        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_NONE
        )
        if (!EGL14.eglChooseConfig(eglDisplay, attributes, 0, configs, 0, 1, count, 0) ||
            count[0] == 0 ||
            configs[0] == null
        ) {
            Log.w(TAG, "eglChooseConfig failed")
            return
        }
        eglConfig = configs[0]

        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            eglConfig,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
            0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.w(TAG, "eglCreateContext failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return
        }

        eglPbuffer = EGL14.eglCreatePbufferSurface(
            eglDisplay,
            eglConfig,
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
            0
        )
        if (eglPbuffer == EGL14.EGL_NO_SURFACE) {
            Log.w(TAG, "eglCreatePbufferSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return
        }
        makeDefaultCurrent()
    }

    companion object {
        private const val TAG = "ThorVGSharedGL"
        private const val EGL_OPENGL_ES3_BIT_KHR = 0x00000040

        val instance: SharedGlThread by lazy { SharedGlThread() }
    }
}
