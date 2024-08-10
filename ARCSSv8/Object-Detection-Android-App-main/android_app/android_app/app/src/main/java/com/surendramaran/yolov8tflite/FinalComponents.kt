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

class FinalComponents : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var bckButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var detector: Detector
    private var cameraProvider: ProcessCameraProvider? = null
    private val requiredComponents = setOf("CPU_CHIP", "CPU_FAN", "POWER_SUP", "RAM")
    private val allComponents = setOf("CPU_CHIP", "CPU_FAN", "GPU", "NETWORK_CARD", "POWER_SUP", "RAM")
    private var detectionActive = true
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bckButton = findViewById(R.id.bck)
        initializeComponents()
        setupButtonListeners()
        setupDetector()
        handlePermissions()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            resetChecklist()
            startActivity(Intent(this, ListCompo::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }

    private fun resetChecklist() {
        DetectionState.clearDetectedComponents() // Assuming you have a method to clear the detected components
        updateChecklist()  // Refresh the checklist display
    }

    private fun initializeComponents() {
        sharedPreferences = getSharedPreferences("StorageSelection", Context.MODE_PRIVATE)
        updateChecklist()
        binding.btnNext.setOnClickListener { goToNextInterface() }
    }

    private fun setupDetector() {
        detector = Detector(baseContext, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.setup()
    }

    private fun handlePermissions() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun updateChecklist() {
        val detectedComponents = DetectionState.getDetectedComponents()
        val stringBuilder = SpannableStringBuilder()
        var allRequiredPresent = true

        allComponents.forEach { component ->
            val line = if (detectedComponents.contains(component)) {
                val spannable = SpannableString("( ✔ ) $component\n")
                spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.green)), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable
            } else {
                if (requiredComponents.contains(component)) {
                    allRequiredPresent = false
                }
                SpannableString("(   ) $component\n")
            }
            stringBuilder.append(line)
        }

        binding.checklistView.text = stringBuilder
        binding.checklistView.movementMethod = LinkMovementMethod.getInstance()

        if (allRequiredPresent) {
            sharedPreferences.edit().putBoolean("allRequiredDetected", true).apply()
            binding.btnNext.visibility = View.VISIBLE
            binding.tvStatus.text = "All critical components detected. Ready to proceed."

        } else {
            binding.btnNext.visibility = View.GONE
            val missingComponents = requiredComponents.filterNot { detectedComponents.contains(it) }
            binding.tvStatus.text = "${missingComponents.joinToString(", ")} MISSING, CAN'T PROCEED"
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
            binding.tvStatus.text = "No component detected"
            binding.tvStatus.visibility = View.VISIBLE
            binding.btnNext.visibility = View.GONE
            binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
            updateChecklist()  // Ensure this is also updated in case of changes
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            // Define a special confidence threshold for 'GPU'
            val gpuConfidenceThreshold = 0.8f  // Set a higher confidence for GPU
            val generalConfidenceThreshold = 0.5f  // General confidence for other components

            val bestBox = boundingBoxes.asSequence()
                .filter { box ->
                    // Apply different thresholds based on the class of the box
                    (box.clsName == "GPU" && box.cnf >= gpuConfidenceThreshold) ||
                            (box.clsName != "GPU" && box.cnf >= generalConfidenceThreshold)
                }
                .maxByOrNull { it.cnf }

            if (bestBox != null) {
                DetectionState.addDetectedComponents(listOf(bestBox.clsName))
                updateChecklist()
                binding.boundingBoxOverlay.setBoundingBoxes(listOf(bestBox))
                binding.tvStatus.text = "Component Detected: ${bestBox.clsName} with confidence: %.2f".format(bestBox.cnf)
            } else {
                binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
                binding.tvStatus.text = "No Components Detected with sufficient confidence"
            }
        }
    }


    private fun goToNextInterface() {
        val detectedComponents = DetectionState.getDetectedComponents().joinToString(",")
        val intent = Intent(this, Storage1::class.java).apply {
            putExtra("detectedComponents", detectedComponents)
        }
        startActivity(intent)
        resetChecklist()
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
