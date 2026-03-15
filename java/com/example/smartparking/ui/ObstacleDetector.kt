package com.example.smartparking.ui

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await

object ObstacleDetector {
    // ML Kit detector options
    private val options by lazy {
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification() // attempts to provide labels
            .build()
    }

    /**
     * Runs detection on the bitmap and returns the list of bounding boxes (Rect) and labels.
     * We return a list of Rect for simplicity (caller maps them to slot rectangles).
     */
    suspend fun detectBoundingBoxes(bitmap: Bitmap): List<android.graphics.Rect> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val detector = ObjectDetection.getClient(options)
        val results = detector.process(image).await() // suspend
        val boxes = mutableListOf<android.graphics.Rect>()

        for (obj in results) {
            // If classification label suggests a vehicle/car, accept it.
            val label = obj.labels.firstOrNull()?.text ?: ""
            // ML Kit sometimes labels "Vehicle" or "Car"; this check is forgiving.
            if (label.equals("Car", ignoreCase = true) ||
                label.equals("Vehicle", ignoreCase = true) ||
                label.contains("vehicle", ignoreCase = true) ||
                label.contains("car", ignoreCase = true)
            ) {
                obj.boundingBox?.let { boxes.add(it) }
            } else {
                // If no label available we might still want to treat large objects as vehicles.
                // Simple heuristic: accept bounding boxes with reasonable area.
                obj.boundingBox?.let {
                    val area = it.width().toLong() * it.height()
                    if (area > 2000) boxes.add(it) // threshold tweakable
                }
            }
        }

        return boxes
    }

    /**
     * Map detection bounding box -> slotId based on intersection with provided slotRects.
     * Returns set of occupied slotIds.
     */
    fun mapBoxesToSlots(detectedBoxes: List<android.graphics.Rect>, slotRects: Map<String, Rect>): Set<String> {
        val occupied = mutableSetOf<String>()
        for (box in detectedBoxes) {
            for ((slotId, slotRect) in slotRects) {
                // If intersects significantly, mark occupied.
                if (Rect.intersects(box, slotRect)) {
                    occupied.add(slotId)
                } else {
                    // Optionally: partial overlap threshold (not implemented)
                }
            }
        }
        return occupied
    }
}
