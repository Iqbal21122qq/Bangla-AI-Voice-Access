package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActionType
import com.example.model.DeviceAction
import com.example.model.DialectMode
import com.example.model.PresetCommand
import com.example.model.VoiceAccessResult
import com.example.service.ActionExecutor
import com.example.service.GeminiService
import com.example.service.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceAccessUiState(
    val isServiceActive: Boolean = true,
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioRmsLevel: Float = 0f,
    val currentTranscript: String = "",
    val partialTranscript: String = "",
    val lastResult: VoiceAccessResult? = null,
    val commandHistory: List<VoiceAccessResult> = emptyList(),
    val isGridOverlayEnabled: Boolean = false,
    val selectedDialect: DialectMode = DialectMode.BANGLADESH,
    val isHighThinkingEnabled: Boolean = true,
    val autoExecuteActions: Boolean = true,
    val speechRate: Float = 1.0f,
    val statusMessage: String = "ভয়েস কমান্ড দিতে মাইক্রোফোনে ট্যাপ করুন",
    val capturedScreenshot: Bitmap? = null,
    val lastExecutedStatus: String? = null,
    val activeTab: Int = 0 // 0: Home/Voice, 1: Grid Mode, 2: Commands Cheatsheet, 3: Settings
)

class VoiceAccessViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiService()
    private val actionExecutor = ActionExecutor(application.applicationContext)

    private val _uiState = MutableStateFlow(VoiceAccessUiState())
    val uiState: StateFlow<VoiceAccessUiState> = _uiState.asStateFlow()

    private var voiceEngine: VoiceEngine? = null

    val presetCommands = listOf(
        PresetCommand("1", "ইউটিউব খোলো", "YouTube kholo", "অ্যাপ্লিকেশন", "smart_display"),
        PresetCommand("2", "গান চালাও", "রবীন্দ্রনাথের গান বাজাও", "মিডিয়া", "music_note"),
        PresetCommand("3", "স্ক্রিনশট নাও", "Screen shot naw", "ক্যাপচার", "screenshot"),
        PresetCommand("4", "স্ক্রিনশট হোয়াটসঅ্যাপে পাঠাও", "স্ক্রিনশটটি হোয়াটসঅ্যাপে রহিমকে পাঠাও", "শেয়ার", "share"),
        PresetCommand("5", "ফোন কল করো", "আম্মুকে ফোন করো", "টেলিফোনি", "call"),
        PresetCommand("6", "ক্যামেরা খোলো", "Camera kholo", "টুলস", "photo_camera"),
        PresetCommand("7", "ফ্ল্যাশলাইট জ্বালাও", "Flashlight on koro", "ডিভাইস", "flashlight_on"),
        PresetCommand("8", "১ নম্বর বাটনে চাপ দাও", "Tap 1", "ভয়েস অ্যাক্সেস গ্রিড", "touch_app")
    )

    init {
        initVoiceEngine()
    }

    private fun initVoiceEngine() {
        voiceEngine = VoiceEngine(
            context = getApplication(),
            onSpeechRecognized = { text ->
                _uiState.update {
                    it.copy(
                        currentTranscript = text,
                        partialTranscript = "",
                        isListening = false,
                        statusMessage = "কমান্ড গৃহীত হয়েছে: \"$text\""
                    )
                }
                processCommand(text)
            },
            onPartialSpeech = { partial ->
                _uiState.update { it.copy(partialTranscript = partial) }
            },
            onRmsChangedListener = { rms ->
                _uiState.update { it.copy(audioRmsLevel = rms) }
            },
            onListeningStateChanged = { listening ->
                _uiState.update {
                    it.copy(
                        isListening = listening,
                        statusMessage = if (listening) "শুনছি… বাংলায় বলুন" else it.statusMessage
                    )
                }
            },
            onErrorOccurred = { error ->
                _uiState.update {
                    it.copy(
                        isListening = false,
                        statusMessage = error
                    )
                }
            }
        )
    }

    fun startListening() {
        if (!_uiState.value.isServiceActive) {
            _uiState.update { it.copy(statusMessage = "ভয়েস এক্সেস সার্ভিস বন্ধ রয়েছে। সার্ভিস চালু করুন।") }
            return
        }
        voiceEngine?.stopSpeaking()
        _uiState.update {
            it.copy(
                isListening = true,
                currentTranscript = "",
                partialTranscript = "",
                statusMessage = "শুনছি… বলুন"
            )
        }
        voiceEngine?.startListening()
    }

    fun stopListening() {
        voiceEngine?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun processCommand(commandText: String) {
        if (commandText.isBlank()) return

        _uiState.update {
            it.copy(
                isThinking = true,
                statusMessage = "Gemini 4 Argon (High Thinking) বিশ্লেষণ করছে…"
            )
        }

        viewModelScope.launch {
            val result = geminiService.processVoiceCommand(
                userCommand = commandText,
                dialect = _uiState.value.selectedDialect.code,
                screenshotAvailable = _uiState.value.capturedScreenshot != null
            )

            _uiState.update { state ->
                state.copy(
                    isThinking = false,
                    lastResult = result,
                    commandHistory = listOf(result) + state.commandHistory.take(19),
                    statusMessage = "বিশ্লেষণ সম্পন্ন!"
                )
            }

            // Speak feedback in Bengali
            if (result.spokenFeedbackBn.isNotBlank()) {
                _uiState.update { it.copy(isSpeaking = true) }
                voiceEngine?.speak(result.spokenFeedbackBn) {
                    _uiState.update { it.copy(isSpeaking = false) }
                }
            }

            // Auto-execute if enabled
            if (_uiState.value.autoExecuteActions && result.actions.isNotEmpty()) {
                result.actions.forEach { action ->
                    executeAction(action)
                }
            }
        }
    }

    fun executeAction(action: DeviceAction) {
        actionExecutor.executeAction(
            action = action,
            currentScreenshotBitmap = _uiState.value.capturedScreenshot
        ) { status ->
            _uiState.update {
                it.copy(
                    lastExecutedStatus = status,
                    capturedScreenshot = if (action.type == ActionType.TAKE_SCREENSHOT) ActionExecutor.lastCapturedScreenshot else it.capturedScreenshot
                )
            }
        }
    }

    fun captureScreenshotManually() {
        val bmp = actionExecutor.takeScreenshot(null) { status ->
            _uiState.update {
                it.copy(
                    capturedScreenshot = ActionExecutor.lastCapturedScreenshot,
                    lastExecutedStatus = status
                )
            }
        }
    }

    fun shareScreenshotToWhatsApp(contactName: String? = null) {
        actionExecutor.executeAction(
            DeviceAction(
                type = ActionType.SHARE_WHATSAPP,
                titleBn = "হোয়াটসঅ্যাপে স্ক্রিনশট শেয়ার",
                descriptionBn = "হোয়াটসঅ্যাপে স্ক্রিনশট পাঠানো হচ্ছে",
                parameters = mapOf(
                    "contact_name" to (contactName ?: "বন্ধু"),
                    "content" to "screenshot"
                )
            ),
            currentScreenshotBitmap = _uiState.value.capturedScreenshot
        ) { status ->
            _uiState.update { it.copy(lastExecutedStatus = status) }
        }
    }

    fun toggleService(active: Boolean) {
        _uiState.update { it.copy(isServiceActive = active) }
        if (!active) {
            stopListening()
            voiceEngine?.stopSpeaking()
        }
    }

    fun toggleGridOverlay(enabled: Boolean) {
        _uiState.update { it.copy(isGridOverlayEnabled = enabled) }
    }

    fun setDialect(dialect: DialectMode) {
        _uiState.update { it.copy(selectedDialect = dialect) }
        voiceEngine?.setLanguage(dialect.code)
    }

    fun toggleHighThinking(enabled: Boolean) {
        _uiState.update { it.copy(isHighThinkingEnabled = enabled) }
    }

    fun toggleAutoExecute(enabled: Boolean) {
        _uiState.update { it.copy(autoExecuteActions = enabled) }
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate) }
        voiceEngine?.setSpeechRate(rate)
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine?.destroy()
    }
}
