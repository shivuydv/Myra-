package com.example

import android.content.Context
import kotlin.math.abs

data class MyraSafeBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

class MyraSafeGestureBoundary(private val context: Context) {
    fun getSafeBounds(): MyraSafeBounds {
        val g = MyraScreenGeometryEngine(context).getGeometry()
        return MyraSafeBounds(g.width*.03f, g.height*.04f, g.width*.97f, g.height*.96f)
    }
    fun clamp(x: Float, y: Float): Pair<Float,Float> {
        val b = getSafeBounds()
        return x.coerceIn(b.left,b.right) to y.coerceIn(b.top,b.bottom)
    }
    fun isValidGesture(sx: Float, sy: Float, ex: Float, ey: Float): Boolean {
        val b = getSafeBounds()
        fun inside(x: Float,y: Float) = x in b.left..b.right && y in b.top..b.bottom
        return inside(sx,sy) && inside(ex,ey) && (abs(ex-sx)>1f || abs(ey-sy)>1f)
    }
}
