package com.surendramaran.yolov8tflite

object DetectionState {
    private val detectedComponents = mutableSetOf<String>()
    private val allComponents = setOf("CPU_CHIP", "CPU_FAN", "RAM", "M2" , "SATA_SSD" , "POWER_SUP" , "GPU" , "LAN_CARD" , "HDD")
    private val allParts = setOf("CPU socket", "Memory socket", "M.2 Interface", "Graphics card socket", "SATA Interface", "Power Interface", "CPU power interface", "CPU fan interface", "Motherboard baffle", "PCI interface", "PCIE interface", "Jumper interface", "USB3.0 interface", "USB2.0 interface")
    private val allComponentss = setOf(" CPU socket (MISSING_1)", " Memory socket (MISSING_2)", "M.2 Interface (MISSING_3)", "Graphics card socket (SLOT OPTIONAL 6)", "SATA Interface (DETECTED)", "Power Interface (DETECTED)", "CPU power interface (DETECTED)", "CPU fan interface (DETECTED)", "Motherboard baffle (DETECTED)", "PCI interface (SLOT OPTIONAL 1)", "PCIE interface (SLOT OPTIONAL 2)", "Jumper interface (SLOT OPTIONAL 3)", "USB3.0 interface (SLOT OPTIONAL 4)", "USB2.0 interface (SLOT OPTIONAL 5)")
    fun addComponent(component: String) {
        if (component in allParts) {
            detectedComponents.add(component)
        }
    }
    fun addComponent2(component: String) {
        if (component in allComponents) {
            detectedComponents.add(component)
        }
    }
    fun addComponent3(component: String) {
        if (component in allComponentss) {
            detectedComponents.add(component)
        }
    }
    fun addDetectedComponents(components: List<String>) {
        detectedComponents.addAll(components)

    }

    fun clearDetectedComponents() {
        detectedComponents.clear()
    }

    fun updateDetectedComponents(components: List<String>) {
        detectedComponents.clear()
        detectedComponents.addAll(components)
    }

    fun removeComponent(component: String) {
        detectedComponents.remove(component)
    }

    fun hasComponent(component: String): Boolean = component in detectedComponents

    fun getDetectedComponents(): List<String> = detectedComponents.toList()

    fun isDetectionComplete(): Boolean = detectedComponents.containsAll(allParts)

    fun resetDetections() {
        detectedComponents.clear()
    }
}


