package com.nauhalf.camerax.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nauhalf.camerax.R

/**
 *  let Kotlin handle the constructors by using default values thanks to @JvmOverloads
 *  but it is danger
 *  https://blog.q42.nl/the-danger-of-assumptions-kotlin-with-android-custom-views-adb79bf2da45/
 * */
class CameraIdentityFrame : View {

    constructor(context: Context) : super(context) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(
        context,
        attrs,
        attributeSetId
    ) {
        initPaints()
    }

    private var mTransparentPaint: Paint? = null
    private var mSemiBlackPaint: Paint? = null
    private val mPath = Path()

    private fun initPaints() {
        mTransparentPaint = Paint().also {
            it.color = Color.TRANSPARENT
            it.strokeWidth = 0f
        }
        mSemiBlackPaint = Paint().also {
            it.color = ContextCompat.getColor(context, R.color.transparent)
            it.strokeWidth = -1f
            it.style = Paint.Style.FILL_AND_STROKE
            it.strokeCap = Paint.Cap.ROUND
            it.isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPath.reset()
        val frameW = (width - dpToPx(30)) / 2
        val height = frameW / 1.5
        val frameH = height.toInt()
        val topMargin = dpToPx(13)
        val canvasHalfWidth = width / 2
        val doubleHeight = (getHeight() / 2).toDouble()
        val canvasHalfHeight = doubleHeight.toInt()
        val roundedCorner = 25
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPath.addRoundRect(
                (canvasHalfWidth - frameW).toFloat(),
                (
                        canvasHalfHeight - frameH + topMargin).toFloat(),
                (
                        canvasHalfWidth + frameW - 1).toFloat(),
                (
                        canvasHalfHeight + frameH + topMargin - 1).toFloat(),
                roundedCorner.toFloat(),
                roundedCorner.toFloat(),
                Path.Direction.CW
            )
        } else {
            mPath.addRect(
                (canvasHalfWidth - frameW).toFloat(), (
                        canvasHalfHeight - frameH + topMargin).toFloat(), (
                        canvasHalfWidth + frameW).toFloat(), (
                        canvasHalfHeight + frameH + topMargin).toFloat(), Path.Direction.CW
            )
        }
        mPath.fillType = Path.FillType.INVERSE_EVEN_ODD
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(
                (canvasHalfWidth - frameW).toFloat(),
                (
                        canvasHalfHeight - frameH + topMargin).toFloat(),
                (
                        canvasHalfWidth + frameW - 1).toFloat(),
                (
                        canvasHalfHeight + frameH + topMargin - 1).toFloat(),
                roundedCorner.toFloat(),
                roundedCorner.toFloat(),
                mTransparentPaint!!
            )
        } else {
            canvas.drawRect(
                (canvasHalfWidth - frameW).toFloat(), (
                        canvasHalfHeight - frameH + topMargin).toFloat(), (
                        canvasHalfWidth + frameW).toFloat(), (
                        canvasHalfHeight + frameH + topMargin).toFloat(), mTransparentPaint!!
            )
        }
        canvas.drawPath(mPath, mSemiBlackPaint!!)
        canvas.clipPath(mPath)
        canvas.drawColor(Color.parseColor("#80000000"))
    }


    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }
}