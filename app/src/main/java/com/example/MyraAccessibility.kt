package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

class MyraAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MyraAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        MyraScreenObserver.onEvent(event)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path, 0, 100
                )
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun longPress(x: Float, y: Float, duration: Long = 700): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path, 0, duration
                )
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long = 500
    ): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path, 0, duration
                )
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun findText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return root.findAccessibilityNodeInfosByText(text)
            .firstOrNull()
    }

    fun clickText(text: String): Boolean {
        val node = findText(text) ?: return false
        var current: AccessibilityNodeInfo? = node

        repeat(5) {
            if (current?.isClickable == true) {
                return current!!.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }
            current = current?.parent
        }

        return false
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(
            AccessibilityNodeInfo.FOCUS_INPUT
        ) ?: return false

        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )
    }
}

object MyraScreenState {
    @Volatile var latestScreenshot: Bitmap? = null
    @Volatile var screenshotTime: Long = 0L
    @Volatile var isCapturing: Boolean = false
    @Volatile var currentPackage: String = ""
    @Volatile var currentClass: String = ""

    fun update(bitmap: Bitmap) {
        latestScreenshot = bitmap
        screenshotTime = System.currentTimeMillis()
    }

    fun hasFreshScreenshot(maxAge: Long = 3000L): Boolean {
        return latestScreenshot != null &&
            System.currentTimeMillis() - screenshotTime <= maxAge
    }

    fun clear() {
        latestScreenshot = null
        screenshotTime = 0L
    }
}

object MyraScreenController {

    fun findElement(text: String): AccessibilityNodeInfo? {
        return MyraAccessibilityService.instance?.findText(text)
    }

    fun click(text: String): Boolean {
        return MyraAccessibilityService.instance?.clickText(text) ?: false
    }

    fun longPress(text: String): Boolean {
        val node = findElement(text) ?: return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return MyraAccessibilityService.instance?.longPress(
            rect.centerX().toFloat(),
            rect.centerY().toFloat()
        ) ?: false
    }

    fun type(text: String): Boolean {
        return MyraAccessibilityService.instance?.typeText(text) ?: false
    }

    fun exists(text: String): Boolean {
        return findElement(text) != null
    }

    suspend fun waitForElement(
        text: String,
        timeout: Long = 4000L
    ): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            if (exists(text)) return true
            delay(250)
        }
        return false
    }
}

object MyraScreenObserver {
    fun onEvent(event: AccessibilityEvent?) {
        if (event == null) return
        MyraScreenState.currentPackage =
            event.packageName?.toString() ?: ""
        MyraScreenState.currentClass =
            event.className?.toString() ?: ""
    }
}

class MyraScreenWaiter {

    suspend fun waitForPackage(
        packageName: String,
        timeout: Long = 5000L
    ): Boolean {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeout) {
            if (
                MyraScreenState.currentPackage
                    .contains(packageName, ignoreCase = true)
            ) return true

            delay(250)
        }

        return false
    }

    suspend fun waitForElement(
        text: String,
        timeout: Long = 5000L
    ): Boolean {
        return MyraScreenController.waitForElement(text, timeout)
    }
}

class MyraScreenInspector {

    fun inspect(): List<String> {
        val root =
            MyraAccessibilityService.instance
                ?.rootInActiveWindow ?: return emptyList()

        val result = mutableListOf<String>()

        fun walk(
            node: AccessibilityNodeInfo?
        ) {
            if (node == null) return

            node.text?.toString()?.takeIf { it.isNotBlank() }
                ?.let { result.add("text=$it") }

            node.contentDescription?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { result.add("description=$it") }

            if (node.childCount > 0) {
                for (i in 0 until node.childCount) {
                    walk(node.getChild(i))
                }
            }
        }

        walk(root)
        return result
    }
}
