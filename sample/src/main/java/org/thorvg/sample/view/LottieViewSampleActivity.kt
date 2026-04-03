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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.thorvg.view.lottie.LottieView
import org.thorvg.sample.R

class LottieViewSampleActivity : AppCompatActivity() {
    private val statusHandler = Handler(Looper.getMainLooper())
    private val statusRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            statusHandler.postDelayed(this, 100L)
        }
    }

    private lateinit var lottieView: LottieView
    private lateinit var stateButton: Button
    private lateinit var frameText: TextView
    private lateinit var speedText: TextView
    private lateinit var runningText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_sample)

        title = getString(R.string.sample_view_title)
        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lottieView = findViewById(R.id.lottie_view)
        stateButton = findViewById(R.id.anim_state)
        frameText = findViewById(R.id.anim_frame)
        speedText = findViewById(R.id.anim_speed)
        runningText = findViewById(R.id.anim_running)

        findViewById<View>(R.id.anim_state).setOnClickListener {
            if (lottieView.isAnimating()) {
                lottieView.pauseAnimation()
            } else {
                lottieView.resumeAnimation()
            }
            updateStatus()
        }

        findViewById<View>(R.id.anim_stop).setOnClickListener {
            lottieView.stopAnimation()
            updateStatus()
        }

        findViewById<View>(R.id.anim_replay).setOnClickListener {
            lottieView.startAnimation()
            updateStatus()
        }

        findViewById<View>(R.id.speed_half).setOnClickListener {
            lottieView.setSpeed(0.5f)
            updateStatus()
        }

        findViewById<View>(R.id.speed_normal).setOnClickListener {
            lottieView.setSpeed(1f)
            updateStatus()
        }

        findViewById<View>(R.id.speed_double).setOnClickListener {
            lottieView.setSpeed(2f)
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updateStatus() {
        val isAnimating = lottieView.isAnimating()
        runningText.text = if (isAnimating) {
            getString(R.string.sample_running)
        } else {
            getString(R.string.sample_idle)
        }
        frameText.text = getString(R.string.sample_frame_format, lottieView.getCurrentFrame())
        speedText.text = getString(R.string.sample_speed_format, lottieView.getSpeed())
        stateButton.text = if (isAnimating) {
            getString(R.string.sample_pause)
        } else {
            getString(R.string.sample_resume)
        }
    }
}
