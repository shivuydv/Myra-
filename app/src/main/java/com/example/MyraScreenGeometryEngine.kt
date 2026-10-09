package com.example

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

data class MyraScreenGeometry(val width: Int, val height: Int, val density: Float)

class MyraScreenGeometryEngine(private val context: Context) {
    @Suppress("DEPRECATION")
    fun getGeometry(): MyraScreenGeometry {
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)
        return MyraScreenGeometry(metrics.widthPixels, metrics.heightPixels, metrics.density)
    }
    fun center(): Pair<Float,Float> {
        val g = getGeometry(); return g.width/2f to g.height/2f
    }
    fun clampX(x: Float) = x.coerceIn(0f, getGeometry().width.toFloat())
    fun clampY(y: Float) = y.coerceIn(0f, getGeometry().height.toFloat())
    fun clampPoint(x: Float, y: Float) = clampX(x) to clampY(y)
}
