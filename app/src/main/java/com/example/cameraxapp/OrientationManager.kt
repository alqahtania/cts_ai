package com.example.cameraxapp

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface


class OrientationManager(private val context: Context, private val rate: Int, private val listener: (ScreenOrientation) -> Unit) :
    OrientationEventListener(context, rate) {
    enum class ScreenOrientation(val orientaion : Int) {
        REVERSED_LANDSCAPE(Surface.ROTATION_270), LANDSCAPE(Surface.ROTATION_90), PORTRAIT(Surface.ROTATION_0), REVERSED_PORTRAIT(Surface.ROTATION_180)
    }

    var screenOrientation: ScreenOrientation? = null

    override fun onOrientationChanged(orientation: Int) {
        if (orientation == -1) {
            return
        }
        val newOrientation = if (orientation >= 45 && orientation < 135) {
            ScreenOrientation.REVERSED_LANDSCAPE
        } else if (orientation >= 135 && orientation < 225) {
            ScreenOrientation.REVERSED_PORTRAIT
        } else if (orientation >= 225 && orientation < 315) {
            ScreenOrientation.LANDSCAPE
        } else {
            ScreenOrientation.PORTRAIT
        }
        if (newOrientation != screenOrientation) {
            screenOrientation = newOrientation
            screenOrientation?.let{
                listener(it)
            }
        }
    }


}