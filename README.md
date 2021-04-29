# Custom CameraX

  Custom CameraX with Preview Frame KTP and Head&KTP

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
    CameraUtil(context, lifecycleOwner, previewView, outputFile)
}

// Start Camera
cameraUtil.startCamera()

// Take Photo
cameraUtil.takePhoto { uri ->
    val msg = "Photo capture succeeded: $uri"
    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
    Log.d(CameraUtil.TAG, msg)
}

// Toggle Torch
cameraUtil.toggleTorch()

// Camera Selector
cameraUtil.flipCamera { selector ->
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
```
