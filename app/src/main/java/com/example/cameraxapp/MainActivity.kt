package com.example.cameraxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.alterac.blurkit.BlurKit
import kotlinx.android.synthetic.main.image_captured.view.*
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.PorterDuff

import android.graphics.PorterDuffXfermode

import android.graphics.Bitmap





typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BlurKit.init(this)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkPhotoOrientationBeforeImageCapture(bitmap: Bitmap, orientation: Int) {
        if (orientation == Surface.ROTATION_0 || orientation == Surface.ROTATION_180) {
            showVerticalImageWarningDialog(bitmap)
        } else {
            savePhoto(bitmap)
        }
    }

    private fun showVerticalImageWarningDialog(bitmap: Bitmap){
        alertDialog(this, false) {
            setTitle("تنبيه")
            setMessage(
                "تم التقاط الصورة بطريقة عمودية، \n" +
                        "\n" +
                        "للتأكيد، الرجاء اختيار \"نعم\" "
            )
            positiveButton(text = "نعم") {
                savePhoto(bitmap)
                it.dismiss()
            }
            negativeButton(text = "الغاء") {
                showCameraView()
                it.dismiss()
            }
        }.show()
    }

    private fun savePhoto(image: Bitmap) {

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        try {
            val fos = FileOutputStream(photoFile)
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.close()
            showCameraView()
            val savedUri = Uri.fromFile(photoFile)
            val msg = "Photo capture succeeded: $savedUri"
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: " + e.message)
        }

    }

    private fun hideCameraView(bitmap: Bitmap, orientation: Int) {
        viewFinder.visibility = View.INVISIBLE
        camera_capture_button.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        imageView.image_view.setImageBitmap(bitmap)
        imageView.saveImageBtn.setOnClickListener {
            checkPhotoOrientationBeforeImageCapture(bitmap, orientation)
        }
        imageView.cancelImageBtn.setOnClickListener {
            showCameraView()
        }
    }

    private fun showCameraView() {
        viewFinder.visibility = View.VISIBLE
        camera_capture_button.visibility = View.VISIBLE
        imageView.visibility = View.INVISIBLE
    }

    private fun startCamera() {
        //This is used to bind the lifecycle of cameras to the lifecycle owner.
        // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()

            imageCapture?.let {
                setTargetRotation(it)
            }

            val resolutionSize = Size(viewFinder.width, viewFinder.height)
            val freezAnalyzer = FreezeAnalyzer { bitmap ->
                runOnUiThread {
                    detectFace(bitmap,checkCurrentOrientation())
                }
            }
            camera_capture_button.setOnClickListener { freezAnalyzer.freeze() }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(resolutionSize)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        freezAnalyzer
                    )
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    private fun checkCurrentOrientation(): Int {
        return imageCapture!!.targetRotation
    }

    private fun setTargetRotation(imageCapture: ImageCapture): Int {
        OrientationManager(this, SensorManager.SENSOR_DELAY_NORMAL) { screenOrientation ->
            imageCapture.targetRotation = screenOrientation.orientaion
        }.enable()
        return imageCapture.targetRotation
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun detectFace(bitmap: Bitmap, currentOrientation: Int) {
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setMinFaceSize(0.01f).build()
        val faceDetector = FaceDetection.getClient(faceDetectorOptions)
        val inputImage =  InputImage.fromBitmap(bitmap, OrientationManager.getOrientationDegree(currentOrientation))
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                var blurredFaces : Bitmap? = null
                for (face in faces) {
                    val bounds = face.boundingBox
                    face.headEulerAngleX
                    blurredFaces = blurFaces(bitmap, bounds)
                }
                runOnUiThread {
                    val msg = if(faces.size == 1) "${faces.size} person detected" else if (faces.size > 1) "${faces.size} people deteced" else "no people detected"
                    showToast(msg)
                    if(blurredFaces != null){
                        hideCameraView(blurredFaces, currentOrientation)
                    }else{
                        hideCameraView(bitmap, currentOrientation)
                    }

                }
            }
            .addOnFailureListener { e ->
                Log.e("facedetection", "error -> ${e.message}")
                showToast("An error occurred while detecting faces")
            }
    }

    private fun blurFaces(bitmap: Bitmap, rect: Rect): Bitmap? {
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top
        val rectBitmap = Bitmap.createBitmap(w, h, bitmap.config)
        val canvas = Canvas(rectBitmap)
        canvas.drawBitmap(bitmap, -rect.left.toFloat(), -rect.top.toFloat(), null)
        val blurredBitmap = BlurKit.getInstance().blur(rectBitmap, 25)
        val circularBitmap = getCircularBitmap(blurredBitmap)
        val originalCanvas = Canvas(bitmap)
        if(blurredBitmap != null)
            originalCanvas.drawBitmap(circularBitmap, rect.left.toFloat(), rect.top.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
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
    

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}