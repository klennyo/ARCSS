package com.surendramaran.yolov8tflite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide


class StepEight_OC_P1 : AppCompatActivity() {
    private lateinit var bckButton: Button
    private lateinit var videoView: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.oc_p1)

        bckButton = findViewById(R.id.bck)
        setupButtonListeners()
        videoView = findViewById(R.id.background_video)
        val backgroundUri = Uri.parse("android.resource://${packageName}/${R.raw.stepeight_oc_p1}")
        videoView.setVideoURI(backgroundUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            videoView.start() // Start the video once it is prepared
        }
        // Load the GIF using Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.otherc)
            .into(findViewById(R.id.gifImageView))
    }

    fun onScreenTapped(view: View) {
        val intent = Intent(this, StepEight_OC_P2::class.java)
        startActivity(intent)
        // Apply the slide transition animations
        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }
    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            startActivity(Intent(this, StepSeven_POWER_P1::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }
}
