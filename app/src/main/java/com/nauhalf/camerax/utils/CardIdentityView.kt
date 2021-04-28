package com.nauhalf.camerax.utils

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View

/**
 *  let Kotlin handle the constructors by using default values thanks to @JvmOverloads
 *  but it is danger
 *  https://blog.q42.nl/the-danger-of-assumptions-kotlin-with-android-custom-views-adb79bf2da45/
 * */
class CardIdentityView : View {

    private lateinit var paint: Paint
    private lateinit var bm: Bitmap
    private lateinit var cv: Canvas

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

    private fun initPaints() {
        paint = Paint().also {
            it.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            it.isAntiAlias = true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cv = Canvas(bm)
        }
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        bm.eraseColor(Color.TRANSPARENT)
        cv.drawColor(Color.parseColor("#80000000"))

        drawCard()

        canvas.drawBitmap(bm, 0f, 0f, null)

        super.onDraw(canvas)
    }

    private fun drawCard() {
        val w = width
        val h = height
        val left = w / 25f
        val right = w - left
        val top = h / 2f - h / 5.5f
        val bottom = h / 2f + h / 5.5f
        val radius = 35f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cv.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    radius,
                    radius,
                    paint
            )
        } else {
            cv.drawRect(
                    left,
                    top,
                    right,
                    bottom,
                    paint
            )
        }
    }
}