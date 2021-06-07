package com.nauhalf.camerax

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import id.dipay.camerax.Selector

class MainViewModel : ViewModel() {

    private val _cameraSelector = MutableLiveData(Selector.BACK)
    val cameraSelector: LiveData<Selector>
        get() = _cameraSelector
    fun setCameraSelector(value: Selector) {
        _cameraSelector.value = value
    }


    private val _timer = MutableLiveData(0)
    val timer: LiveData<Int>
        get() = _timer
    fun setTimer(value: Int){
        _timer.value = value
    }

}