package com.example.cameraxapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.nio.ByteBuffer

class FreezeAnalyzer(private val callback: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
    private var flag = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        if(flag){
            flag = false
            val bitmapConversion = requireNotNull(image.toBitmap())
            val bitmap = rotateBitmap(bitmapConversion, image.imageInfo.rotationDegrees, false, false)
            if(bitmap != null){
                callback(bitmap)
            }
        }
        image.close()
    }

    fun freeze(){
        flag = true
    }

}