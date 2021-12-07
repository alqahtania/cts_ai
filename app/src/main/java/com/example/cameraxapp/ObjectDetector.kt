package com.example.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.cameraxapp.tflite.Detector
import com.example.cameraxapp.tflite.TFLiteObjectDetectionAPIModel
import kotlinx.coroutines.*
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
        // to rotate and run the image from all angles to get more accuracy
        val angles = hashMapOf<Int, Int>(
            0 to 0,
            1 to 90,
            2 to 180,
            3 to 270,
            4 to 350
        )
        // copy the original bitmap so we don't lose the bytes when we rotate it below
        var copyBitmap = bitmap.copy(bitmap.config, true)
        val allResults = mutableListOf<Detector.Recognition>()
        // run it first for the original bitmap
        val firstResult = async { detector.recognizeImage(bitmap) }
        allResults.addAll(firstResult.await())
        for (i in 0 until angles.size) {
            copyBitmap = requireNotNull(rotateBitmap(copyBitmap, requireNotNull(angles[i]), false, false))
            val result = async { detector.recognizeImage(copyBitmap) }
            allResults.addAll(result.await())
            Log.d("results", allResults.toString())
        }

        withContext(Dispatchers.Main) {
            callBack(allResults)
        }
    }

    companion object {
        const val TF_OD_API_MODEL_FILE = "detect.tflite"
        const val TF_OD_API_IS_QUANTIZED = true
        const val TF_OD_API_INPUT_SIZE = 300
        const val TF_OD_API_LABELS_FILE = "labelmap.txt"

    }
}