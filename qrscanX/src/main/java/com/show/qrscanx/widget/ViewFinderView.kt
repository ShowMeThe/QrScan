package com.show.qrscanX.widget

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import androidx.core.view.doOnPreDraw
import com.show.qrscanX.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ViewFinderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mHeight = 0
    private var mWidth = 0
    private var rectColor = Color.WHITE
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private var delays = intArrayOf(200, 300, 400, 100, 200, 300, 0, 100, 200)
    private val rects = ArrayList<Rect>()
    private var set: AnimatorSet? = null
    private var duration = 1500
    private var dotCount = 10
    private val interpolator = AccelerateDecelerateInterpolator()
    private val listener = ValueAnimator.AnimatorUpdateListener { invalidate() }
    private val animator = ArrayList<Animator>()
    private val path = Path()
    var isStaring = false

    private val animatorListenerAdapter = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            goAnimator()
        }
    }

    init {
        initAttr(context, attrs)
    }

    private fun initAttr(context: Context, attrs: AttributeSet?) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.ViewFinderView)
        rectColor = array.getColor(R.styleable.ViewFinderView_finderScannerColor, Color.WHITE)
        duration = array.getInteger(R.styleable.ViewFinderView_finderScannerDuration, duration)
        dotCount = array.getInteger(R.styleable.ViewFinderView_finderScannerDot, dotCount)
        paint.color = rectColor
        array.recycle()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = measuredWidth
        mHeight = measuredHeight
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnPreDraw {
            goAnimator()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimator()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (rect in rects) {
            createRect(rect.cx, rect.cy, rect.radius, rectColor, canvas)
        }
    }

    private fun goAnimator() {
        isStaring = true
        if (set != null) {
            set?.cancel()
            set = null
        }
        if (set == null) {
            set = AnimatorSet()
            set?.playTogether(createNextRect())
            set?.start()
        }
    }

    private fun createNextRect(): ArrayList<Animator> {
        rects.clear()
        animator.clear()
        val random = Random(System.currentTimeMillis())
        val animators = ArrayList<Animator>()
        for (i in 0..dotCount) {
            val x = random.nextFloat() * (mWidth)
            val y = random.nextFloat() * (mHeight)
            val rect = Rect(x, y)
            rects.add(rect)
            animators.add(getAnimator(i, rect))
        }
        animators.last().addListener(animatorListenerAdapter)
        animator.addAll(animators)
        return animators
    }

    private fun getAnimator(index: Int, rect: Rect): ObjectAnimator {
        val objectAnim = ObjectAnimator.ofObject(
            rect,
            "radius", FloatEvaluator(), 0.0, 10.0, 0.0
        )
        objectAnim.duration = duration.toLong()
        objectAnim.startDelay = delays[index % (delays.size)].toLong()
        objectAnim.interpolator = interpolator
        objectAnim.addUpdateListener(listener)
        return objectAnim
    }



    private fun createRect(cx: Float, cy: Float, radius: Float, color: Int, canvas: Canvas) {
        canvas.save()

        path.reset()
        path.moveTo(cx, cy - radius)
        path.lineTo(cx - radius, cy)
        path.lineTo(cx, cy + radius)
        path.lineTo(cx + radius, cy)
        canvas.drawPath(path, paint)

        path.reset()
        paint.alpha = (0.4 * 255f).toInt()
        val diRadius = radius * 2.0f
        path.moveTo(cx, cy - diRadius)
        path.lineTo(cx - diRadius, cy)
        path.lineTo(cx, cy + diRadius)
        path.lineTo(cx + diRadius, cy)
        canvas.drawPath(path, paint)

        paint.alpha = 255
        canvas.restore()
    }


    private class Rect(var cx: Float, var cy: Float) {
        var radius = 0.0f
    }

    fun starAnimator(){
        post {
            goAnimator()
        }
    }

    fun stopAnimator(){
        isStaring = false
        rects.clear()
        if(animator.isNotEmpty()){
            animator.last().removeAllListeners()
            animator.forEach {
                it.cancel()
            }
            animator.clear()
        }
        if (set != null) {
            set?.removeAllListeners()
            set?.cancel()
            set = null
        }
        postInvalidate()
    }


}