package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.surendramaran.yolov8tflite.Constants.MB_LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MB_MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StepSix_LANCARD_P3 : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var statusTextView: TextView
    private lateinit var nextButton: Button
    private lateinit var bckButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statusTextView = findViewById(R.id.tvStatus)
        nextButton = findViewById(R.id.btnNext)

        bckButton = findViewById(R.id.bck)
        setupButtonListeners()


        detector = Detector(baseContext, MB_MODEL_PATH, MB_LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            startActivity(Intent(this, StepSix_LANCARD_P2::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            if (binding.viewFinder.isAttachedToWindow) {
                bindCameraUseCases()
            } else {
                binding.viewFinder.post { bindCameraUseCases() }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
            detector.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            statusTextView.text = "PLACE THE MOTHERBOARD IN THE SCREEN"
            statusTextView.visibility = View.VISIBLE
            nextButton.visibility = View.VISIBLE
            binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {

        runOnUiThread {
            // Filter and keep all bounding boxes for 'USB2.0 Interface' and 'USB3.0 Interface'
            val usb20Boxes = boundingBoxes.filter { it.clsName == " PCI interface" }
            val usb30Boxes = boundingBoxes.filter { it.clsName == " PCIE interface" }

            // Combine all bounding boxes into one list to display
            val boxesToDisplay = usb20Boxes + usb30Boxes

            // Update UI based on detection
            binding.boundingBoxOverlay.setBoundingBoxes(boxesToDisplay)
            if (boxesToDisplay.isNotEmpty()) {
                statusTextView.text = "PLUG NETWORK HERE"
            } else {
                statusTextView.text = "NO CONNECTIONS"
                nextButton.visibility = View.VISIBLE
            }
            statusTextView.visibility = View.VISIBLE
            nextButton.visibility = View.VISIBLE
            nextButton.setOnClickListener {
                goToNextInterface()
            }
            }

    }


    private fun goToNextInterface() {
        val intent = Intent(this, StepSeven_POWER_P1::class.java)
        startActivity(intent)
        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }
}