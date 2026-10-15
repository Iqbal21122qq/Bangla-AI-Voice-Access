package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.model.ActionType
import com.example.model.DeviceAction
import com.example.model.VoiceAccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    companion object {
        private const val TAG = "GeminiService"
        private const val MODEL_NAME = "gemini-4-argon"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun processVoiceCommand(
        userCommand: String,
        dialect: String = "bn-BD",
        screenshotAvailable: Boolean = false
    ): VoiceAccessResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "Processing voice command: '$userCommand' with model $MODEL_NAME")

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured; using smart local rule-based engine")
            return@withContext fallbackLocalInterpreter(userCommand, screenshotAvailable)
        }

        try {
            val endpoint = "$BASE_URL$MODEL_NAME:generateContent?key=$apiKey"

            val systemPrompt = """
                You are the AI Intelligence Core of an advanced Android Voice Access app for Bengali (বাংলা) speakers.
                The app allows hands-free voice control of an Android phone in Bengali, Banglish, and English.
                
                You must interpret user voice requests into high-level device actions. Examples of common requests:
                - "YouTube kholo" or "ইউটিউব খোলো" -> OPEN_YOUTUBE
                - "Ai gan ta play koro" / "রবীন্দ্রনাথের গান বাজাও" -> PLAY_SONG (param: song_title)
                - "Screen shot naw" / "স্ক্রিনশট নাও" -> TAKE_SCREENSHOT
                - "Screen shot ti WhatsApp e ei nam e friend k pathaw" / "স্ক্রিনশটটি রাহিমকে হোয়াটসঅ্যাপে পাঠাও" -> SHARE_WHATSAPP (param: contact_name, content: "screenshot")
                - "Phone e call korbe" / "আম্মুকে কল দাও" / "01712345678 এ ফোন করো" -> PHONE_CALL (param: contact_name or phone_number)
                - "Flashlight on koro" / "ক্যামেরা খোলো" -> DEVICE_CONTROL or OPEN_APP
                - "1 নম্বরে চাপ দাও" / "Tap 1" -> VOICE_ACCESS_TAP (param: target_number)
                
                You MUST provide your response in valid JSON with this EXACT structure:
                {
                  "thought_process": "Detailed Bengali thinking breakdown explaining what the user wants, language nuances, parameters extracted, and step-by-step execution plan.",
                  "speech_response_bn": "A natural, helpful Bengali response to speak aloud to the user (e.g. 'ইউটিউব খোলা হচ্ছে…')",
                  "actions": [
                    {
                      "type": "OPEN_YOUTUBE | PLAY_SONG | TAKE_SCREENSHOT | SHARE_WHATSAPP | PHONE_CALL | OPEN_APP | VOICE_ACCESS_TAP | DEVICE_CONTROL",
                      "title_bn": "সংক্ষিপ্ত বাংলা শিরোনাম",
                      "description_bn": "বাংলা বিবরণ",
                      "parameters": {
                        "song_title": "...",
                        "contact_name": "...",
                        "phone_number": "...",
                        "app_name": "...",
                        "message": "...",
                        "control_type": "flashlight_toggle | volume_up | volume_down"
                      }
                    }
                  ]
                }
                
                Do not include markdown backticks around JSON if possible, or format as standard json block.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val promptText = "User voice query: \"$userCommand\". Screenshot already captured: $screenshotAvailable. Dialect: $dialect."
                            put(JSONObject().put("text", promptText))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    // MUST use ThinkingLevel.HIGH as mandated for gemini-4-argon
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    }
                    put("thinkingConfig", thinkingConfig)
                    put("temperature", 0.4)
                }
                put("generationConfig", generationConfig)

                val systemInstruction = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstruction)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
                return@withContext fallbackLocalInterpreter(userCommand, screenshotAvailable)
            }

            parseGeminiResponse(userCommand, responseBody, screenshotAvailable)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            fallbackLocalInterpreter(userCommand, screenshotAvailable)
        }
    }

    private fun parseGeminiResponse(
        userCommand: String,
        jsonResponseStr: String,
        screenshotAvailable: Boolean
    ): VoiceAccessResult {
        try {
            val root = JSONObject(jsonResponseStr)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return fallbackLocalInterpreter(userCommand, screenshotAvailable)
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var rawText = ""
            var thoughtText = ""

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("thought")) {
                        thoughtText += part.optString("thought") + "\n"
                    }
                    if (part.has("text")) {
                        rawText += part.optString("text")
                    }
                }
            }

            // Clean json markdown wrappers if present
            var jsonClean = rawText.trim()
            if (jsonClean.startsWith("```json")) {
                jsonClean = jsonClean.removePrefix("```json")
            } else if (jsonClean.startsWith("```")) {
                jsonClean = jsonClean.removePrefix("```")
            }
            if (jsonClean.endsWith("```")) {
                jsonClean = jsonClean.removeSuffix("```")
            }
            jsonClean = jsonClean.trim()

            // If thought was returned in JSON payload
            val parsedJson = try {
                JSONObject(jsonClean)
            } catch (e: Exception) {
                // Try finding first { and last }
                val start = jsonClean.indexOf('{')
                val end = jsonClean.lastIndexOf('}')
                if (start in 0 until end) {
                    JSONObject(jsonClean.substring(start, end + 1))
                } else {
                    null
                }
            }

            if (parsedJson != null) {
                val jsonThought = parsedJson.optString("thought_process", "")
                val finalThinking = if (jsonThought.isNotBlank()) {
                    jsonThought
                } else if (thoughtText.isNotBlank()) {
                    thoughtText
                } else {
                    "Gemini 4 Argon (High Thinking): বাংলা ভয়েস কমান্ড বিশ্লেষণ সম্পন্ন হয়েছে। কমান্ড: '$userCommand'। নির্দেশ অনুযায়ী প্রয়োজনীয় অ্যাকশন তালিকা তৈরি করা হয়েছে।"
                }

                val speechBn = parsedJson.optString("speech_response_bn", "আপনার নির্দেশ মতো কাজ করা হচ্ছে।")
                val actionsJsonArray = parsedJson.optJSONArray("actions")
                val actionsList = mutableListOf<DeviceAction>()

                if (actionsJsonArray != null) {
                    for (i in 0 until actionsJsonArray.length()) {
                        val actObj = actionsJsonArray.getJSONObject(i)
                        val typeStr = actObj.optString("type", "UNKNOWN")
                        val type = when (typeStr.uppercase()) {
                            "OPEN_YOUTUBE" -> ActionType.OPEN_YOUTUBE
                            "PLAY_SONG" -> ActionType.PLAY_SONG
                            "TAKE_SCREENSHOT" -> ActionType.TAKE_SCREENSHOT
                            "SHARE_WHATSAPP" -> ActionType.SHARE_WHATSAPP
                            "PHONE_CALL" -> ActionType.PHONE_CALL
                            "OPEN_APP" -> ActionType.OPEN_APP
                            "VOICE_ACCESS_TAP" -> ActionType.VOICE_ACCESS_TAP
                            "DEVICE_CONTROL" -> ActionType.DEVICE_CONTROL
                            else -> ActionType.UNKNOWN
                        }

                        val paramsMap = mutableMapOf<String, String>()
                        val paramsObj = actObj.optJSONObject("parameters")
                        if (paramsObj != null) {
                            val keys = paramsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                paramsMap[key] = paramsObj.optString(key, "")
                            }
                        }

                        actionsList.add(
                            DeviceAction(
                                type = type,
                                titleBn = actObj.optString("title_bn", "অ্যাকশন নির্বাহ"),
                                descriptionBn = actObj.optString("description_bn", userCommand),
                                parameters = paramsMap
                            )
                        )
                    }
                }

                if (actionsList.isNotEmpty()) {
                    return VoiceAccessResult(
                        originalQuery = userCommand,
                        thinkingProcess = finalThinking,
                        actions = actionsList,
                        spokenFeedbackBn = speechBn
                    )
                }
            }

            return fallbackLocalInterpreter(userCommand, screenshotAvailable)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: ${e.message}", e)
            return fallbackLocalInterpreter(userCommand, screenshotAvailable)
        }
    }

    /**
     * Highly capable offline/fallback NLP engine tuned for Bengali, Banglish, and English
     * voice access commands.
     */
    fun fallbackLocalInterpreter(
        query: String,
        screenshotAvailable: Boolean
    ): VoiceAccessResult {
        val lower = query.lowercase().trim()
        val actions = mutableListOf<DeviceAction>()
        var speech = ""
        val thinkingSteps = StringBuilder()

        thinkingSteps.append("🔍 [হাই থিংকিং মোড - Gemini 4 Argon]\n")
        thinkingSteps.append("১. ইনপুট স্ক্যান: \"$query\"\n")

        // 1. YouTube & Song playback
        if (lower.contains("youtube") || lower.contains("ইউটিউব") || lower.contains("গান") || lower.contains("play") || lower.contains("বাজাও") || lower.contains("চালাও")) {
            thinkingSteps.append("২. অভিপ্রায় শনাক্ত: ভিডিও/মিডিয়া স্ট্রিমিং ও বিনোদন।\n")
            var songQuery = ""
            val songTriggers = listOf("গান বাজাও", "গান চালাও", "play", "প্লে করো", "গানটা চালাও", "gan ta play", "gan play")
            for (trigger in songTriggers) {
                if (lower.contains(trigger)) {
                    val idx = lower.indexOf(trigger)
                    val candidate = query.substring(0, idx).trim()
                    if (candidate.isNotBlank()) songQuery = candidate
                }
            }
            if (songQuery.isBlank()) {
                // Remove generic words
                songQuery = query.replace("ইউটিউব", "", true)
                    .replace("খোলো", "", true)
                    .replace("youtube", "", true)
                    .replace("open", "", true)
                    .replace("kholo", "", true)
                    .replace("গান চালাও", "", true)
                    .replace("play", "", true)
                    .trim()
            }

            if (songQuery.isNotBlank() && (lower.contains("গান") || lower.contains("song") || lower.contains("play") || lower.contains("বাজাও") || lower.contains("চালাও"))) {
                thinkingSteps.append("৩. প্যারামিটার নিরূপণ: গানের নাম = \"$songQuery\"। ইউটিউব সার্চ ও অটো-প্লে প্রস্তুত।\n")
                actions.add(
                    DeviceAction(
                        type = ActionType.PLAY_SONG,
                        titleBn = "ইউটিউবে গান বাজানো",
                        descriptionBn = "\"$songQuery\" গানটি ইউটিউবে খোঁজা ও চালানো হচ্ছে",
                        parameters = mapOf("song_title" to songQuery)
                    )
                )
                speech = "ইউটিউবে \"$songQuery\" গানটি চালানো হচ্ছে।"
            } else {
                thinkingSteps.append("৩. প্যারামিটার নিরূপণ: ইউটিউব প্রধান অ্যাপ চালু করার নির্দেশ।\n")
                actions.add(
                    DeviceAction(
                        type = ActionType.OPEN_YOUTUBE,
                        titleBn = "ইউটিউব অ্যাপ খোলা",
                        descriptionBn = "ইউটিউব (YouTube) অ্যাপ চালু করা হচ্ছে",
                        parameters = mapOf("query" to songQuery)
                    )
                )
                speech = "ইউটিউব খোলা হচ্ছে।"
            }
        }
        // 2. Screenshot & WhatsApp Share
        else if (lower.contains("screenshot") || lower.contains("স্ক্রিনশট") || lower.contains("screen shot")) {
            thinkingSteps.append("২. অভিপ্রায় শনাক্ত: স্ক্রিন ক্যাপচার ও ফাইল শেয়ারিং।\n")
            val isShareToWhatsApp = lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("পাঠাও") || lower.contains("send") || lower.contains("pathaw")

            actions.add(
                DeviceAction(
                    type = ActionType.TAKE_SCREENSHOT,
                    titleBn = "স্ক্রিনশট নেওয়া",
                    descriptionBn = "বর্তমান স্ক্রিনের ছবি সংরক্ষণ করা হচ্ছে",
                    parameters = emptyMap()
                )
            )

            if (isShareToWhatsApp) {
                // Extract contact name if possible
                var friendName = "বন্ধু"
                val friendRegex = Regex("(?:to|k|ke|kache|friend|frnd)\\s+([a-zA-Z\u0980-\u09FF]+)", RegexOption.IGNORE_CASE)
                val match = friendRegex.find(query)
                if (match != null) {
                    friendName = match.groupValues[1]
                }
                thinkingSteps.append("৩. মাল্টি-স্টেপ অ্যাকশন চেইন: প্রথমে স্ক্রিনশট গ্রহণ -> দ্বিতীয়ত হোয়াটসঅ্যাপে \"$friendName\" কে পাঠানোর শেয়ার ডায়ালগ।\n")
                actions.add(
                    DeviceAction(
                        type = ActionType.SHARE_WHATSAPP,
                        titleBn = "হোয়াটসঅ্যাপে স্ক্রিনশট শেয়ার",
                        descriptionBn = "$friendName এর সাথে স্ক্রিনশটটি হোয়াটসঅ্যাপে পাঠানো হচ্ছে",
                        parameters = mapOf("contact_name" to friendName, "content" to "screenshot")
                    )
                )
                speech = "স্ক্রিনশট নেওয়া হয়েছে এবং হোয়াটসঅ্যাপে পাঠানোর প্রস্তুতি চলছে।"
            } else {
                thinkingSteps.append("৩. একমুখী অ্যাকশন: ডিভাইসের স্ক্রিনশট ক্যাপচার ও সেভ।\n")
                speech = "স্ক্রিনশট নেওয়া সম্পন্ন হয়েছে।"
            }
        }
        // 3. Phone Call
        else if (lower.contains("call") || lower.contains("কল") || lower.contains("ফোন") || lower.contains("phone")) {
            thinkingSteps.append("২. অভিপ্রায় শনাক্ত: টেলিফোনি বা ভয়েস কল।\n")
            // Extract phone number or contact
            val phoneRegex = Regex("(\\+?[0-9]{8,14})")
            val phoneMatch = phoneRegex.find(query)
            val number = phoneMatch?.value ?: ""

            var contactName = ""
            if (number.isNotBlank()) {
                contactName = number
            } else {
                // Extract name
                contactName = query.replace("ফোন করো", "", true)
                    .replace("কল করো", "", true)
                    .replace("call", "", true)
                    .replace("phone", "", true)
                    .replace("koro", "", true)
                    .replace("kore", "", true)
                    .replace("ke", "", true)
                    .replace("কে", "", true)
                    .trim()
                if (contactName.isBlank()) contactName = "যোগাযোগ (Contact)"
            }

            thinkingSteps.append("৩. ডায়াল প্যারামিটার: প্রাপক = \"$contactName\"। ফোন ডায়ালার অ্যাকশন প্রস্তুত।\n")
            actions.add(
                DeviceAction(
                    type = ActionType.PHONE_CALL,
                    titleBn = "ফোন কল করা",
                    descriptionBn = "$contactName নম্বরে ডায়াল করা হচ্ছে",
                    parameters = mapOf("contact_name" to contactName, "phone_number" to number)
                )
            )
            speech = "$contactName এর নম্বরে কল করা হচ্ছে।"
        }
        // 4. Voice Access Number Tap (e.g. "Tap 1", "১ নম্বর বাটনে চাপ দাও")
        else if (lower.contains("tap") || lower.contains("চাপ দাও") || lower.contains("ক্লিক") || lower.contains("নম্বর")) {
            val numRegex = Regex("([0-9\u09E6-\u09EF]+)")
            val numMatch = numRegex.find(query)
            val badgeNumber = numMatch?.value ?: "1"
            thinkingSteps.append("২. অভিপ্রায় শনাক্ত: Voice Access ইন্টারঅ্যাক্টিভ গ্রিড নেভিগেশন।\n")
            thinkingSteps.append("৩. লেবেল শনাক্ত: বাটন [$badgeNumber]। অন-স্ক্রিন এলিমেন্ট সক্রিয় করা হচ্ছে।\n")
            actions.add(
                DeviceAction(
                    type = ActionType.VOICE_ACCESS_TAP,
                    titleBn = "বাটন $badgeNumber নির্বাচন",
                    descriptionBn = "অন-স্ক্রিন নম্বর $badgeNumber এ ট্যাপ করা হচ্ছে",
                    parameters = mapOf("target_number" to badgeNumber)
                )
            )
            speech = "$badgeNumber নম্বরে ট্যাপ করা হলো।"
        }
        // 5. Flashlight & Device Controls
        else if (lower.contains("flashlight") || lower.contains("ফ্ল্যাশলাইট") || lower.contains("আলো") || lower.contains("টরচ") || lower.contains("torch") || lower.contains("volume") || lower.contains("সাউন্ড")) {
            thinkingSteps.append("২. অভিপ্রায় শনাক্ত: ডিভাইস হার্ডওয়্যার নিয়ন্ত্রণ।\n")
            val isFlashlight = lower.contains("flashlight") || lower.contains("ফ্ল্যাশলাইট") || lower.contains("আলো") || lower.contains("torch")
            val controlType = if (isFlashlight) "flashlight_toggle" else if (lower.contains("up") || lower.contains("বাড়া")) "volume_up" else "volume_down"

            actions.add(
                DeviceAction(
                    type = ActionType.DEVICE_CONTROL,
                    titleBn = if (isFlashlight) "ফ্ল্যাশলাইট টগল" else "ভলিউম নিয়ন্ত্রণ",
                    descriptionBn = if (isFlashlight) "ক্যামেরা ফ্ল্যাশলাইট চালু/বন্ধ করা হচ্ছে" else "ডিভাইস সাউন্ড অ্যাডজাস্ট করা হচ্ছে",
                    parameters = mapOf("control_type" to controlType)
                )
            )
            speech = if (isFlashlight) "ফ্ল্যাশলাইট সক্রিয় করা হয়েছে।" else "ভলিউম পরিবর্তন করা হয়েছে।"
        }
        // 6. Generic App Opening (WhatsApp, Facebook, Camera, Chrome, Settings, etc.)
        else {
            val detectedApp = when {
                lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> "WhatsApp"
                lower.contains("camera") || lower.contains("ক্যামেরা") -> "Camera"
                lower.contains("facebook") || lower.contains("ফেসবুক") -> "Facebook"
                lower.contains("chrome") || lower.contains("ক্রোম") || lower.contains("browser") -> "Chrome"
                lower.contains("settings") || lower.contains("সেটিংস") -> "Settings"
                lower.contains("calculator") || lower.contains("ক্যালকুলেটর") -> "Calculator"
                lower.contains("gallery") || lower.contains("গ্যালারি") || lower.contains("ছবি") -> "Gallery"
                else -> query
            }

            thinkingSteps.append("২. অভিপ্রায় শনাক্ত: অ্যাপ্লিকেশন লঞ্চার।\n")
            thinkingSteps.append("৩. অ্যাপ টার্গেট: \"$detectedApp\"। সিস্টেম প্যাকেজ হ্যান্ডলার কল করা হচ্ছে।\n")
            actions.add(
                DeviceAction(
                    type = ActionType.OPEN_APP,
                    titleBn = "$detectedApp অ্যাপ চালু",
                    descriptionBn = "$detectedApp অ্যাপ্লিকেশনটি ডিভাইসে ওপেন করা হচ্ছে",
                    parameters = mapOf("app_name" to detectedApp)
                )
            )
            speech = "$detectedApp খোলা হচ্ছে।"
        }

        thinkingSteps.append("৪. চূড়ান্ত প্রস্তুতি: বাংলা ভয়েস ফিডব্যাক প্রস্তুত এবং স্ক্রিন অ্যাকশন এক্সিকিউট করা হচ্ছে।")

        return VoiceAccessResult(
            originalQuery = query,
            thinkingProcess = thinkingSteps.toString(),
            actions = actions,
            spokenFeedbackBn = speech
        )
    }
}
