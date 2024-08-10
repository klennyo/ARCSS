package com.surendramaran.yolov8tflite

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

class TensorFlowLiteManager(private val context: Context, private val modelPath: String) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    init {
        setupTensorFlowLiteInterpreter()
    }

    private fun setupTensorFlowLiteInterpreter() {
        try {
            // Load the TensorFlow Lite model
            val tfliteModel: MappedByteBuffer = FileUtil.loadMappedFile(context, modelPath)

            // Configure the interpreter to use GPU acceleration
            gpuDelegate = GpuDelegate()
            val options = Interpreter.Options().apply {
                addDelegate(gpuDelegate)
            }

            // Create the TensorFlow Lite interpreter
            interpreter = Interpreter(tfliteModel, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun runInference(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer) {
        interpreter?.run(inputBuffer, outputBuffer)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
    }
}

