package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FinalChecking_P2 : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bckButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var detector: Detector
    private var cameraProvider: ProcessCameraProvider? = null
    private val requiredComponents = setOf("CPU socket (MISSING_1)", "CPU fan interface (DETECTED)", "Power Interface (DETECTED)", "CPU power interface (DETECTED)")
    private val allComponentss = setOf("CPU socket (MISSING_1)", "Memory socket (MISSING_2)", "M.2 Interface (MISSING_3)", "Graphics card socket (SLOT OPTIONAL 6)", "SATA Interface (DETECTED)", "Power Interface (DETECTED)", "CPU power interface (DETECTED)", "CPU fan interface (DETECTED)", "Motherboard baffle (DETECTED)", "PCI interface (SLOT OPTIONAL 1)", "PCIE interface (SLOT OPTIONAL 2)", "Jumper interface (SLOT OPTIONAL 3)", "USB3.0 interface (SLOT OPTIONAL 4)", "USB2.0 interface (SLOT OPTIONAL 5)")
    private var detectionActive = true
    private var camera: Camera? = null
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bckButton = findViewById(R.id.bck)
        nextButton = findViewById(R.id.btnNext)
        setupButtonListeners()
        setupDetector()
        handlePermissions()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            startActivity(Intent(this, ListCompo::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }

    private fun setupDetector() {
        detector = Detector(baseContext, Constants.MB_MODEL_PATH, Constants.TESTLABELS_PATH, this)
        detector.setup()
    }

    private fun handlePermissions() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
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
        val rotation = binding.viewFinder.display.rotation
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (detectionActive) {
                        processImageProxy(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        cameraProvider?.unbindAll()  // Clear any previous use cases binding
        try {
            camera = cameraProvider?.bindToLifecycle(
                this,  // LifecycleOwner
                cameraSelector,
                preview,
                imageAnalyzer
            )  // Returns Camera, which could be null
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        imageProxy.use { it.planes[0].buffer.rewind(); bitmapBuffer.copyPixelsFromBuffer(it.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
        detector.detect(rotatedBitmap)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.tvStatus.text = "Detecting Motherboard"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.black))
            binding.tvStatus.visibility = View.VISIBLE
            nextButton.visibility = View.GONE
            binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
             // Ensure this is also updated in case of changes
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            // Define a special confidence threshold for 'GPU'
            val gpuConfidenceThreshold = 0.8f  // Set a higher confidence for GPU
            val generalConfidenceThreshold = 0.5f  // General confidence for other components

            // Filter bounding boxes based on the confidence thresholds
            val detectedBoxes = boundingBoxes.filter { box ->
                (box.clsName == "GPU" && box.cnf >= gpuConfidenceThreshold) ||
                        (box.clsName != "GPU" && box.cnf >= generalConfidenceThreshold)
            }

            if (detectedBoxes.isNotEmpty()) {
                // Update the detected components
                val detectedComponents = detectedBoxes.map { it.clsName }
                DetectionState.addDetectedComponents(detectedComponents)

                // Update the checklist


                // Update the bounding boxes overlay
                binding.boundingBoxOverlay.setBoundingBoxes(detectedBoxes)

                // Check for specific components that should hide the proceed button
                val componentsToCheck = listOf(
                    "CPU socket (MISSING_1)",
                    "CPU fan interface (DETECTED)",
                    "Power Interface (DETECTED)",
                    "CPU power interface (DETECTED)"
                )
                val shouldHideProceedButton = detectedComponents.any { it in componentsToCheck }

                if (shouldHideProceedButton) {
                    nextButton.visibility = View.GONE
                    binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
                    binding.tvStatus.text = "Specific components detected, can't proceed"
                } else {
                    nextButton.visibility = View.VISIBLE
                    binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
                    binding.tvStatus.text = "PROCEED!"
                    nextButton.setOnClickListener {
                        goToNextInterface()
                    }
                }
            } else {
                // No components detected with sufficient confidence
                binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
                binding.tvStatus.text = "Scanning Motherboard"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }




    private fun goToNextInterface() {
        val detectedComponents = DetectionState.getDetectedComponents().joinToString(",")
        val intent = Intent(this, FinalChecking_P3::class.java).apply {
            putExtra("detectedComponents", detectedComponents)
        }
        startActivity(intent)

        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }
}
