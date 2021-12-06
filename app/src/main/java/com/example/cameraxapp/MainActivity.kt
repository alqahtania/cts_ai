package com.example.cameraxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
import android.util.Log
import android.util.Size
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

import android.graphics.Bitmap
import android.os.Build
import android.view.*
import com.example.cameraxapp.util.Constants.BICYCLE
import com.example.cameraxapp.util.Constants.BUS
import com.example.cameraxapp.util.Constants.CAR
import com.example.cameraxapp.util.Constants.MOTORCYCLE
import com.example.cameraxapp.util.Constants.TRUCK
import com.example.cameraxapp.util.ImageHelper
import org.tensorflow.lite.examples.detection.tflite.Detector

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // remove title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window?.insetsController

            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        BlurKit.init(this)

        // Request camera permissions
        if (allPermissionsGranted()) {
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    override fun onStart() {
        super.onStart()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
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

    private fun checkPhotoOrientationBeforeImageCapture(
        bitmap: Bitmap,
        orientation: Int,
        facesDetected: Boolean,
        imageContainsVehicle: Boolean
    ) {
        val verticalOrientationWarning =
            orientation == Surface.ROTATION_0 || orientation == Surface.ROTATION_180
        if (verticalOrientationWarning || facesDetected || !imageContainsVehicle) {
            showWarningsDialog(
                bitmap,
                verticalOrientationWarning,
                facesDetected,
                imageContainsVehicle
            )
        } else {
            savePhoto(bitmap)
        }
    }

    private fun showWarningsDialog(
        bitmap: Bitmap,
        verticalImageWarning: Boolean,
        facesDetectedWarning: Boolean,
        imageContainsVehicle: Boolean
    ) {
        var message = ""
        if (verticalImageWarning) {
            message = getString(R.string.vertical_message)
        }
        if (facesDetectedWarning) {
            message += "\n${getString(R.string.faces_message)}"
        }
        if (!imageContainsVehicle) {
            message += "\n${getString(R.string.vehicles_message)}"
        }
        message += "\n${getString(R.string.confirm_dialog)}"
        alertDialog(this, false) {
            setTitle(getString(R.string.dialog_title))
            setMessage(
                message
            )
            negativeButton(text = getString(R.string.dialog_cancel_btn)) {
                showCameraView()
                it.dismiss()
            }
            positiveButton(text = getString(R.string.dialog_ok_btn)) {
                savePhoto(bitmap)
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
            val msg = "Photo saved successfully: $savedUri"
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: " + e.message)
        }

    }

    private fun hideCameraView(
        bitmap: Bitmap,
        orientation: Int,
        facesDetected: Boolean,
        imageContainsVehicle: Boolean
    ) {
        viewFinder.visibility = View.INVISIBLE
        camera_capture_button.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        imageView.image_view.setImageBitmap(bitmap)
        imageView.saveImageBtn.setOnClickListener {
            checkPhotoOrientationBeforeImageCapture(
                bitmap,
                orientation,
                facesDetected,
                imageContainsVehicle
            )
        }
        imageView.cancelImageBtn.setOnClickListener {
            showCameraView()
        }
    }

    private fun showCameraView() {
        viewFinder.visibility = View.VISIBLE
        camera_capture_button.visibility = View.VISIBLE
        camera_capture_button.isEnabled = true
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
            val freezAnalyzer = FreezeAnalyzer(this) { bitmap, recognizables ->
                runOnUiThread {
                    detectFace(
                        bitmap,
                        checkCurrentOrientation(),
                        imageContainsAVehicle(recognizables)
                    )
                }
            }
            camera_capture_button.setOnClickListener {
                camera_capture_button.isEnabled = false
                freezAnalyzer.freeze()
            }
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
    private fun detectFace(bitmap: Bitmap, currentOrientation: Int, imageContainsVehicle: Boolean) {
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setMinFaceSize(0.01f).build()
        val faceDetector = FaceDetection.getClient(faceDetectorOptions)
        var originalBitmap = bitmap
        val inputImage = InputImage.fromBitmap(
            originalBitmap,
            OrientationManager.getOrientationDegree(currentOrientation)
        )
        val imageHelper = ImageHelper()
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                var blurredFaces: Bitmap? = null
                // to rotate the image once in the for loop
                var accessed = false
                for (face in faces) {
                    val bounds = face.boundingBox
                    face.headEulerAngleX
                    // we need to rotate the image if the orientation is not vertical (0) to blur the right coordinate from face.boundingBox returned from the api
                    // we put it inside the loop so we don't touch the original image if no faces are detected
                    // if image is taken vertically but upside down we rotate it back up
                    if (currentOrientation != 0 && !accessed) {
                        accessed = true
                        originalBitmap = rotateBitmap(
                            bitmap,
                            OrientationManager.getOrientationDegree(currentOrientation),
                            false,
                            false
                        )!!
                    }
                    blurredFaces = imageHelper.blurFace(originalBitmap, bounds, true)
                }
                // after the
                if (currentOrientation != 0 && blurredFaces != null) {
                    blurredFaces = rotateBitmap(
                        blurredFaces,
                        OrientationManager.getOrientationDegree(currentOrientation),
                        true,
                        true
                    )!!
                }
                runOnUiThread {
//                    val msg = if(faces.size == 1) "${faces.size} person detected" else if (faces.size > 1) "${faces.size} people deteced" else "no people detected"
//                    showToast(msg)
                    if (blurredFaces != null) {
                        hideCameraView(blurredFaces, currentOrientation, true, imageContainsVehicle)
                    } else {
                        hideCameraView(
                            originalBitmap,
                            currentOrientation,
                            false,
                            imageContainsVehicle
                        )
                    }

                }
            }
            .addOnFailureListener { e ->
                Log.e("facedetection", "error -> ${e.message}")
                showToast("An error occurred while detecting faces")
            }
    }


    private fun imageContainsAVehicle(recognizables: List<Detector.Recognition>): Boolean {
        val vehiclesDetected = recognizables.filter {
            it.title == CAR || it.title == MOTORCYCLE || it.title == BUS || it.title == TRUCK || it.title == BICYCLE
        }
        val carHighestConfid = getHighestConfidenceForEachLabel(CAR, vehiclesDetected) * 100
        val busHighestConfid = getHighestConfidenceForEachLabel(BUS, vehiclesDetected) * 100
        val motorcycleHighestConfid = getHighestConfidenceForEachLabel(MOTORCYCLE, vehiclesDetected) * 100
        val truckHighestConfid = getHighestConfidenceForEachLabel(TRUCK, vehiclesDetected) * 100
        val bicycleHighestConfid = getHighestConfidenceForEachLabel(BICYCLE, vehiclesDetected) * 100
        val message = "Car = $carHighestConfid%\nBus = $busHighestConfid%\nMotorcycle = $motorcycleHighestConfid%\nTruck = $truckHighestConfid%\nBicycle = $bicycleHighestConfid%"
        // TODO remove this dialog
        alertDialog(this, false) {
            setMessage(
                message
            )

            positiveButton(text = getString(R.string.dialog_cancel_btn)) {
                it.dismiss()
            }

        }.show()

        return vehiclesDetected.any {
            it.confidence > VEHICLES_CONFIDENCE_THRESHOLD
        }
    }

    private fun getHighestConfidenceForEachLabel(title : String, list : List<Detector.Recognition>) : Float{
        return list.filter {
            it.title == title
        }.map {
            it.confidence
        }.maxOrNull() ?: 0.0f
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val VEHICLES_CONFIDENCE_THRESHOLD = 0.35
    }
}