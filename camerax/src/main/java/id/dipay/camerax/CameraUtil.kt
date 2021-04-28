package id.dipay.camerax

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** reference
 * https://medium.com/androiddevelopers/camerax-learn-how-to-use-cameracontroller-e3ed10fffecf
 * */

class CameraUtil(
    private val activity: AppCompatActivity,
    private val preview: PreviewView,
    private val outputFile: File
) {

    companion object {
        const val TAG = "CameraXBasic"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        fun getOutputDirectory(activity: AppCompatActivity): File {
            val mediaDir = activity.externalMediaDirs.firstOrNull()?.let {
                File(it, activity.applicationInfo.loadLabel(activity.packageManager).toString()).apply {
                    mkdirs()
                }
            }

            return if (mediaDir != null && mediaDir.exists()) mediaDir else activity.filesDir
        }
    }

    private var controller: CameraController? = null

    init {
        controller = LifecycleCameraController(activity.baseContext)
    }

    fun startCamera(selector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {

        (controller as LifecycleCameraController).apply {
            unbind()
            bindToLifecycle(activity)
        }
        controller?.cameraSelector = selector
        preview.controller = controller
    }

    fun takePhoto(result: (Uri) -> Unit) {
        val photoFile = File(
            outputFile, SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        controller?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity.baseContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = Uri.fromFile(photoFile)
                    result(saveUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo captured failed: ${exception.message}", exception)
                }

            })
    }


    fun toggleTorch() {
        controller?.apply {
            val isEnable = torchState.value == 1
            enableTorch(!isEnable)
        }
    }

    fun flipCamera(currentCameraSelector: (CAMERA_SELECTOR) -> Unit) {
        controller?.apply {
            when (cameraSelector) {
                CameraSelector.DEFAULT_BACK_CAMERA -> {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    currentCameraSelector(CAMERA_SELECTOR.FRONT)
                }
                CameraSelector.DEFAULT_FRONT_CAMERA -> {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    currentCameraSelector(CAMERA_SELECTOR.BACK)

                }
            }
        }
    }

    enum class CAMERA_SELECTOR {
        FRONT,
        BACK
    }

}