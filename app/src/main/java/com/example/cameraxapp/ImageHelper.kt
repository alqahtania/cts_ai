package com.example.cameraxapp

import android.graphics.*
import io.alterac.blurkit.BlurKit

class ImageHelper {

     fun blurFace(bitmap: Bitmap, rect: Rect, circular : Boolean): Bitmap {
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top
        val rectBitmap = Bitmap.createBitmap(w, h, bitmap.config)
        val canvas = Canvas(rectBitmap)
        canvas.drawBitmap(bitmap, -rect.left.toFloat(), -rect.top.toFloat(), null)
        var blurredBitmap = BlurKit.getInstance().blur(rectBitmap, 25)
         if(circular){
             blurredBitmap = getCircularBitmap(blurredBitmap)
         }
        val originalCanvas = Canvas(bitmap)
        if(blurredBitmap != null)
            originalCanvas.drawBitmap(blurredBitmap, rect.left.toFloat(), rect.top.toFloat(), Paint(
                Paint.FILTER_BITMAP_FLAG)
            )
        return bitmap
    }

    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(),
            (bitmap.width / 2).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output
    }

}