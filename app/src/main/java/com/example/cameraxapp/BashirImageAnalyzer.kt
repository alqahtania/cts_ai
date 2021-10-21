package com.example.cameraxapp

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

internal class BashirImageAnalyzer(
    private val imageClassifier: BashirImageClassifier,
    private val scoreListener: (Float) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {

        scoreListener(
            imageClassifier.getScore(image.toTensorImage(), image.toBitmap())
        )
        image.close()
    }
}