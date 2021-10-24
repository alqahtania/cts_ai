package com.example.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

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
        var score = 0.0f
        imageClassifier.classify(image)
            .forEach { classifications ->
                score = classifications.toScore()
            }
        return score
    }

    private fun Classifications.toScore(): Float {
        categories.forEach { category ->
                Log.d(this::class.java.name, "label ${category.label}")
                return category.score
        }
        return 0.0f
    }


    private companion object {
        const val MODEL_PATH = "yolov5s.tflite"
        const val SCORE_THRESHOLD = 0.05f
    }
}