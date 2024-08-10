package com.surendramaran.yolov8tflite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class StepFive_GPU_P1 : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var button2: Button
    private lateinit var bckButton: Button
    private lateinit var videoView: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.gpu_p1)
        button = findViewById(R.id.button)
        button2 = findViewById(R.id.button2)
        bckButton = findViewById(R.id.bck)
        setupButtonListeners()
        videoView = findViewById(R.id.background_video)
        val backgroundUri = Uri.parse("android.resource://${packageName}/${R.raw.stepfive_gpu_p1}")
        videoView.setVideoURI(backgroundUri)
        videoView.setOnPreparedListener { mediaPlayer ->
            videoView.start() // Start the video once it is prepared
        }
    }

    private fun setupButtonListeners() {
        button.setOnClickListener {
            startActivity(Intent(this, StepFive_GPU_P2::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
        button2.setOnClickListener {
            startActivity(Intent(this, StepSix_LANCARD_P1::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
        bckButton.setOnClickListener {
            startActivity(Intent(this, ChooseStorage::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }

    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }

}
