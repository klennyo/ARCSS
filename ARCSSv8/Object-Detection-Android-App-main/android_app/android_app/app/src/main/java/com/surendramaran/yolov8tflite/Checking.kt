package com.surendramaran.yolov8tflite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.surendramaran.yolov8tflite.databinding.StoragechoiceBinding

class Checking : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var bckButton: Button
    private lateinit var videoView: VideoView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.checking)
        button = findViewById(R.id.button)
        button2 = findViewById(R.id.button2)
        button3 = findViewById(R.id.button3)
        bckButton = findViewById(R.id.bck)


        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        button.setOnClickListener {
            startActivity(Intent(this, Checking1::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
        button2.setOnClickListener {
            startActivity(Intent(this, Checking2::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
        button3.setOnClickListener {
            startActivity(Intent(this, Checking3::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
        bckButton.setOnClickListener {
            startActivity(Intent(this, front::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }


}
