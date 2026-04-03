/*
 * Copyright (c) 2025 - 2026 ThorVG project. All rights reserved.

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

package org.thorvg.sample.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.thorvg.sample.R
import org.thorvg.view.lottie.LottieView

class LottieViewSampleFragment : Fragment(R.layout.fragment_lottie_view_sample) {
    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            statusHandler.postDelayed(this, 100L)
        }
    }

    private var lottieView: LottieView? = null
    private var stateButton: Button? = null
    private var frameText: TextView? = null
    private var speedText: TextView? = null
    private var runningText: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lottieView = view.findViewById(R.id.lottie_view)
        stateButton = view.findViewById(R.id.anim_state)
        frameText = view.findViewById(R.id.anim_frame)
        speedText = view.findViewById(R.id.anim_speed)
        runningText = view.findViewById(R.id.anim_running)

        view.findViewById<View>(R.id.anim_state).setOnClickListener {
            val target = lottieView ?: return@setOnClickListener
            if (target.isAnimating()) {
                target.pauseAnimation()
            } else {
                target.resumeAnimation()
            }
            updateStatus()
        }

        view.findViewById<View>(R.id.anim_stop).setOnClickListener {
            lottieView?.stopAnimation()
            updateStatus()
        }

        view.findViewById<View>(R.id.anim_replay).setOnClickListener {
            lottieView?.startAnimation()
            updateStatus()
        }

        view.findViewById<View>(R.id.speed_half).setOnClickListener {
            lottieView?.setSpeed(0.5f)
            updateStatus()
        }

        view.findViewById<View>(R.id.speed_normal).setOnClickListener {
            lottieView?.setSpeed(1f)
            updateStatus()
        }

        view.findViewById<View>(R.id.speed_double).setOnClickListener {
            lottieView?.setSpeed(2f)
            updateStatus()
        }

        updateStatus()
    }

    override fun onStart() {
        super.onStart()
        statusHandler.post(statusRunnable)
    }

    override fun onStop() {
        statusHandler.removeCallbacks(statusRunnable)
        super.onStop()
    }

    override fun onDestroyView() {
        lottieView = null
        stateButton = null
        frameText = null
        speedText = null
        runningText = null
        super.onDestroyView()
    }

    private fun updateStatus() {
        val target = lottieView ?: return
        val context = context ?: return
        val isAnimating = target.isAnimating()
        runningText?.text = if (isAnimating) {
            context.getString(R.string.sample_running)
        } else {
            context.getString(R.string.sample_idle)
        }
        frameText?.text = context.getString(R.string.sample_frame_format, target.getCurrentFrame())
        speedText?.text = context.getString(R.string.sample_speed_format, target.getSpeed())
        stateButton?.text = if (isAnimating) {
            context.getString(R.string.sample_pause)
        } else {
            context.getString(R.string.sample_resume)
        }
    }
}
