# Custom CameraX

  Custom CameraX with Preview Frame Identity Card and Head with Identity Card

## How To Use

### Step 1. Add the JitPack repository to your build.gradle project

```
allprojects {
    repositories {
        ...
        maven { 
            url 'https://jitpack.io'
        }
    }
}
```

### Step 2. Add the dependency in your build.gradle module

```
dependencies {
    ...
    implementation "androidx.camera:camera-view:1.0.0-alpha24"
    implementation 'com.github.nauhalf:Camerax:<LATEST_VERSION>'
}
```

### Step 3. Add permission and uses-feature for using camera

```
<uses-feature android:name="android.hardware.camera.any" />
<uses-permission android:name="android.permission.CAMERA"/>
```

### Step 4. Add below code on your activity

```
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

//set Output Directory
private fun outputDirectory(): String {
    val mediaDir = this.externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply {
            mkdirs()
        }
    }

    return if (mediaDir != null && mediaDir.exists()) mediaDir.absolutePath else filesDir.absolutePath
}

// Start Camera
cameraUtil.startCamera()

// Take Photo
cameraUtil.takePicture({
    val msg = "Photo capture succeeded: $it"
    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    Log.d(CameraUtil.TAG, msg)
}, {
    //callback if use timer
    vm.setTimer(it)
})

// Set flash after camera start
cameraUtil.flash(
    if (cameraUtil.getFlashMode() == ImageCapture.FLASH_MODE_OFF) {
        Toast.makeText(this, "FLASH_MODE_ON", Toast.LENGTH_SHORT).show()
        ImageCapture.FLASH_MODE_ON
    } else {
        Toast.makeText(this, "FLASH_MODE_OFF", Toast.LENGTH_SHORT).show()
        ImageCapture.FLASH_MODE_OFF
    }
)

// Camera Selector
cameraUtil.flip { selector ->
    vm.setCameraSelector(selector)
}


// Don't forget to call unbind & unregisterDisplayManager() in onDestroy() function
override fun onDestroy() {
    super.onDestroy()
    cameraUtil.unbind()
    cameraUtil.unregisterDisplayManager()
}
```

### Custom Frames Identity Card & Head with Identity Card
```
<id.dipay.camerax.HeadPersonView
    android:id="@+id/personFrame"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

<id.dipay.camerax.CardIdentityView
    android:id="@+id/identityFrame"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```
