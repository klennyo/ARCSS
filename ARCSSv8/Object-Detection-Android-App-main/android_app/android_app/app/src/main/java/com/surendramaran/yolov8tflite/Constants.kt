package com.surendramaran.yolov8tflite

object Constants {
    const val CB_PATH = "cobweb.tflite"
    const val CB_LABELS = "cobweb_label.txt87"
    const val TEST_PATH = "lastchecking.tflite"
    const val TESTLABELS_PATH_P2 = "lastchecking.txt"
    const val TESTLABELS_PATH = "checkcheck.txt"
    const val MODEL_PATH = "realcompov8.tflite"
    const val LABELS_PATH = "labelss.txt"
    const val HANDMODEL_PATH = "finalhands.tflite"
    const val HANDLABELS_PATH = "labelsss.txt"
    val ALL_COMPONENTS = listOf("CPU_CHIP", "CPU_FAN", "RAM", "POWER_SUP", "GPU", "LAN_CARD")
    val MEMORY = listOf("M2", "SATA_SSD", "HDD")
    val ALL_PARTS = listOf("CPU socket", "Memory socket", "M.2 Interface", "Graphics card socket", "SATA Interface", "Power Interface", "CPU power interface", "CPU fan interface", "Motherboard baffle", "PCI interface", "PCIE interface", "Jumper interface", "USB3.0 interface", "USB2.0 interface")
    const val MB_MODEL_PATH = "testmb.tflite"
    const val MB_LABELS_PATH = "mblabels.txt"
}
