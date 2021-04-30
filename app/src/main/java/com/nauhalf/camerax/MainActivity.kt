package com.nauhalf.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import id.dipay.camerax.CameraUtil
import id.dipay.camerax.Selector
import id.dipay.utils.CameraTimer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

open class MainActivity : AppCompatActivity() {

    private val vm by viewModels<MainViewModel>()

    private val cameraUtil: CameraUtil by lazy {
        CameraUtil(this)
            .setLifecycleOwner(this)
            .setCoroutineScope(this.lifecycleScope)
            .setPreviewView(viewFinder)
            .setOutputDirectory(outputDirectory())
            .setTimer(CameraTimer.S3)
            .setImageQuality(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
    }

    private fun outputDirectory(): String {
        val mediaDir = this.externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir.absolutePath else filesDir.absolutePath
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            try {
                cameraUtil.startCamera()
            } catch (exception: IllegalStateException) {
                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
            } catch (exception: Exception) {
                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraUtil.registerDisplayManager()
        viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) {
                cameraUtil.registerDisplayManager()
            }

            override fun onViewDetachedFromWindow(v: View?) {
                cameraUtil.unregisterDisplayManager()
            }

        })

        camera_capture_button.setOnClickListener {
            cameraUtil.takePicture({
                val msg = "Photo capture succeeded: $it"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(CameraUtil.TAG, msg)
            }, {
                vm.setTimer(it)
            })
        }

        btnTorch.setOnClickListener {
            cameraUtil.flash(
                if (cameraUtil.getFlashMode() == ImageCapture.FLASH_MODE_OFF) {
                    Toast.makeText(this, "FLASH_MODE_ON", Toast.LENGTH_SHORT).show()
                    ImageCapture.FLASH_MODE_ON
                } else {
                    Toast.makeText(this, "FLASH_MODE_OFF", Toast.LENGTH_SHORT).show()
                    ImageCapture.FLASH_MODE_OFF
                }
            )
        }

        btnMirror.setOnClickListener {
            cameraUtil.flip { selector ->
                vm.setCameraSelector(selector)
            }
        }

        observeLiveData()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraUtil.unbind()
        cameraUtil.unregisterDisplayManager()
    }

    private fun observeLiveData() {
        vm.cameraSelector.observe(this) {
            when (it) {
                Selector.FRONT -> {
                    identityFrame.visibility = View.GONE
                    personFrame.visibility = View.VISIBLE
                }

                Selector.BACK -> {
                    identityFrame.visibility = View.VISIBLE
                    personFrame.visibility = View.GONE
                }

                else -> {
                    identityFrame.visibility = View.VISIBLE
                    personFrame.visibility = View.GONE
                }
            }

        }

        vm.timer.observe(this) {
            tvTimer.visibility = if (it > 0) View.VISIBLE else View.GONE
            if (it > 0) {
                tvTimer.text = "Timer : $it"
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraUtil.startCamera()
            } else {
                Toast.makeText(this, "Permission not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}