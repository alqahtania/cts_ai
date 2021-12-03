package com.example.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*
import org.tensorflow.lite.examples.detection.tflite.Detector
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel
import java.io.IOException
import kotlin.coroutines.CoroutineContext

class ObjectDetector(
    private val context: Context,
    private val bitmap: Bitmap,
    private val callBack: (List<Detector.Recognition>) -> Unit
) :
    CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default
    private lateinit var detector: Detector

    init {
        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                context,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun detectObject() = launch {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,
            TF_OD_API_INPUT_SIZE,
            TF_OD_API_INPUT_SIZE, true)
        val results = async { detector.recognizeImage(scaledBitmap) }

        Log.d("results", results.await().toString())
        withContext(Dispatchers.Main) {
            callBack(results.await())
        }
    }

    companion object {
        private const val TF_OD_API_MODEL_FILE = "detect.tflite"
        private const val TF_OD_API_IS_QUANTIZED = true
        private const val TF_OD_API_INPUT_SIZE = 300
        private const val TF_OD_API_LABELS_FILE = "labelmap.txt"

    }
}