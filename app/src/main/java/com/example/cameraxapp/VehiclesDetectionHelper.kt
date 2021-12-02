package com.example.cameraxapp

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.cameraxapp.imageclassification.ClassifierModel
import com.example.cameraxapp.imageclassification.ImageClassification
import com.example.cameraxapp.imageclassification.Recognizable
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class VehiclesDetectionHelper(private val context : Context, private val bitmap : Bitmap, private val callBack : (List<Recognizable>) -> Unit) : CoroutineScope {

    private lateinit var imageClassification: ImageClassification
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default


    private suspend fun loadModule() {
            imageClassification = ImageClassification.create(
                classifierModel = ClassifierModel.QUANTIZED,
                assetManager = context.assets,
                modelPath = MODEL_PATH,
                labelPath = LABEL_PATH
            )
    }

    fun detectObject() = launch {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        loadModule()
        val results = async { imageClassification.classifyImage(scaledBitmap) }

        Log.d("results", results.await().toString())
        withContext(Dispatchers.Main) {
//            object_name.text = results.await().toString()
            callBack(results.await())
        }
    }


    companion object{
        private const val MODEL_PATH = "mobilenet_v1_1.0_224.tflite"
        private const val LABEL_PATH = "labels.txt"
        private const val INPUT_SIZE = 224
        private const val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}