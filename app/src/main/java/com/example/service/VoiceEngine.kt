package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onPartialSpeech: (String) -> Unit,
    private val onRmsChangedListener: (Float) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onErrorOccurred: (String) -> Unit
) {

    companion object {
        private const val TAG = "VoiceEngine"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var currentLanguageCode: String = "bn-BD"

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val bengaliLocale = Locale("bn", "BD")
                val result = textToSpeech?.setLanguage(bengaliLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val inBengali = Locale("bn", "IN")
                    val fallbackResult = textToSpeech?.setLanguage(inBengali)
                    if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Bengali TTS not natively supported on this device; using system default voice")
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                }
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.0f)
                Log.d(TAG, "TextToSpeech initialized successfully")
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            }
        }
    }

    fun setLanguage(languageCode: String) {
        currentLanguageCode = languageCode
        val locale = when (languageCode) {
            "bn-IN" -> Locale("bn", "IN")
            else -> Locale("bn", "BD")
        }
        if (isTtsInitialized) {
            textToSpeech?.setLanguage(locale)
        }
    }

    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not ready to speak: '$text'")
            onFinished?.invoke()
            return
        }

        val utteranceId = "VoiceAccess_${System.currentTimeMillis()}"
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onFinished?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onFinished?.invoke()
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorOccurred("এই ডিভাইসে ভয়েস রিকগনিশন সার্ভিস উপলব্ধ নয়।")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB to 10dB into 0f..1f
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRmsChangedListener(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিং ত্রুটি"
                            SpeechRecognizer.ERROR_CLIENT -> "ক্লায়েন্ট সাইড ত্রুটি"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন পারমিশন প্রয়োজন"
                            SpeechRecognizer.ERROR_NETWORK -> "ইন্টারনেট সংযোগ ত্রুটি"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক টাইমআউট"
                            SpeechRecognizer.ERROR_NO_MATCH -> "কোনো কথা শনাক্ত করা যায়নি, আবার বলুন"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস সার্ভিস ব্যস্ত আছে"
                            SpeechRecognizer.ERROR_SERVER -> "সার্ভার রেসপন্স ত্রুটি"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি"
                            else -> "ভয়েস ইনপুট ত্রুটি ($error)"
                        }
                        Log.w(TAG, "SpeechRecognizer error: $message ($error)")
                        onListeningStateChanged(false)
                        onErrorOccurred(message)
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val query = matches?.firstOrNull() ?: ""
                        if (query.isNotBlank()) {
                            Log.d(TAG, "Speech recognized: $query")
                            onSpeechRecognized(query)
                        } else {
                            onErrorOccurred("কথা বোঝা যায়নি, আবার চেষ্টা করুন")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            onPartialSpeech(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguageCode)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, currentLanguageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognizer: ${e.message}", e)
            onListeningStateChanged(false)
            onErrorOccurred("ভয়েস ইনপুট শুরু করতে সমস্যা হয়েছে: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer: ${e.message}")
        }
        onListeningStateChanged(false)
    }

    fun destroy() {
        stopListening()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
