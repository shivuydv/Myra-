package com.example

import android.content.Context
import kotlinx.coroutines.delay

class MyraCommandNormalizer {
    fun normalize(command: String): String {
        return command
            .replace("खोल दो", "खोलो")
            .replace("खोल दीजिए", "खोलो")
            .replace("वापिस", "वापस")
            .trim()
    }
}

class MyraPlanner {
    fun plan(command: String): MyraActionPlan {
        val text = command.trim()

        val parts = text.split(
            Regex("(?i)\\s+(?:फिर|phir|then|and then)\\s+")
        ).filter { it.isNotBlank() }

        val actions = parts.mapNotNull {
            parsePart(it.trim())
        }

        return MyraActionPlan(
            goal = command,
            actions = actions
        )
    }

    private fun parsePart(text: String): MyraAction? {
        val lower = text.lowercase()

        return when {
            lower.contains("youtube") && lower.contains("खोल") ->
                MyraAction(
                    "OPEN_APP",
                    target = "youtube",
                    expectedPackage = "com.google.android.youtube"
                )

            lower.contains("whatsapp") && lower.contains("खोल") ->
                MyraAction(
                    "OPEN_APP",
                    target = "whatsapp",
                    expectedPackage = "com.whatsapp"
                )

            lower.startsWith("click ") ->
                MyraAction(
                    "CLICK",
                    target = text.substringAfter("click ").trim()
                )

            lower.startsWith("tap ") ->
                MyraAction(
                    "TAP",
                    target = text.substringAfter("tap ").trim()
                )

            lower.contains("वापस") || lower == "back" ->
                MyraAction("BACK")

            lower.contains("swipe up") ||
                lower.contains("ऊपर swipe") ->
                MyraAction("SWIPE", value = "up")

            lower.contains("swipe down") ||
                lower.contains("नीचे swipe") ->
                MyraAction("SWIPE", value = "down")

            lower.startsWith("type ") ->
                MyraAction(
                    "TYPE",
                    value = text.substringAfter("type ").trim()
                )

            else -> null
        }
    }
}

data class MyraActionPlan(
    val goal: String,
    val actions: List<MyraAction>
)

class MyraFallbackEngine {

    suspend fun clickWithFallback(
        target: String
    ): MyraActionResult {

        if (MyraScreenController.click(target)) {
            return MyraActionResult(
                MyraActionStatus.SUCCESS,
                "$target पर click किया."
            )
        }

        if (!MyraScreenState.hasFreshScreenshot()) {
            return MyraActionResult(
                MyraActionStatus.FAILED,
                "$target नहीं मिला और screenshot उपलब्ध नहीं है."
            )
        }

        val bitmap = MyraScreenState.latestScreenshot
            ?: return MyraActionResult(
                MyraActionStatus.FAILED,
                "Screenshot उपलब्ध नहीं है."
            )

        val ocr = MyraOCR()
        val items = ocr.scan(bitmap)
        val match = MyraVisualFinder().find(items, target)

        if (match == null) {
            return MyraActionResult(
                MyraActionStatus.FAILED,
                "$target screen पर नहीं मिला."
            )
        }

        val x = (match.left + match.right) / 2f
        val y = (match.top + match.bottom) / 2f

        val tapped =
            MyraAccessibilityService.instance?.tap(x, y) ?: false

        return if (tapped) {
            MyraActionResult(
                MyraActionStatus.SUCCESS,
                "$target पर OCR fallback से click किया."
            )
        } else {
            MyraActionResult(
                MyraActionStatus.FAILED,
                "$target पर tap नहीं हो सका."
            )
        }
    }
}

data class MyraMemoryItem(
    var action: String = "",
    var target: String = "",
    var value: String = ""
)

class MyraConversationMemory {
    private val items = mutableListOf<MyraMemoryItem>()

    fun save(item: MyraMemoryItem) {
        items.add(item)
        if (items.size > 20) items.removeAt(0)
    }

    fun last(): MyraMemoryItem? = items.lastOrNull()

    fun clear() = items.clear()
}

class MyraContextResolver(
    private val memory: MyraConversationMemory
) {
    fun resolveTarget(text: String): String {
        val pronouns = listOf(
            "उसे", "उसको", "उन्हें",
            "उसी को", "use", "usko",
            "them", "him", "her"
        )

        if (pronouns.any { text.contains(it, true) }) {
            return memory.last()?.target ?: ""
        }

        return ""
    }
}

class MyraSmartRetry(
    private val maxRetries: Int = 2
) {
    suspend fun execute(
        action: MyraAction,
        executor: suspend (MyraAction) -> MyraActionResult,
        fallback: suspend (MyraAction) -> MyraActionResult,
        onProgress: (String) -> Unit = {}
    ): MyraActionResult {

        var result = executor(action)

        if (result.success) return result

        if (action.type == "CLICK" || action.type == "TAP") {
            onProgress("Accessibility fail हुआ, OCR fallback try कर रही हूँ...")
            result = fallback(action)
            if (result.success) return result
        }

        for (attempt in 1..maxRetries) {
            onProgress("Retry $attempt/$maxRetries...")
            delay(500L * attempt)
            result = executor(action)
            if (result.success) return result
        }

        return result.copy(retryCount = maxRetries)
    }
}

data class MyraVerificationResult(
    val success: Boolean,
    val message: String
)

class MyraSmartVerifier {

    suspend fun verify(
        expectedElement: String = "",
        expectedPackage: String = "",
        timeout: Long = 4000L
    ): MyraVerificationResult {

        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeout) {

            if (
                expectedPackage.isNotBlank() &&
                MyraScreenState.currentPackage
                    .contains(expectedPackage, ignoreCase = true)
            ) {
                return MyraVerificationResult(
                    true,
                    "Expected app/screen मिल गया."
                )
            }

            if (
                expectedElement.isNotBlank() &&
                MyraScreenController.findElement(expectedElement) != null
            ) {
                return MyraVerificationResult(
                    true,
                    "$expectedElement मिल गया."
                )
            }

            delay(250)
        }

        return MyraVerificationResult(
            false,
            "Expected screen/element नहीं मिला."
        )
    }
}

class MyraSafetyGate {

    fun requiresConfirmation(action: MyraAction): Boolean {
        if (action.requiresConfirmation) return true

        return action.type in setOf(
            "CALL",
            "SMS",
            "WHATSAPP",
            "DELETE_FILE",
            "LOCK_DEVICE"
        )
    }

    fun confirmationMessage(action: MyraAction): String {
        return when (action.type) {
            "CALL" -> "क्या call करना है?"
            "SMS" -> "क्या SMS भेजना है?"
            "WHATSAPP" -> "क्या WhatsApp message भेजना है?"
            "DELETE_FILE" -> "क्या file delete करनी है?"
            "LOCK_DEVICE" -> "क्या device lock करना है?"
            else -> "क्या यह action करना है?"
        }
    }
}

enum class MyraConfirmation {
    YES, NO, FIRST, SECOND, UNKNOWN
}

object MyraConfirmationParser {
    fun parse(text: String): MyraConfirmation {
        val t = text.trim().lowercase()

        return when {
            t in setOf("हाँ", "हां", "yes", "haan", "ha") ->
                MyraConfirmation.YES

            t in setOf("नहीं", "no", "nahi", "nahin") ->
                MyraConfirmation.NO

            t.contains("पहला") || t == "first" || t == "1" ->
                MyraConfirmation.FIRST

            t.contains("दूसरा") || t == "second" || t == "2" ->
                MyraConfirmation.SECOND

            else -> MyraConfirmation.UNKNOWN
        }
    }
}

enum class MyraPolicy {
    ALLOWED, CONFIRM, BLOCKED
}

object MyraActionRegistry {

    private val definitions = mutableMapOf<String, MyraActionDefinition>()

    init {
        listOf(
            MyraActionDefinition("CLICK"),
            MyraActionDefinition("TAP"),
            MyraActionDefinition("LONG_PRESS"),
            MyraActionDefinition("TYPE"),
            MyraActionDefinition("SWIPE"),
            MyraActionDefinition("BACK"),

            MyraActionDefinition("OPEN_APP"),
            MyraActionDefinition("SWITCH_APP"),
            MyraActionDefinition("HOME"),
            MyraActionDefinition("SETTINGS"),
            MyraActionDefinition("CAMERA"),

            MyraActionDefinition("CALL", true),
            MyraActionDefinition("SMS", true),
            MyraActionDefinition("WHATSAPP", true),

            MyraActionDefinition("WEB_SEARCH"),
            MyraActionDefinition("OPEN_URL"),

            MyraActionDefinition("VOLUME_UP"),
            MyraActionDefinition("VOLUME_DOWN"),
            MyraActionDefinition("FLASHLIGHT"),
            MyraActionDefinition("BATTERY"),
            MyraActionDefinition("TIMER"),
            MyraActionDefinition("ALARM"),

            MyraActionDefinition("MEDIA_PLAY"),
            MyraActionDefinition("MEDIA_PAUSE"),
            MyraActionDefinition("MEDIA_NEXT"),
            MyraActionDefinition("MEDIA_PREVIOUS"),
            MyraActionDefinition("MEDIA_STOP"),

            MyraActionDefinition("NAVIGATION"),

            MyraActionDefinition("READ_FILE"),
            MyraActionDefinition("WRITE_FILE"),
            MyraActionDefinition("APPEND_FILE"),
            MyraActionDefinition("SEARCH_FILE"),
            MyraActionDefinition("LIST_FILES"),
            MyraActionDefinition("SHARE_FILE"),
            MyraActionDefinition("DELETE_FILE", true),

            MyraActionDefinition("LOCK_DEVICE", true),

            MyraActionDefinition("CLIPBOARD_COPY"),
            MyraActionDefinition("CLIPBOARD_GET"),
            MyraActionDefinition("CLIPBOARD_CLEAR"),
            MyraActionDefinition("DEVICE_INFO"),
            MyraActionDefinition("WAIT"),

            MyraActionDefinition("EXECUTE_INTENT"),
            MyraActionDefinition("SCRAPE_SCREEN"),
            MyraActionDefinition("SEND_EMERGENCY_ALERT", true)
        ).forEach {
            definitions[it.type] = it
        }
    }

    fun get(type: String): MyraActionDefinition? =
        definitions[type]
}

data class MyraActionDefinition(
    val type: String,
    val requiresConfirmation: Boolean = false,
    val enabled: Boolean = true
)

object MyraActionPolicy {

    fun check(action: MyraAction): MyraPolicy {

        val definition =
            MyraActionRegistry.get(action.type)
                ?: return MyraPolicy.BLOCKED

        if (!definition.enabled) {
            return MyraPolicy.BLOCKED
        }

        if (
            definition.requiresConfirmation ||
            action.requiresConfirmation
        ) {
            return MyraPolicy.CONFIRM
        }

        return MyraPolicy.ALLOWED
    }
}

class MyraCore(
    private val context: Context
) {

    private val planner = MyraPlanner()
    private val safety = MyraSafetyGate()
    private val memory = MyraConversationMemory()

    val capabilityManager = MyraCapabilityManager()

    private var pendingAction: MyraAction? = null

    init {
        capabilityManager.register(
            MyraScreenAutomationCapability()
        )

        capabilityManager.register(
            MyraAppCapability(context)
        )

        capabilityManager.register(
            MyraSystemCapability(context)
        )

        capabilityManager.register(
            MyraUtilityCapability(context)
        )

        capabilityManager.register(
            MyraMediaCapability(context)
        )

        capabilityManager.register(
            MyraBrowserCapability(context)
        )

        capabilityManager.register(
            MyraFileCapability(context)
        )

        capabilityManager.register(
            MyraNavigationCapability(context)
        )

        capabilityManager.register(
            MyraCommunicationCapability(context)
        )

        capabilityManager.register(
            MyraIntentCapability(context)
        )

        capabilityManager.register(
            MyraScreenScrapingCapability()
        )

        capabilityManager.register(
            MyraEmergencyCapability(context)
        )
    }

    suspend fun process(
        command: String,
        onProgress: (String) -> Unit = {}
    ): String {

        val clean =
            MyraCommandNormalizer()
                .normalize(command)

        val confirmation =
            pendingAction

        if (confirmation != null) {

            when (
                MyraConfirmationParser.parse(clean)
            ) {

                MyraConfirmation.YES -> {
                    pendingAction = null

                    val result =
                        capabilityManager.execute(
                            confirmation
                        )

                    if (result.success) {
                        memory.save(
                            MyraMemoryItem(
                                confirmation.type,
                                confirmation.target,
                                confirmation.value
                            )
                        )
                    }

                    return result.message
                }

                MyraConfirmation.NO -> {
                    pendingAction = null
                    return "ठीक है, action cancel कर दिया."
                }

                else -> {
                    return "हाँ या नहीं बोलो."
                }
            }
        }

        val plan =
            planner.plan(clean)

        if (plan.actions.isEmpty()) {
            return "Command समझ नहीं आया."
        }

        val responses =
            mutableListOf<String>()

        for (action in plan.actions) {

            val policy =
                MyraActionPolicy.check(action)

            when (policy) {

                MyraPolicy.BLOCKED -> {
                    responses.add(
                        "${action.type} action allowed नहीं है."
                    )
                    continue
                }

                MyraPolicy.CONFIRM -> {
                    pendingAction = action
                    return safety.confirmationMessage(action)
                }

                MyraPolicy.ALLOWED -> {}
            }

            onProgress(
                "${action.type} execute कर रही हूँ..."
            )

            val result =
                capabilityManager.execute(action)

            responses.add(result.message)

            if (!result.success) {
                break
            }

            memory.save(
                MyraMemoryItem(
                    action.type,
                    action.target,
                    action.value
                )
            )
        }

        return responses.joinToString("\n")
    }

    fun getCapabilityStatus():
        List<MyraCapabilityStatus> {
        return capabilityManager.status()
    }
}
