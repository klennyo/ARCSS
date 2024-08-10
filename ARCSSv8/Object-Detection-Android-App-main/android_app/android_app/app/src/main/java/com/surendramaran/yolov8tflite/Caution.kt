package com.surendramaran.yolov8tflite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Caution : AppCompatActivity() {
    private lateinit var bckButton: Button
    private lateinit var videoView: VideoView
    // Changed to nullable for safety

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.caution)

        bckButton = findViewById(R.id.bck)
        setupButtonListeners()

        // Initialize Background VideoView
        videoView = findViewById(R.id.background_video)
        val backgroundUri = Uri.parse("android.resource://${packageName}/${R.raw.caution}")
        videoView.setVideoURI(backgroundUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            videoView.start() // Start the video once it is prepared
        }

    }

    fun onScreenTapped(view: View) {
        val intent = Intent(this, ProceedingCompo::class.java)
        startActivity(intent)
        // Apply the slide transition animations
        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }

    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            startActivity(Intent(this, Hands::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }


}
