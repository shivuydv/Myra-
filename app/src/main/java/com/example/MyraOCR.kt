package com.example

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class MyraOCRResult(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class MyraOCR {

    suspend fun scan(
        bitmap: Bitmap
    ): List<MyraOCRResult> {

        val image = InputImage.fromBitmap(bitmap, 0)

        val recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

        val result = suspendCancellableCoroutine<Text> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    cont.resume(text)
                }
                .addOnFailureListener { exception ->
                    cont.resumeWithException(exception)
                }
        }

        return result.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                MyraOCRResult(
                    line.text,
                    box.left.toFloat(),
                    box.top.toFloat(),
                    box.right.toFloat(),
                    box.bottom.toFloat()
                )
            }
        }
    }
}

data class MyraVisualMatch(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class MyraVisualFinder {
    fun find(
        items: List<MyraOCRResult>,
        target: String
    ): MyraVisualMatch? {

        items.firstOrNull {
            it.text.equals(target, ignoreCase = true)
        }?.let {
            return MyraVisualMatch(
                it.text, it.left, it.top, it.right, it.bottom
            )
        }

        return items.firstOrNull {
            it.text.contains(target, ignoreCase = true)
        }?.let {
            MyraVisualMatch(
                it.text, it.left, it.top, it.right, it.bottom
            )
        }
    }
}
