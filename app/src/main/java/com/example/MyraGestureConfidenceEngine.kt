package com.example

data class MyraGestureConfidence(val score: Float, val reliable: Boolean, val reason: String)

class MyraGestureConfidenceEngine {
    fun calculate(
        packageChanged: Boolean, classChanged: Boolean, visualChanged: Boolean,
        visualScore: Float, ocrChanged: Boolean
    ): MyraGestureConfidence {
        var score = 0f
        if (packageChanged) score += .35f
        if (classChanged) score += .20f
        if (visualChanged) score += .25f
        if (ocrChanged) score += .15f
        score += when { visualScore >= .5f -> .15f; visualScore >= .2f -> .08f; else -> 0f }
        score = score.coerceIn(0f,1f)
        return MyraGestureConfidence(score, score >= .55f,
            if (score >= .55f) "Reliable" else "More verification needed")
    }
}
