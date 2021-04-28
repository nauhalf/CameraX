package com.nauhalf.camerax.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class HeadPersonView : View {
    var bm: Bitmap? = null
    var cv: Canvas? = null
    lateinit var eraser: Paint

    constructor(context: Context?) : super(context) {
        Init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        Init()
    }

    constructor(
        context: Context?, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        Init()
    }

    private fun Init() {
        eraser = Paint()
        eraser.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        eraser.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cv = Canvas(bm!!)
        }
        super.onSizeChanged(w, h, oldw, oldh)
    }

    private fun drawHead() {
        val w = width
        val h = height
        val top = h / 8f
        val bottom = h / 2f + top
        val left = w / 6f
        val right = w - left

        cv?.drawOval(left, top, right, bottom, eraser)

    }

    private fun drawEar() {
        val w = width
        val h = height
        val topEar = h / 2 - h / 5
        val bottomEar = h / 2 - h / 12
        val leftEar = w / 8f
        val rightEar = w - leftEar
        val radiusEar = 50f

        cv?.drawRoundRect(
            leftEar,
            topEar.toFloat(),
            rightEar,
            bottomEar.toFloat(),
            radiusEar,
            radiusEar,
            eraser
        )

    }

    private fun drawCard() {
        val w = width
        val h = height
        val left = w / 8f
        val right = w - left
        val top = h / 2f + h / 5f
        val bottom = h/4 + top
        val radius = 35f
        cv?.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            eraser
        )
    }

    override fun onDraw(canvas: Canvas) {
        bm?.let { bm ->
            bm.eraseColor(Color.TRANSPARENT)
            cv?.drawColor(Color.parseColor("#80000000"))

            drawHead()
            drawEar()
            drawCard()
            canvas.drawBitmap(bm, 0f, 0f, null)
        }
        super.onDraw(canvas)
    }
}