package com.example.cameraxapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.examples.detection.tflite.Detector

class FreezeAnalyzer(private val context: Context, private val callback: (Bitmap, List<Detector.Recognition>) -> Unit) : ImageAnalysis.Analyzer {
    private var flag = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if(flag){
            flag = false
            val bitmapConversion = requireNotNull(image.toBitmap())
            val bitmap = rotateBitmap(bitmapConversion, image.imageInfo.rotationDegrees, false, false)
            if(bitmap != null){
                ObjectDetector(context, bitmap){
                    callback(bitmap, it)
                }.detectObject()
            }
        }
        image.close()
    }

    fun freeze(){
        flag = true
    }

}