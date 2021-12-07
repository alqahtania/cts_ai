package com.example.cameraxapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.cameraxapp.tflite.Detector

class FreezeAnalyzer(
    private val context: Context,
    private val callback: (Bitmap, List<Detector.Recognition>) -> Unit
) : ImageAnalysis.Analyzer {
    private var flag = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if (flag) {
            flag = false
            val bitmapConversion = requireNotNull(image.toBitmap())
            ObjectDetector(context, bitmapConversion) {
                val bitmap = requireNotNull(
                    rotateBitmap(
                        bitmapConversion,
                        image.imageInfo.rotationDegrees,
                        false,
                        false
                    )
                )
                callback(bitmap, it)
            }.detectObject()

        }
        image.close()
    }

    fun freeze() {
        flag = true
    }

}