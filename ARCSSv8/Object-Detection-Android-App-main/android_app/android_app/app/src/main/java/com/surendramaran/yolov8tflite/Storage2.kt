package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Storage2 : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var statusTextView: TextView
    private lateinit var nextButton: Button
    private lateinit var checklistView: TextView
    private var detectionActive = true
    private val isFrontCamera: Boolean = false
    private lateinit var bckButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        statusTextView = findViewById(R.id.tvStatus)
        nextButton = findViewById(R.id.btnNext)
        detector = Detector(baseContext, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.setup()
        checklistView = findViewById(R.id.checklistView)
        updateChecklist()
        bckButton = findViewById(R.id.bck)
        setupButtonListeners()



        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private fun setupButtonListeners() {
        bckButton.setOnClickListener {
            resetChecklist()
            startActivity(Intent(this, Storage1::class.java))
            overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
        }
    }
    private fun resetChecklist() {
        DetectionState.clearDetectedComponents() // Assuming you have a method to clear the detected components
        updateChecklist()  // Refresh the checklist display
    }

    private fun updateChecklist() {
        val detectedComponents = DetectionState.getDetectedComponents()
        val components = Constants.MEMORY
        val stringBuilder = SpannableStringBuilder()

        components.forEach { component ->
            val line = if (detectedComponents.contains(component)) {
                val spannable = SpannableString("( ✔ ) $component\n")
                spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.green)), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable
            } else {
                SpannableString("(   ) $component\n")
            }
            stringBuilder.append(line)
        }

        binding.checklistView.text = stringBuilder
        binding.checklistView.movementMethod = LinkMovementMethod.getInstance()
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
            if (!detectionActive) {
                imageProxy.close()
                return@setAnalyzer
            }

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
        const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).toTypedArray()
        private const val CONFIDENCE_THRESHOLD = 0.8f
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
            statusTextView.text = "No component detected"
            statusTextView.visibility = View.VISIBLE
            nextButton.visibility = View.GONE
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        var detected = false // Control flag to check detection status

        val detectableClasses = listOf("M2", "SATA_SSD", "HDD")
        for (box in boundingBoxes) {
            if (detectableClasses.contains(box.clsName) && box.cnf >= CONFIDENCE_THRESHOLD) {
                runOnUiThread {
                    binding.boundingBoxOverlay.setBoundingBoxes(listOf(box))
                    statusTextView.text = "${box.clsName} Detected!"
                    statusTextView.visibility = View.VISIBLE
                    DetectionState.addComponent2(box.clsName)
                    updateChecklist()
                    nextButton.visibility = View.VISIBLE
                    nextButton.setOnClickListener { goToNextInterface(box.clsName) }

                    // Save the detected component to SharedPreferences
                    val preferences = getSharedPreferences("StorageSelection", Context.MODE_PRIVATE)
                    preferences.edit().putString("detectedComponents", box.clsName).apply()
                }
                detected = true
                detectionActive = false  // Disable further detection
                binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
                break
            }
        }

        if (!detected) {
            runOnUiThread {
                if (DetectionState.getDetectedComponents().isEmpty()) {
                    statusTextView.text = "NO Component Detected"
                    statusTextView.visibility = View.VISIBLE
                    binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
                    nextButton.visibility = View.GONE
                }
            }
        }
    }

    private fun goToNextInterface(detectedComponent: String) {
        val intent = Intent(this, IntroMB::class.java)
        intent.putExtra("detectedComponents", detectedComponent)
        startActivity(intent)
        overridePendingTransition(R.animator.fade_in, R.animator.fade_out)
    }
    override fun onBackPressed() {
        // Do nothing on back press to disable the hardware back button
    }
}
