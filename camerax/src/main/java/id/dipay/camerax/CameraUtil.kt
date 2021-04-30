package id.dipay.camerax

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.SensorRotationListener
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
    private val activity: Activity,
) {

    // An instance for display manager to get display change callbacks
    private val displayManager by lazy { activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private var preview: Preview? = null
    private var mPreviewDisplay: Display? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private var mFlashMode = FLASH_MODE_OFF
    private var mQuality = CAPTURE_MODE_MAXIMIZE_QUALITY
    private var mLensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    // Selector showing is there any selected timer and it's value (3s or 10s)
    private var mSelectedTimer = CameraTimer.OFF

    var mSensorRotationListener: SensorRotationListener? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var coroutineScope: CoroutineScope? = null
    private var viewFinder: PreviewView? = null
    private var outputDirectory: String? = ""

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        @SuppressLint("UnsafeOptInUsageError")
        override fun onDisplayChanged(displayId: Int) {
            mPreviewDisplay?.let {
                if (it.displayId == displayId) {
                    preview?.targetRotation = it.rotation
                }
            }

        }
    }

    init {
        // Listen to motion sensor reading and set target rotation for ImageCapture and
        // VideoCapture.
        mSensorRotationListener = object : SensorRotationListener(activity) {
            override fun onRotationChanged(rotation: Int) {
                imageCapture?.targetRotation = rotation
            }
        }

        mPreviewDisplay =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                activity.display
            } else {
                activity.windowManager.defaultDisplay
            }

    }

    fun setLifecycleOwner(lifecycleOwner: LifecycleOwner): CameraUtil {
        this.lifecycleOwner = lifecycleOwner
        return this
    }

    fun setCoroutineScope(coroutineScope: CoroutineScope): CameraUtil {
        this.coroutineScope = coroutineScope
        return this
    }

    fun setPreviewView(previewView: PreviewView): CameraUtil {
        this.viewFinder = previewView
        return this
    }

    fun setOutputDirectory(outputDirectory: String): CameraUtil {
        this.outputDirectory = outputDirectory
        return this
    }

    fun setTimer(timer: CameraTimer): CameraUtil {
        this.mSelectedTimer = timer
        return this
    }

    fun setCameraSelector(cameraSelector: CameraSelector): CameraUtil {
        this.mLensFacing = cameraSelector
        return this
    }

    fun registerDisplayManager() {
        displayManager.registerDisplayListener(displayListener, null)
        mSensorRotationListener?.let {
            if (it.canDetectOrientation()) {
                it.enable()
            }
        }
    }

    fun unregisterDisplayManager() {
        displayManager.unregisterDisplayListener(displayListener)
        mSensorRotationListener?.disable()
    }

    fun getFlashMode() = mFlashMode

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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // The display information
            val metrics = DisplayMetrics().also { viewFinder?.display?.getRealMetrics(it) }
            // The ratio for the output image and preview
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            // The display rotation
            val rotation = viewFinder?.display?.rotation ?: Surface.ROTATION_0

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
                    lifecycleOwner!!, // current lifecycle owner
                    mLensFacing, // either front or back facing
                    preview, // camera preview use case
                    imageCapture, // image capture use case
                    imageAnalyzer, // image analyzer use case
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder?.surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind use cases", e)
            }

        }, ContextCompat.getMainExecutor(activity))
    }

    @Suppress("NON_EXHAUSTIVE_WHEN")
    fun takePicture(result: (Uri) -> Unit, timer: ((Int) -> Unit)? = null) =
        coroutineScope?.launch(Dispatchers.Main) {
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
        File(outputDirectory).mkdirs()
        val file = File(outputDirectory, "${System.currentTimeMillis()}.jpg")


        val outputOptions = OutputFileOptions.Builder(file).setMetadata(metadata).build()

        val executor = Executors.newSingleThreadExecutor()
        localImageCapture.takePicture(
            outputOptions, // the options needed for the final image
            executor, // the executor, on which the task will run
            object : OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: OutputFileResults) {

                    val uri = Uri.fromFile(file)
                    uri?.let { uri ->
                        activity.runOnUiThread {
                            result(uri)
                            Log.d(TAG, "Photo saved in $uri")
                        }
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