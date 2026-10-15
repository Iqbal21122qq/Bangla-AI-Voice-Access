package com.example.model

enum class ActionType {
    OPEN_YOUTUBE,
    PLAY_SONG,
    TAKE_SCREENSHOT,
    SHARE_WHATSAPP,
    PHONE_CALL,
    OPEN_APP,
    VOICE_ACCESS_TAP,
    DEVICE_CONTROL,
    UNKNOWN
}

data class DeviceAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ActionType,
    val titleBn: String,
    val descriptionBn: String,
    val parameters: Map<String, String> = emptyMap(),
    val isExecuted: Boolean = false,
    val executionStatus: String? = null
)

data class VoiceAccessResult(
    val originalQuery: String,
    val thinkingProcess: String, // Gemini-4-argon deep reasoning
    val actions: List<DeviceAction>,
    val spokenFeedbackBn: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PresetCommand(
    val id: String,
    val titleBn: String,
    val queryText: String,
    val category: String,
    val iconName: String
)

enum class DialectMode(val label: String, val code: String) {
    BANGLADESH("বাংলা (বাংলাদেশ)", "bn-BD"),
    WEST_BENGAL("বাংলা (ভারত)", "bn-IN"),
    BANGLISH("বাংলিশ (Banglish Phonetic)", "bn-BD")
}
