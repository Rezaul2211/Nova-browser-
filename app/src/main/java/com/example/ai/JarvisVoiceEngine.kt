package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface JarvisAction {
    data class Navigate(val url: String, val label: String) : JarvisAction
    data class Search(val query: String) : JarvisAction
    data class Scroll(val direction: String) : JarvisAction // "down", "up", "top", "bottom"
    data class ClickElement(val targetText: String) : JarvisAction
    data class NewTab(val url: String? = null) : JarvisAction
    data object CloseTab : JarvisAction
    data object RefreshPage : JarvisAction
    data object GoBack : JarvisAction
    data object GoForward : JarvisAction
    data object SummarizePage : JarvisAction
    data class TranslatePage(val targetLang: String) : JarvisAction
    data class SpeakOnly(val message: String) : JarvisAction
}

enum class JarvisVoiceStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    EXECUTING,
    SPEAKING,
    ERROR
}

data class JarvisUiState(
    val isLiveModeActive: Boolean = false,
    val status: JarvisVoiceStatus = JarvisVoiceStatus.IDLE,
    val recognizedSpeech: String = "",
    val aiResponseText: String = "",
    val activeActionLabel: String = "",
    val isTtsSpeaking: Boolean = false,
    val soundLevel: Float = 0f,
    val preferredLanguage: String = "bn-BD",
    val error: String? = null
)

class JarvisVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val conversationHistory = mutableListOf<String>()

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@JarvisVoiceEngine)
                    }
                }
                textToSpeech = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                // Sandbox fallback
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.let { tts ->
                val bnLocale = Locale("bn", "BD")
                val langResult = tts.setLanguage(bnLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.US
                }
                tts.setPitch(1.02f)
                tts.setSpeechRate(1.0f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(
                            isTtsSpeaking = true,
                            status = JarvisVoiceStatus.SPEAKING
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(
                            isTtsSpeaking = false,
                            status = if (_uiState.value.isLiveModeActive) JarvisVoiceStatus.LISTENING else JarvisVoiceStatus.IDLE
                        )
                        // Gemini Live continuous listening loop:
                        if (_uiState.value.isLiveModeActive) {
                            mainHandler.postDelayed({
                                if (_uiState.value.isLiveModeActive && !_uiState.value.isTtsSpeaking) {
                                    startListeningInternal(_uiState.value.preferredLanguage)
                                }
                            }, 450)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(
                            isTtsSpeaking = false,
                            status = if (_uiState.value.isLiveModeActive) JarvisVoiceStatus.LISTENING else JarvisVoiceStatus.IDLE
                        )
                    }
                })
            }
        }
    }

    fun toggleLiveMode(preferredLanguage: String = "bn-BD") {
        if (_uiState.value.isLiveModeActive) {
            stopLiveMode()
        } else {
            startLiveMode(preferredLanguage)
        }
    }

    fun startLiveMode(preferredLanguage: String = "bn-BD") {
        stopSpeaking()
        _uiState.value = _uiState.value.copy(
            isLiveModeActive = true,
            preferredLanguage = preferredLanguage,
            status = JarvisVoiceStatus.LISTENING,
            recognizedSpeech = "",
            aiResponseText = "",
            activeActionLabel = if (preferredLanguage.startsWith("bn")) "শুনছি, বলুন..." else "Listening live...",
            error = null
        )
        startListeningInternal(preferredLanguage)
    }

    fun stopLiveMode() {
        stopListening()
        stopSpeaking()
        _uiState.value = _uiState.value.copy(
            isLiveModeActive = false,
            status = JarvisVoiceStatus.IDLE,
            recognizedSpeech = "",
            aiResponseText = "",
            activeActionLabel = "",
            soundLevel = 0f,
            error = null
        )
    }

    fun setLanguage(language: String) {
        _uiState.value = _uiState.value.copy(preferredLanguage = language)
        if (_uiState.value.isLiveModeActive) {
            startListeningInternal(language)
        }
    }

    private fun startListeningInternal(preferredLanguage: String) {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@JarvisVoiceEngine)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferredLanguage)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, preferredLanguage)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, preferredLanguage)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Voice input error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
        }
    }

    fun speak(text: String, isBengali: Boolean = true) {
        if (!isTtsInitialized || text.isBlank()) return
        mainHandler.post {
            try {
                textToSpeech?.let { tts ->
                    val cleanText = text.replace(Regex("""[*#_`>]"""), "").trim()
                    if (isBengali || containsBengali(cleanText)) {
                        tts.language = Locale("bn", "BD")
                    } else {
                        tts.language = Locale.US
                    }
                    tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speech_${System.currentTimeMillis()}")
                }
            } catch (e: Exception) {}
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            try {
                textToSpeech?.stop()
                _uiState.value = _uiState.value.copy(isTtsSpeaking = false)
            } catch (e: Exception) {}
        }
    }

    // Recognition Listener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _uiState.value = _uiState.value.copy(
            status = JarvisVoiceStatus.LISTENING,
            activeActionLabel = if (_uiState.value.preferredLanguage.startsWith("bn")) "শুনছি..." else "Listening..."
        )
    }

    override fun onBeginningOfSpeech() {
        _uiState.value = _uiState.value.copy(
            status = JarvisVoiceStatus.LISTENING,
            activeActionLabel = if (_uiState.value.preferredLanguage.startsWith("bn")) "কথা শুনছি..." else "Hearing voice..."
        )
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
        _uiState.value = _uiState.value.copy(soundLevel = normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _uiState.value = _uiState.value.copy(
            status = JarvisVoiceStatus.PROCESSING,
            activeActionLabel = if (_uiState.value.preferredLanguage.startsWith("bn")) "বিশ্লেষণ করা হচ্ছে..." else "Thinking..."
        )
    }

    override fun onError(error: Int) {
        // In Live Mode, if no speech is detected or recognizer times out, keep listening smoothly
        if (_uiState.value.isLiveModeActive && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
            mainHandler.postDelayed({
                if (_uiState.value.isLiveModeActive && !_uiState.value.isTtsSpeaking) {
                    startListeningInternal(_uiState.value.preferredLanguage)
                }
            }, 600)
            return
        }

        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
            SpeechRecognizer.ERROR_CLIENT -> "Client paused"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission required"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue"
            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            status = if (_uiState.value.isLiveModeActive) JarvisVoiceStatus.LISTENING else JarvisVoiceStatus.IDLE,
            error = errorMessage
        )
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestSpokenText = matches?.firstOrNull() ?: ""
        if (bestSpokenText.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                recognizedSpeech = bestSpokenText,
                status = JarvisVoiceStatus.PROCESSING,
                activeActionLabel = bestSpokenText
            )
            processVoiceCommand(bestSpokenText)
        } else {
            if (_uiState.value.isLiveModeActive) {
                startListeningInternal(_uiState.value.preferredLanguage)
            } else {
                _uiState.value = _uiState.value.copy(status = JarvisVoiceStatus.IDLE)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = partials?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _uiState.value = _uiState.value.copy(recognizedSpeech = text)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    var onExecuteAction: ((JarvisAction) -> Unit)? = null
    var getPageContext: (() -> Pair<String, String>)? = null // Pair<currentUrl, currentTitle>

    fun processVoiceCommand(input: String) {
        val query = input.trim()
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(
            recognizedSpeech = query,
            status = JarvisVoiceStatus.PROCESSING,
            activeActionLabel = "Thinking..."
        )

        // Store short rolling conversation history
        conversationHistory.add("User: $query")
        if (conversationHistory.size > 8) conversationHistory.removeAt(0)

        // FULL DYNAMIC GEMINI LIVE AI AUTOMATION & CONVERSATION
        scope.launch {
            val (currentUrl, currentTitle) = getPageContext?.invoke() ?: ("" to "")
            val historyContext = conversationHistory.takeLast(4).joinToString("\n")

            val systemInstruction = """
                You are AUREN JARVIS, a hyper-intelligent, friendly, and natural voice AI assistant like Google Gemini Live, integrated directly inside Auren AI Browser.
                You can converse naturally, answer ANY question, tell stories, explain complex topics, research facts, AND seamlessly automate the browser for the user.
                
                The user can speak in Bengali (বাংলা), English, or mixed language.
                ALWAYS respond verbally in the same language the user spoke (Bangla or English).
                Keep your spoken voice response concise (1-2 sentences for browser actions, or natural conversational answers for general questions), warm, pleasant, and intelligent.
                
                Current Browser State:
                - URL: $currentUrl
                - Title: $currentTitle
                
                Recent Conversation:
                $historyContext
                
                You must decide whether the user wants to:
                1. Navigate to a site: action = "NAVIGATE", target = "https://..."
                2. Search for something: action = "SEARCH", target = "<search query>"
                3. Scroll the webpage: action = "SCROLL_DOWN" | "SCROLL_UP" | "SCROLL_TOP" | "SCROLL_BOTTOM"
                4. Click on a visible element or link: action = "CLICK", target = "<button or link text>"
                5. Tab management: action = "NEW_TAB" | "CLOSE_TAB" | "RELOAD" | "BACK" | "FORWARD"
                6. Summarize page: action = "SUMMARIZE"
                7. Translate page: action = "TRANSLATE", target = "Bengali"
                8. Conversational / Question Answering / Chit-Chat: action = "SPEAK", target = ""
                
                Output strictly a valid JSON object with NO markdown formatting around it:
                {
                   "action": "NAVIGATE" | "SEARCH" | "SCROLL_DOWN" | "SCROLL_UP" | "SCROLL_TOP" | "SCROLL_BOTTOM" | "CLICK" | "NEW_TAB" | "CLOSE_TAB" | "RELOAD" | "BACK" | "FORWARD" | "SUMMARIZE" | "TRANSLATE" | "SPEAK",
                   "target": "<URL or query or element text or target lang or empty>",
                   "voice_response": "<Natural conversational Bengali or English voice speech to speak aloud>"
                }
            """.trimIndent()

            val aiService = GeminiAiService()
            val result = aiService.generateContent(
                prompt = "User said: \"$query\"",
                systemInstruction = systemInstruction
            )

            result.onSuccess { rawResponse ->
                val (action, voiceReply) = parseAiJarvisResponse(rawResponse, query)
                conversationHistory.add("Jarvis: $voiceReply")
                withContext(Dispatchers.Main) {
                    executeJarvisAction(action, voiceReply, query)
                }
            }.onFailure { err ->
                // Fallback direct intent
                val (fallbackAction, fallbackReply) = parseLocalFallback(query)
                conversationHistory.add("Jarvis: $fallbackReply")
                withContext(Dispatchers.Main) {
                    executeJarvisAction(fallbackAction, fallbackReply, query)
                }
            }
        }
    }

    private fun executeJarvisAction(action: JarvisAction, voiceReply: String, originalCommand: String) {
        _uiState.value = _uiState.value.copy(
            status = JarvisVoiceStatus.EXECUTING,
            aiResponseText = voiceReply,
            activeActionLabel = getActionDescription(action)
        )

        onExecuteAction?.invoke(action)
        speak(voiceReply, isBengali = containsBengali(voiceReply) || containsBengali(originalCommand))
    }

    private fun parseAiJarvisResponse(jsonText: String, originalCommand: String): Pair<JarvisAction, String> {
        return try {
            val clean = jsonText.replace("```json", "").replace("```", "").trim()
            val actionType = clean.substringAfter("\"action\":").substringBefore(",").replace("\"", "").trim()
            val target = clean.substringAfter("\"target\":").substringBefore(",").replace("\"", "").trim()
            val voiceReply = clean.substringAfter("\"voice_response\":").substringBefore("}").replace("\"", "").trim()

            val effectiveReply = if (voiceReply.isNotBlank()) voiceReply else "Done."

            val action: JarvisAction = when (actionType.uppercase()) {
                "NAVIGATE" -> {
                    val url = if (target.startsWith("http://") || target.startsWith("https://")) target else "https://$target"
                    JarvisAction.Navigate(url, target)
                }
                "SEARCH" -> JarvisAction.Search(target.ifBlank { originalCommand })
                "SCROLL_DOWN" -> JarvisAction.Scroll("down")
                "SCROLL_UP" -> JarvisAction.Scroll("up")
                "SCROLL_TOP" -> JarvisAction.Scroll("top")
                "SCROLL_BOTTOM" -> JarvisAction.Scroll("bottom")
                "CLICK" -> JarvisAction.ClickElement(target)
                "NEW_TAB" -> JarvisAction.NewTab(target.takeIf { it.isNotBlank() })
                "CLOSE_TAB" -> JarvisAction.CloseTab
                "RELOAD" -> JarvisAction.RefreshPage
                "BACK" -> JarvisAction.GoBack
                "FORWARD" -> JarvisAction.GoForward
                "SUMMARIZE" -> JarvisAction.SummarizePage
                "TRANSLATE" -> JarvisAction.TranslatePage(if (target.isNotBlank()) target else "Bengali")
                else -> JarvisAction.SpeakOnly(effectiveReply)
            }
            action to effectiveReply
        } catch (e: Exception) {
            parseLocalFallback(originalCommand)
        }
    }

    private fun parseLocalFallback(raw: String): Pair<JarvisAction, String> {
        val text = raw.lowercase().trim()
        val isBn = containsBengali(raw)

        if (text.contains("scroll down") || text.contains("নিচে যাও") || text.contains("নিচে স্ক্রোল")) {
            return JarvisAction.Scroll("down") to if (isBn) "নিচে স্ক্রোল করা হলো" else "Scrolling down"
        }
        if (text.contains("scroll up") || text.contains("উপরে যাও") || text.contains("উপরে স্ক্রোল")) {
            return JarvisAction.Scroll("up") to if (isBn) "উপরে স্ক্রোল করা হলো" else "Scrolling up"
        }
        if (text.contains("youtube") || text.contains("ইউটিউব")) {
            return JarvisAction.Navigate("https://www.youtube.com", "YouTube") to if (isBn) "YouTube ওপেন করা হচ্ছে" else "Opening YouTube"
        }
        if (text.contains("google") || text.contains("গুগল")) {
            return JarvisAction.Navigate("https://www.google.com", "Google") to if (isBn) "Google ওপেন করা হচ্ছে" else "Opening Google"
        }
        if (text.contains("facebook") || text.contains("ফেসবুক")) {
            return JarvisAction.Navigate("https://www.facebook.com", "Facebook") to if (isBn) "Facebook ওপেন করা হচ্ছে" else "Opening Facebook"
        }
        if (text.contains("summarize") || text.contains("সামারি") || text.contains("সারসংক্ষেপ")) {
            return JarvisAction.SummarizePage to if (isBn) "পেজের সারসংক্ষেপ তৈরি করা হচ্ছে" else "Summarizing page"
        }
        if (text.contains("translate") || text.contains("অনুবাদ")) {
            return JarvisAction.TranslatePage("Bengali") to if (isBn) "পেজটি বাংলায় অনুবাদ করা হচ্ছে" else "Translating to Bengali"
        }

        // Default fallback to web search with conversational tone
        return JarvisAction.Search(raw) to if (isBn) "\"$raw\" অনুসন্ধান করা হচ্ছে" else "Searching for $raw"
    }

    private fun getActionDescription(action: JarvisAction): String {
        return when (action) {
            is JarvisAction.Navigate -> "Opening ${action.label}"
            is JarvisAction.Search -> "Searching \"${action.query}\""
            is JarvisAction.Scroll -> "Scrolling ${action.direction}"
            is JarvisAction.ClickElement -> "Clicking \"${action.targetText}\""
            is JarvisAction.NewTab -> "New Tab"
            is JarvisAction.CloseTab -> "Closed Tab"
            is JarvisAction.RefreshPage -> "Reloaded"
            is JarvisAction.GoBack -> "Going Back"
            is JarvisAction.GoForward -> "Going Forward"
            is JarvisAction.SummarizePage -> "Summarizing"
            is JarvisAction.TranslatePage -> "Translating"
            is JarvisAction.SpeakOnly -> "Jarvis"
        }
    }

    private fun containsBengali(text: String): Boolean {
        return text.any { it.code in 0x0980..0x09FF }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {}
    }
}
