package id.dipay.utils

import java.io.File

object FileCreator {
    const val JPEG_FORMAT = ".jpg"

    fun createTempFile(fileFormat: String) = File.createTempFile(System.currentTimeMillis().toString(), fileFormat)
}