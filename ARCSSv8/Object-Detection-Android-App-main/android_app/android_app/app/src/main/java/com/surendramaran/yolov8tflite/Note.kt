package com.surendramaran.yolov8tflite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity

class Note : AppCompatActivity(){

    private lateinit var bckButton: Button
    private lateinit var videoView: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_note)
        videoView = findViewById(R.id.background_video)
        val backgroundUri = Uri.parse("android.resource://${packageName}/${R.raw.note}")
        videoView.setVideoURI(backgroundUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            videoView.start() // Start the video once it is prepared
        }
        bckButton = findViewById(R.id.bck)
        setupButtonListeners()
    }
    fun onScreenTapped(view: View) {
        val intent = Intent(this, StepOne_P1::class.java)
        startActivity(intent)
        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }
    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            startActivity(Intent(this, Reminder::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }


}