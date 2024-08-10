package com.surendramaran.yolov8tflite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class front : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var transitionVideoView: VideoView? = null  // Changed to nullable for safety

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.front)

        // Initialize Background VideoView
        videoView = findViewById(R.id.background_video)
        val backgroundUri = Uri.parse("android.resource://${packageName}/${R.raw.finalintro}")
        videoView.setVideoURI(backgroundUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true // Ensure the video loops continuously
            videoView.start() // Start the video once it is prepared
        }

        // Attempt to initialize Transition VideoView
        transitionVideoView = findViewById(R.id.transition_video_view)
    }

    // This method will be called when the screen is tapped
    fun onScreenTapped(view: View) {
        // Stop the video playback before starting the transition
        videoView.stopPlayback()
        Log.d("VideoState", "Video stopped. Transitioning now.")

        // Create an intent to start the Safety activity
        val intent = Intent(this, StepOne_P2::class.java)
        startActivity(intent)

        // Apply the slide transition animations
        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }

    override fun onResume() {
        super.onResume()
        if (!videoView.isPlaying) {
            videoView.start() // Resume video playback if not already playing
        }
    }

    override fun onPause() {
        super.onPause()
        videoView.pause() // Pause video playback when the activity is not visible
    }

    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }
}
