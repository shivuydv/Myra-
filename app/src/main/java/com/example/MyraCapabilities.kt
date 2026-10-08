package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import java.io.File

// ============================================================
// CORE MODELS & CAPABILITY INTERFACES
// ============================================================

enum class MyraActionStatus {
    SUCCESS, FAILED, RETRY, WAITING, NEED_CONFIRMATION
}

data class MyraActionResult(
    val status: MyraActionStatus,
    val message: String,
    val retryCount: Int = 0
) {
    val success: Boolean
        get() = status == MyraActionStatus.SUCCESS
}

data class MyraAction(
    val type: String,
    val target: String = "",
    val value: String = "",
    val requiresConfirmation: Boolean = false,
    val expectedElement: String = "",
    val expectedPackage: String = ""
)

interface MyraCapability {
    val id: String
    fun supports(actionType: String): Boolean
    fun isAvailable(): Boolean = true
    fun unavailableReason(): String = ""
    suspend fun execute(action: MyraAction): MyraActionResult
}

data class MyraCapabilityStatus(
    val id: String,
    val available: Boolean,
    val reason: String = ""
)

class MyraCapabilityManager {
    private val capabilities = mutableListOf<MyraCapability>()

    fun register(capability: MyraCapability) {
        capabilities.removeAll { it.id == capability.id }
        capabilities.add(capability)
    }

    fun find(actionType: String): MyraCapability? {
        return capabilities.firstOrNull { it.supports(actionType) }
    }

    suspend fun execute(action: MyraAction): MyraActionResult {
        val capability = find(action.type)
            ?: return MyraActionResult(
                MyraActionStatus.FAILED,
                "इस action की capability नहीं मिली: ${action.type}"
            )

        if (!capability.isAvailable()) {
            return MyraActionResult(
                MyraActionStatus.FAILED,
                capability.unavailableReason()
            )
        }

        return capability.execute(action)
    }

    fun status(): List<MyraCapabilityStatus> {
        return capabilities.map {
            MyraCapabilityStatus(
                it.id,
                it.isAvailable(),
                it.unavailableReason()
            )
        }
    }
}

// ============================================================
// CONTROLLERS
// ============================================================

class MyraActionRouter(private val context: Context) {
    fun openApp(name: String) {
        val packageName = when (name.lowercase().trim()) {
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "chrome" -> "com.android.chrome"
            "browser" -> "com.android.chrome"
            "camera" -> "com.android.camera"
            "settings" -> "com.android.settings"
            else -> name
        }

        val intent = context.packageManager
            .getLaunchIntentForPackage(packageName)

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            throw IllegalArgumentException("App नहीं मिला: $name")
        }
    }
}

class MyraCommunication(private val context: Context) {
    fun call(phone: String) {
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:$phone")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun sms(phone: String, message: String) {
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:$phone")
        )
        intent.putExtra("sms_body", message)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun whatsapp(phone: String, message: String) {
        val uri = Uri.parse(
            "https://wa.me/${phone.replace("+", "")}?text=" +
                Uri.encode(message)
        )
        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

data class MyraContactMatch(
    val name: String,
    val phone: String
)

class MyraContactResolver(private val context: Context) {
    fun resolveAll(query: String): List<MyraContactMatch> {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val result = mutableListOf<MyraContactMatch>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                if (nameIndex >= 0 && phoneIndex >= 0) {
                    result.add(
                        MyraContactMatch(
                            it.getString(nameIndex),
                            it.getString(phoneIndex)
                        )
                    )
                }
            }
        }

        return result
    }

    fun resolve(query: String): MyraContactMatch? =
        resolveAll(query).singleOrNull()
}

class MyraDeviceController(private val context: Context) {
    fun volumeUp() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun volumeDown() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }
}

class MyraBrowserController(private val context: Context) {
    fun searchGoogle(query: String) {
        val url = "https://www.google.com/search?q=" + Uri.encode(query)
        openUrl(url)
    }

    fun openUrl(url: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

class MyraUtilityController(private val context: Context) {
    fun toggleFlashlight() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            if (hasFlash) {
                cameraManager.setTorchMode(cameraId, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun batteryLevel(): Int {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100
    }

    fun startTimer(duration: String) {
        val seconds = duration.filter { it.isDigit() }.toIntOrNull() ?: 60
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun startAlarm(time: String) {
        val parts = time.split(":")
        val hour = parts.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

class MyraNavigationController(private val context: Context) {
    fun navigateTo(destination: String) {
        val gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination))
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapIntent)
    }
}

class MyraFileController(private val context: Context) {
    fun readFile(fileName: String): String {
        return context.openFileInput(fileName).bufferedReader().use { it.readText() }
    }

    fun writeFile(fileName: String, content: String) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray())
        }
    }

    fun appendFile(fileName: String, content: String) {
        context.openFileOutput(fileName, Context.MODE_APPEND).use {
            it.write(content.toByteArray())
        }
    }

    fun searchFiles(query: String): List<String> {
        return listFiles("").filter { it.contains(query, ignoreCase = true) }
    }

    fun listFiles(dir: String): List<String> {
        return context.fileList().toList()
    }

    fun shareFile(fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, file.readText())
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
        }
    }

    fun deleteFile(fileName: String) {
        context.deleteFile(fileName)
    }
}

class MyraMediaController(private val context: Context) {
    private fun send(keyCode: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun play() = send(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun pause() = send(KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun next() = send(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = send(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    fun stop() = send(KeyEvent.KEYCODE_MEDIA_STOP)
}

// ============================================================
// CAPABILITY IMPLEMENTATIONS
// ============================================================

class MyraBrowserCapability(private val context: Context) : MyraCapability {
    override val id = "browser"
    override fun supports(actionType: String) = actionType in setOf("WEB_SEARCH", "OPEN_URL")
    override suspend fun execute(action: MyraAction): MyraActionResult {
        val browser = MyraBrowserController(context)
        return try {
            when (action.type) {
                "WEB_SEARCH" -> {
                    if (action.value.isBlank()) {
                        MyraActionResult(MyraActionStatus.FAILED, "Search query खाली है.")
                    } else {
                        browser.searchGoogle(action.value)
                        MyraActionResult(MyraActionStatus.SUCCESS, "Google search खोल दिया.")
                    }
                }
                "OPEN_URL" -> {
                    if (action.value.isBlank()) {
                        MyraActionResult(MyraActionStatus.FAILED, "URL खाली है.")
                    } else {
                        browser.openUrl(action.value)
                        MyraActionResult(MyraActionStatus.SUCCESS, "Website खोल दी.")
                    }
                }
                else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown browser action.")
            }
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "Browser action fail हुई: ${e.message}")
        }
    }
}

class MyraFileCapability(private val context: Context) : MyraCapability {
    override val id = "files"
    override fun supports(actionType: String) =
        actionType in setOf("READ_FILE", "WRITE_FILE", "APPEND_FILE", "SEARCH_FILE", "LIST_FILES", "SHARE_FILE", "DELETE_FILE")

    override suspend fun execute(action: MyraAction): MyraActionResult {
        val files = MyraFileController(context)
        return try {
            when (action.type) {
                "READ_FILE" -> MyraActionResult(MyraActionStatus.SUCCESS, files.readFile(action.target))
                "WRITE_FILE" -> {
                    files.writeFile(action.target, action.value)
                    MyraActionResult(MyraActionStatus.SUCCESS, "File लिख दी.")
                }
                "APPEND_FILE" -> {
                    files.appendFile(action.target, action.value)
                    MyraActionResult(MyraActionStatus.SUCCESS, "File में text जोड़ दिया.")
                }
                "SEARCH_FILE" -> MyraActionResult(MyraActionStatus.SUCCESS, files.searchFiles(action.target).joinToString("\n"))
                "LIST_FILES" -> MyraActionResult(MyraActionStatus.SUCCESS, files.listFiles(action.target).joinToString("\n"))
                "SHARE_FILE" -> {
                    files.shareFile(action.target)
                    MyraActionResult(MyraActionStatus.SUCCESS, "File share करने के लिए खोल दी.")
                }
                "DELETE_FILE" -> {
                    files.deleteFile(action.target)
                    MyraActionResult(MyraActionStatus.SUCCESS, "File delete कर दी.")
                }
                else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown file action.")
            }
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "File operation fail हुई: ${e.message}")
        }
    }
}

class MyraNavigationCapability(private val context: Context) : MyraCapability {
    override val id = "navigation"
    override fun supports(actionType: String) = actionType == "NAVIGATION"
    override suspend fun execute(action: MyraAction): MyraActionResult {
        val destination = action.value.ifBlank { action.target }
        if (destination.isBlank()) {
            return MyraActionResult(MyraActionStatus.FAILED, "Destination नहीं मिला.")
        }
        return try {
            MyraNavigationController(context).navigateTo(destination)
            MyraActionResult(MyraActionStatus.SUCCESS, "$destination की navigation खोल दी.")
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "Navigation शुरू नहीं हो सकी: ${e.message}")
        }
    }
}

class MyraScreenAutomationCapability : MyraCapability {
    override val id = "screen_automation"
    override fun supports(actionType: String) =
        actionType in setOf("CLICK", "TAP", "LONG_PRESS", "TYPE", "SWIPE", "BACK")

    override fun isAvailable() = MyraAccessibilityService.instance != null
    override fun unavailableReason() = "MYRA Accessibility permission enable करो."

    override suspend fun execute(action: MyraAction): MyraActionResult {
        return when (action.type) {
            "CLICK", "TAP" -> {
                val ok = MyraScreenController.click(action.target)
                if (ok) {
                    MyraActionResult(MyraActionStatus.SUCCESS, "${action.target} पर click किया.")
                } else {
                    MyraActionResult(MyraActionStatus.FAILED, "${action.target} नहीं मिला.")
                }
            }
            "LONG_PRESS" -> {
                val ok = MyraScreenController.longPress(action.target)
                if (ok) {
                    MyraActionResult(MyraActionStatus.SUCCESS, "Long press किया.")
                } else {
                    MyraActionResult(MyraActionStatus.FAILED, "${action.target} नहीं मिला.")
                }
            }
            "TYPE" -> {
                val ok = MyraScreenController.type(action.value)
                if (ok) {
                    MyraActionResult(MyraActionStatus.SUCCESS, "Text type कर दिया.")
                } else {
                    MyraActionResult(MyraActionStatus.FAILED, "Text field नहीं मिला.")
                }
            }
            "SWIPE" -> {
                val service = MyraAccessibilityService.instance
                    ?: return MyraActionResult(MyraActionStatus.FAILED, "Accessibility service उपलब्ध नहीं है.")
                val ok = when (action.value.lowercase()) {
                    "up" -> service.swipe(500f, 1600f, 500f, 500f)
                    "down" -> service.swipe(500f, 500f, 500f, 1600f)
                    else -> false
                }
                if (ok) {
                    MyraActionResult(MyraActionStatus.SUCCESS, "Swipe किया.")
                } else {
                    MyraActionResult(MyraActionStatus.FAILED, "Swipe fail हुआ.")
                }
            }
            "BACK" -> {
                val ok = MyraAccessibilityService.instance?.performGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_BACK
                ) == true
                if (ok) {
                    MyraActionResult(MyraActionStatus.SUCCESS, "Back किया.")
                } else {
                    MyraActionResult(MyraActionStatus.FAILED, "Back action fail हुआ.")
                }
            }
            else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown screen action.")
        }
    }
}

class MyraAppCapability(private val context: Context) : MyraCapability {
    override val id = "apps"
    override fun supports(actionType: String) =
        actionType in setOf("OPEN_APP", "SWITCH_APP", "HOME", "SETTINGS", "CAMERA")

    override suspend fun execute(action: MyraAction): MyraActionResult {
        val launcher = MyraActionRouter(context)
        return try {
            when (action.type) {
                "OPEN_APP", "SWITCH_APP" -> {
                    launcher.openApp(action.target)
                    MyraActionResult(MyraActionStatus.SUCCESS, "${action.target} खोल दिया.")
                }
                "HOME" -> {
                    val ok = MyraAccessibilityService.instance?.performGlobalAction(
                        AccessibilityService.GLOBAL_ACTION_HOME
                    ) == true
                    if (ok) {
                        MyraActionResult(MyraActionStatus.SUCCESS, "Home पर आ गया.")
                    } else {
                        MyraActionResult(MyraActionStatus.FAILED, "Home action fail हुआ.")
                    }
                }
                "SETTINGS" -> {
                    launcher.openApp("settings")
                    MyraActionResult(MyraActionStatus.SUCCESS, "Settings खोल दिया.")
                }
                "CAMERA" -> {
                    launcher.openApp("camera")
                    MyraActionResult(MyraActionStatus.SUCCESS, "Camera खोल दिया.")
                }
                else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown app action.")
            }
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "App launch fail हुआ: ${e.message}")
        }
    }
}

class MyraSystemCapability(private val context: Context) : MyraCapability {
    override val id = "system"
    override fun supports(actionType: String) =
        actionType in setOf("HOME", "BACK", "LOCK_DEVICE", "CLIPBOARD_COPY", "CLIPBOARD_GET", "CLIPBOARD_CLEAR", "DEVICE_INFO")

    override suspend fun execute(action: MyraAction): MyraActionResult {
        val service = MyraAccessibilityService.instance
        return when (action.type) {
            "HOME" -> {
                val ok = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) == true
                MyraActionResult(
                    if (ok) MyraActionStatus.SUCCESS else MyraActionStatus.FAILED,
                    if (ok) "Home पर आ गया." else "Home action fail हुआ."
                )
            }
            "BACK" -> {
                val ok = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) == true
                MyraActionResult(
                    if (ok) MyraActionStatus.SUCCESS else MyraActionStatus.FAILED,
                    if (ok) "Back किया." else "Back action fail हुआ."
                )
            }
            "LOCK_DEVICE" -> {
                val ok = service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
                MyraActionResult(
                    if (ok) MyraActionStatus.SUCCESS else MyraActionStatus.FAILED,
                    if (ok) "Device lock कर दिया." else "Device lock नहीं हो सका."
                )
            }
            "CLIPBOARD_COPY" -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("MYRA", action.value))
                MyraActionResult(MyraActionStatus.SUCCESS, "Clipboard में copy कर दिया.")
            }
            "CLIPBOARD_GET" -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                MyraActionResult(MyraActionStatus.SUCCESS, text)
            }
            "CLIPBOARD_CLEAR" -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.clearPrimaryClip()
                MyraActionResult(MyraActionStatus.SUCCESS, "Clipboard clear कर दिया.")
            }
            "DEVICE_INFO" -> {
                val info = "Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT}, Device ${Build.MODEL}"
                MyraActionResult(MyraActionStatus.SUCCESS, info)
            }
            else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown system action.")
        }
    }
}

class MyraUtilityCapability(private val context: Context) : MyraCapability {
    override val id = "utility"
    override fun supports(actionType: String) = actionType in setOf("FLASHLIGHT", "BATTERY", "TIMER", "ALARM")
    override suspend fun execute(action: MyraAction): MyraActionResult {
        val utility = MyraUtilityController(context)
        return try {
            when (action.type) {
                "FLASHLIGHT" -> {
                    utility.toggleFlashlight()
                    MyraActionResult(MyraActionStatus.SUCCESS, "Flashlight toggle कर दिया.")
                }
                "BATTERY" -> {
                    val level = utility.batteryLevel()
                    MyraActionResult(MyraActionStatus.SUCCESS, "Battery ${level}% है.")
                }
                "TIMER" -> {
                    utility.startTimer(action.value)
                    MyraActionResult(MyraActionStatus.SUCCESS, "Timer लगा दिया.")
                }
                "ALARM" -> {
                    utility.startAlarm(action.value)
                    MyraActionResult(MyraActionStatus.SUCCESS, "Alarm set कर दिया.")
                }
                else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown utility action.")
            }
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "Utility action fail हुई: ${e.message}")
        }
    }
}

class MyraMediaCapability(private val context: Context) : MyraCapability {
    override val id = "media"
    override fun supports(actionType: String) =
        actionType in setOf("MEDIA_PLAY", "MEDIA_PAUSE", "MEDIA_NEXT", "MEDIA_PREVIOUS", "MEDIA_STOP")

    override suspend fun execute(action: MyraAction): MyraActionResult {
        val media = MyraMediaController(context)
        when (action.type) {
            "MEDIA_PLAY" -> media.play()
            "MEDIA_PAUSE" -> media.pause()
            "MEDIA_NEXT" -> media.next()
            "MEDIA_PREVIOUS" -> media.previous()
            "MEDIA_STOP" -> media.stop()
            else -> return MyraActionResult(MyraActionStatus.FAILED, "Unknown media action.")
        }
        return MyraActionResult(MyraActionStatus.SUCCESS, "Media action complete.")
    }
}

class MyraCommunicationCapability(private val context: Context) : MyraCapability {
    private val resolver = MyraContactResolver(context)
    override val id = "communication"
    override fun supports(actionType: String) = actionType in setOf("CALL", "SMS", "WHATSAPP")

    override suspend fun execute(action: MyraAction): MyraActionResult {
        if (action.target.isBlank()) {
            return MyraActionResult(MyraActionStatus.FAILED, "Contact name नहीं मिला.")
        }

        val matches = resolver.resolveAll(action.target)
        if (matches.isEmpty()) {
            return MyraActionResult(MyraActionStatus.FAILED, "${action.target} contact नहीं मिला.")
        }

        if (matches.size > 1) {
            val list = matches.mapIndexed { i, c -> "${i + 1}. ${c.name} - ${c.phone}" }.joinToString("\n")
            return MyraActionResult(MyraActionStatus.WAITING, list)
        }

        val contact = matches.first()
        val communication = MyraCommunication(context)

        return when (action.type) {
            "CALL" -> {
                communication.call(contact.phone)
                MyraActionResult(MyraActionStatus.SUCCESS, "${contact.name} को call करने के लिए खोल दिया.")
            }
            "SMS" -> {
                if (action.value.isBlank()) {
                    return MyraActionResult(MyraActionStatus.FAILED, "SMS message खाली है.")
                }
                communication.sms(contact.phone, action.value)
                MyraActionResult(MyraActionStatus.SUCCESS, "${contact.name} के लिए SMS खोल दिया.")
            }
            "WHATSAPP" -> {
                if (action.value.isBlank()) {
                    return MyraActionResult(MyraActionStatus.FAILED, "WhatsApp message खाली है.")
                }
                communication.whatsapp(contact.phone, action.value)
                MyraActionResult(MyraActionStatus.SUCCESS, "${contact.name} के लिए WhatsApp खोल दिया.")
            }
            else -> MyraActionResult(MyraActionStatus.FAILED, "Unknown communication action.")
        }
    }
}
