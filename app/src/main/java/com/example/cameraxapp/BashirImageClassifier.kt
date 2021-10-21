package com.example.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.segmenter.OutputType

internal class BashirImageClassifier(val context: Context) {

    private val classifierOptions = ImageClassifier.ImageClassifierOptions
        .builder()
        .setMaxResults(1)
        .setScoreThreshold(SCORE_THRESHOLD)
        .build()

    private val imageClassifier by lazy {
        ImageClassifier.createFromFileAndOptions(context, MODEL_PATH, classifierOptions)
    }

    fun getScore(image: TensorImage, bitmap : Bitmap? = null): Float {
        var hotDogScore = 0.0f
        imageClassifier.classify(image)
            .forEach { classifications ->
                hotDogScore = classifications.toHotDogScore()
            }
        return hotDogScore
    }

    private fun Classifications.toHotDogScore(): Float {
        categories.forEach { category ->
                Log.d(this::class.java.name, "label ${category.label}")
                return category.score
        }
        return 0.0f
    }

    private fun getSensorOrientation(){

//        return when (getWindowManager().getDefaultDisplay().getRotation()) {
//            Surface.ROTATION_270 -> 270
//            Surface.ROTATION_180 -> 180
//            Surface.ROTATION_90 -> 90
//            else -> 0
//        }
    }

    private companion object {
        const val MODEL_PATH = "yolov5s.tflite"
        const val SCORE_THRESHOLD = 0.05f
    }
}