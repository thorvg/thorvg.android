package org.thorvg.sample

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import org.thorvg.lottie.LottieAnimationView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lottieView = findViewById<LottieAnimationView>(R.id.lottie_view)

        findViewById<View>(R.id.anim_state).setOnClickListener { v: View ->
            val button = v as TextView
            if ("Pause".contentEquals(button.text)) {
                lottieView.pauseAnimation()
                button.text = "Resume"
            } else {
                lottieView.resumeAnimation()
                button.text = "Pause"
            }
        }
    }
}