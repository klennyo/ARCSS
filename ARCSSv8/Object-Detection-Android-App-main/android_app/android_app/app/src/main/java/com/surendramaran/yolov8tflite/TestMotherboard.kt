package com.surendramaran.yolov8tflite

import android.Manifest
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
import com.surendramaran.yolov8tflite.Constants.MB_LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MB_MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TestMotherboard : AppCompatActivity(), Detector.DetectorListener {
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
    private lateinit var checklistView: TextView
    private lateinit var componentFilters: Map<String, View>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statusTextView = findViewById(R.id.tvStatus)
        nextButton = findViewById(R.id.btnNext)


        detector = Detector(baseContext, MB_MODEL_PATH, MB_LABELS_PATH, this)
        detector.setup()

        checklistView = findViewById(R.id.checklistView)
        updateChecklist()

        initializeComponentFilters()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initializeComponentFilters() {
        componentFilters = mapOf(
            "CPU socket" to binding.boundingBoxOverlay,
            "Memory socket" to binding.boundingBoxOverlay,
            "M.2 Interface" to binding.boundingBoxOverlay,
            "Graphics card socket" to binding.boundingBoxOverlay,
            "SATA Interface" to binding.boundingBoxOverlay,
            "Power Interface" to binding.boundingBoxOverlay,
            "CPU power interface" to binding.boundingBoxOverlay,
            "CPU fan interface" to binding.boundingBoxOverlay,
            "Motherboard baffle" to binding.boundingBoxOverlay,
            "PCI interface" to binding.boundingBoxOverlay,
            "PCIE interface" to binding.boundingBoxOverlay,
            "Jumper interface" to binding.boundingBoxOverlay,
            "USB3.0 interface" to binding.boundingBoxOverlay,
            "USB2.0 interface" to binding.boundingBoxOverlay,
            // Add other components as needed
        )
    }

    private fun updateChecklist() {
        val detectedComponents = DetectionState.getDetectedComponents()
        val components = Constants.ALL_PARTS
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
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        // Debugging to ensure cameraProvider is not null and proper initialization
        Log.d(TAG, "Binding camera use cases")
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        // Initialize and bind the camera use cases
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
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Ensure this is supported
                .build()
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
        private const val CONFIDENCE_THRESHOLD = 0.8f
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            statusTextView.text = "No component detected"
            statusTextView.visibility = View.VISIBLE
            nextButton.visibility = View.GONE
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            var detected = false
            for ((name, overlay) in componentFilters) {
                boundingBoxes.filter { it.clsName == name && it.cnf >= CONFIDENCE_THRESHOLD }
                    .maxByOrNull { it.cnf }?.also { detection ->
                        // Ensure that `detection` is the correct type expected by `setBoundingBoxes`
                        Log.d(TAG, "Detected: $name with confidence: ${detection.cnf}")

                        statusTextView.text = "$name Detected!"
                        statusTextView.visibility = View.VISIBLE
                        DetectionState.addComponent(name)
                        detected = true
                    }
            }


            if (!detected) {
                statusTextView.text = "NO Component Detected"
                statusTextView.visibility = View.VISIBLE
                binding.boundingBoxOverlay.setBoundingBoxes(emptyList())
                nextButton.visibility = View.GONE
            } else {
                nextButton.visibility = View.VISIBLE
                nextButton.setOnClickListener {
                    goToNextInterface()
                }
            }
            updateChecklist()  // Make sure this function correctly updates the UI
        }
    }

    private fun goToNextInterface() {
        val intent = Intent(this, front::class.java)
        startActivity(intent)
    }
}
