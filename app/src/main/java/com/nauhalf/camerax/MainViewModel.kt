package com.nauhalf.camerax

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nauhalf.camerax.utils.CameraUtil

class MainViewModel : ViewModel() {
    private val _cameraSelector = MutableLiveData(CameraUtil.CAMERA_SELECTOR.BACK)
    val cameraSelector: LiveData<CameraUtil.CAMERA_SELECTOR>
        get() = _cameraSelector

    fun setCameraSelector(value: CameraUtil.CAMERA_SELECTOR) {
        _cameraSelector.value = value
    }

}