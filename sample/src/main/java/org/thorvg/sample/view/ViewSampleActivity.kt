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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.thorvg.sample.R

class ViewSampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_sample)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sampleType = intent.getStringExtra(EXTRA_SAMPLE_TYPE)
            ?.let(ViewSampleType::valueOf)
            ?: ViewSampleType.Lottie

        supportActionBar?.setTitle(
            when (sampleType) {
                ViewSampleType.Lottie -> R.string.sample_view_title
                ViewSampleType.LottiePlayback -> R.string.sample_playback_view_title
                ViewSampleType.Svg -> R.string.sample_svg_view_title
            }
        )

        if (savedInstanceState == null) {
            val fragment = when (sampleType) {
                ViewSampleType.Lottie -> LottieViewSampleFragment()
                ViewSampleType.LottiePlayback -> LottiePlaybackProfilerFragment()
                ViewSampleType.Svg -> SvgViewSampleFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val EXTRA_SAMPLE_TYPE = "sample_type"

        fun createIntent(context: Context, sampleType: ViewSampleType): Intent {
            return Intent(context, ViewSampleActivity::class.java).apply {
                putExtra(EXTRA_SAMPLE_TYPE, sampleType.name)
            }
        }
    }
}
