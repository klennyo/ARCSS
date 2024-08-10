package com.surendramaran.yolov8tflite

object StatusState {
    private val detectedComponents = mutableSetOf<String>()
    private val allComponents = setOf("CAUTION:NO ASWB", "PROCEED")

    fun addComponent(component: String) {
        if (component in allComponents) {
            detectedComponents.add(component)
        }
    }
    fun removeComponent(component: String) {
        detectedComponents.remove(component)
    }

    fun hasComponent(component: String): Boolean = component in detectedComponents

    fun getDetectedComponents(): List<String> = detectedComponents.toList()

    fun isDetectionComplete(): Boolean = detectedComponents.containsAll(allComponents)

    fun resetDetections() {
        detectedComponents.clear()
    }
}


