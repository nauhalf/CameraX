package id.dipay.camerax

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.*
import androidx.camera.core.R
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import id.dipay.utils.CameraTimer
import id.dipay.utils.ThreadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/** reference
 * https://medium.com/androiddevelopers/camerax-learn-how-to-use-cameracontroller-e3ed10fffecf
 * */

class CameraUtil(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope,
    private val viewFinder: PreviewView,
    private val outputDirectory: String
) {

    // An instance for display manager to get display change callbacks
    private val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private var mWidth = 0
    private var mHeight = 0
    private var mFlashMode = ImageCapture.FLASH_MODE_AUTO
    private var mQuality = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    private var mLensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    // Selector showing is there any selected timer and it's value (3s or 10s)
    private var mSelectedTimer = CameraTimer.OFF

    @SuppressLint("UnsafeOptInUsageError")
    fun rotationListener(rotation: Int) {
        preview?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        imageAnalyzer?.targetRotation = rotation
    }

    fun getFlashMode() = mFlashMode


    val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == viewFinder.id) {
                rotationListener(viewFinder.display.rotation)
            }
        }
    }

    fun registerDisplayManager(){
        displayManager.registerDisplayListener(displayListener, null)
    }

    fun unregisterDisplayManager(){
        displayManager.unregisterDisplayListener(displayListener)
    }

    fun setFlashMode(@ImageCapture.FlashMode flashMode: Int): CameraUtil {
        this.mFlashMode = flashMode
        return this
    }

    fun setImageQuality(@ImageCapture.CaptureMode quality: Int): CameraUtil {
        this.mQuality = quality
        return this
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    fun startCamera() {
        // This is the CameraX PreviewView where the camera will be rendered
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // The display information
            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            // The ratio for the output image and preview
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            // The display rotation
            val rotation = viewFinder.display.rotation

            val localCameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

            // The Configuration of camera preview
            preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio) // set the camera aspect ratio
                .setTargetRotation(rotation) // set the camera rotation
                .build()

            // The Configuration of image capture
            imageCapture = Builder()
                .setCaptureMode(mQuality) // setting to have pictures with highest quality possible (may be slow)
                .setFlashMode(mFlashMode) // set capture flash
                .setTargetAspectRatio(aspectRatio) // set the capture aspect ratio
                .setTargetRotation(rotation) // set the capture rotation
//                .also {
//                    // Create a Vendor Extension for HDR
////                    val hdrImageCapture = HdrImageCaptureExtender.create(it)
//
//                    // Check if the extension is available on the device
//                    if (!hdrImageCapture.isExtensionAvailable(mLensFacing)) {
//                        // If not, hide the HDR button
////                        binding.btnHdr.visibility = View.GONE
//                    } else if (hasHdr) {
//                        // If yes, turn on if the HDR is turned on by the user
//                        hdrImageCapture.enableExtension(mLensFacing)
//                    }
//                }
                .build()

            // The Configuration of image analyzing
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(aspectRatio) // set the analyzer aspect ratio
                .setTargetRotation(rotation) // set the analyzer rotation
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // in our analysis, we care about the latest image
                .build()
                .apply {
                    // Use a worker thread for image analysis to prevent glitches
                    val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
                    setAnalyzer(
                        ThreadExecutor(Handler(analyzerThread.looper)),
                        LuminosityAnalyzer()
                    )
                }

            localCameraProvider.unbindAll() // unbind the use-cases before rebinding them

            try {
                // Bind all use cases to the camera with lifecycle
                localCameraProvider.bindToLifecycle(
                    lifecycleOwner, // current lifecycle owner
                    mLensFacing, // either front or back facing
                    preview, // camera preview use case
                    imageCapture, // image capture use case
                    imageAnalyzer, // image analyzer use case
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind use cases", e)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    fun takePicture(result: (Uri) -> Unit, timer: ((Int) -> Unit)? = null) =
        coroutineScope.launch(Dispatchers.Main) {
            // Show a timer based on user selection
            when (mSelectedTimer) {
                CameraTimer.S3 -> for (i in 3 downTo 1) {
                    timer?.invoke(i)
                    delay(1000)
                }
                CameraTimer.S10 -> for (i in 10 downTo 1) {
                    timer?.invoke(i)
                    delay(1000)
                }
            }
            timer?.invoke(0)
            captureImage(result)
        }

    fun flash(@FlashMode flash: Int) {
        setFlashMode(flash)
        imageCapture?.flashMode = flash
    }

    fun flip(selector: (Selector) -> Unit) {
        mLensFacing = if (mLensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
            selector(Selector.FRONT)
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            selector(Selector.BACK)
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun captureImage(result: (Uri) -> Unit) {
        val localImageCapture =
            imageCapture ?: throw IllegalStateException("Camera initialization failed.")

        // Setup image capture metadata
        val metadata = Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = mLensFacing == CameraSelector.DEFAULT_FRONT_CAMERA
        }

        // Options fot the output image file
        val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis())
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, outputDirectory)
            }

            val contentResolver = context.contentResolver

            // Create the output uri
            val contentUri =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            OutputFileOptions.Builder(contentResolver, contentUri, contentValues)
        } else {
            File(outputDirectory).mkdirs()
            val file = File(outputDirectory, "${System.currentTimeMillis()}.jpg")

            OutputFileOptions.Builder(file)
        }.setMetadata(metadata).build()

        val executor = Executors.newSingleThreadExecutor()
        localImageCapture.takePicture(
            outputOptions, // the options needed for the final image
            executor, // the executor, on which the task will run
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri
                        ?.let { uri ->
                            result(uri)
                            Log.d(TAG, "Photo saved in $uri")
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = "Photo capture failed: ${exception.message}"
                    Log.e(TAG, msg)
                }
            }
        )
    }


    companion object {
         const val TAG = "CameraXBasic"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0 // aspect ratio 4x3
        private const val RATIO_16_9_VALUE = 16.0 / 9.0 // aspect ratio 16x9
    }

}