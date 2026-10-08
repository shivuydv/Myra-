package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

// ============================================================
// 1. ADVANCED INTENTS CONTROLLER & CAPABILITY
// ============================================================
class MyraIntentController(private val context: Context) {
    fun executeIntent(actionName: String, dataUri: String = "") {
        try {
            val intent = if (dataUri.isNotBlank()) {
                Intent(actionName, Uri.parse(dataUri))
            } else {
                Intent(actionName)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class MyraIntentCapability(private val context: Context) : MyraCapability {
    override val id = "advanced_intents"
    override fun supports(actionType: String) = actionType == "EXECUTE_INTENT"

    override suspend fun execute(action: MyraAction): MyraActionResult {
        return try {
            MyraIntentController(context).executeIntent(action.target, action.value)
            MyraActionResult(
                MyraActionStatus.SUCCESS,
                "Intent triggered: ${action.target} with ${action.value}"
            )
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "Intent error: ${e.message}")
        }
    }
}

// ============================================================
// 2. SCREEN SCRAPING INSPECTOR & CAPABILITY
// ============================================================
class MyraScreenScraper {
    fun scrapeScreenElements(): List<String> {
        val root = MyraAccessibilityService.instance?.rootInActiveWindow ?: return emptyList()
        val scrapedData = mutableListOf<String>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            
            val text = node.text?.toString()
            val description = node.contentDescription?.toString()
            
            if (!text.isNullOrBlank()) {
                scrapedData.add("Text: $text [Class: ${node.className}]")
            }
            if (!description.isNullOrBlank()) {
                scrapedData.add("Desc: $description")
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)
        return scrapedData
    }
}

class MyraScreenScrapingCapability : MyraCapability {
    override val id = "screen_scraping"
    override fun supports(actionType: String) = actionType == "SCRAPE_SCREEN"

    override suspend fun execute(action: MyraAction): MyraActionResult {
        val elements = MyraScreenScraper().scrapeScreenElements()
        val message = if (elements.isEmpty()) {
            "Screen पर कोई elements नहीं मिले। कृपया एक्सेसिबिलिटी सर्विस चालू रखें।"
        } else {
            elements.joinToString("\n")
        }
        return MyraActionResult(MyraActionStatus.SUCCESS, message)
    }
}

// ============================================================
// 3. SOS EMERGENCY ALERT CONTROLLER & CAPABILITY
// ============================================================
class MyraEmergencyController(private val context: Context) {
    fun sendEmergencyAlert(trustedPhone: String, locationUrl: String) {
        val emergencyMessage = "EMERGENCY! मुझे तुरंत मदद की ज़रूरत है। मेरी वर्तमान लोकेशन: $locationUrl"
        
        val uri = Uri.parse("smsto:$trustedPhone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", emergencyMessage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

class MyraEmergencyCapability(private val context: Context) : MyraCapability {
    override val id = "emergency_sos"
    override fun supports(actionType: String) = actionType == "SEND_EMERGENCY_ALERT"

    override suspend fun execute(action: MyraAction): MyraActionResult {
        return try {
            MyraEmergencyController(context).sendEmergencyAlert(action.target, action.value)
            MyraActionResult(
                MyraActionStatus.SUCCESS,
                "${action.target} को SOS इमरजेंसी एसएमएस भेज दिया।"
            )
        } catch (e: Exception) {
            MyraActionResult(MyraActionStatus.FAILED, "SOS SMS error: ${e.message}")
        }
    }
}
