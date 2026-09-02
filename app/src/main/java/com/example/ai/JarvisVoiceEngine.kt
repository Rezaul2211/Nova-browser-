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
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

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
    var apiKeyProvider: (() -> String)? = null

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun processVoiceCommand(input: String) {
        val query = input.trim()
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(
            recognizedSpeech = query,
            status = JarvisVoiceStatus.PROCESSING,
            activeActionLabel = if (_uiState.value.preferredLanguage.startsWith("bn")) "বিশ্লেষণ করা হচ্ছে..." else "Thinking..."
        )

        // Instant local fast-path for direct hardware/browser physical actions (e.g. scroll, navigate, reload)
        val instantAction = checkImmediateLocalAction(query)
        if (instantAction != null) {
            val (action, reply) = instantAction
            conversationHistory.add("User: $query")
            conversationHistory.add("Jarvis: $reply")
            if (conversationHistory.size > 8) conversationHistory.removeAt(0)
            executeJarvisAction(action, reply, query)
            return
        }

        // Store short rolling conversation history
        conversationHistory.add("User: $query")
        if (conversationHistory.size > 8) conversationHistory.removeAt(0)

        // FULL DYNAMIC GEMINI LIVE AI AUTOMATION & CONVERSATION
        scope.launch {
            val (currentUrl, currentTitle) = getPageContext?.invoke() ?: ("" to "")
            val historyContext = conversationHistory.takeLast(6).joinToString("\n")
            val customKey = apiKeyProvider?.invoke()?.takeIf { it.isNotBlank() }

            val systemInstruction = """
                You are AUREN JARVIS, a hyper-intelligent, human-like voice AI assistant integrated directly inside Auren AI Browser.
                You can converse naturally like Google Gemini Live, answer ANY question, explain topics, provide facts, chat warmly, AND seamlessly automate the browser.
                
                The user speaks in Bengali (বাংলা) or English.
                ALWAYS respond verbally in the exact same language the user spoke (Bangla or English).
                Keep spoken responses natural, expressive, friendly, and concise (1-2 sentences for browser actions, or direct informative voice answers for general questions).
                
                Current Webpage:
                - URL: $currentUrl
                - Title: $currentTitle
                
                Recent Conversation:
                $historyContext
                
                ACTION DECISION RULES:
                1. Scroll page: If user wants to scroll or move the page up/down (e.g. "scroll", "স্ক্রোল", "স্কুল", "নিচে যাও", "উপরে ওঠাও"):
                   action = "SCROLL_DOWN" | "SCROLL_UP" | "SCROLL_TOP" | "SCROLL_BOTTOM", target = ""
                2. Navigate: If user mentions opening a website or going to a service (e.g. "ইউটিউব ওপেন করো", "youtube", "facebook", "wikipedia.org"):
                   action = "NAVIGATE", target = "https://<valid_domain>"
                3. Search: ONLY if the user specifically requests to search the web (e.g. "সার্চ করো X", "search for X", "look up X on google"):
                   action = "SEARCH", target = "<query>"
                4. Browser tabs: "NEW_TAB" | "CLOSE_TAB" | "RELOAD" | "BACK" | "FORWARD"
                5. Page reading: "SUMMARIZE" | "TRANSLATE" (target: "Bengali")
                6. Conversational / Questions / Chit-chat / Advice: If user asks ANY question, greeting, joke, calculation, knowledge query, or statement:
                   action = "SPEAK", target = "", voice_response = "<Human-like, friendly, helpful spoken answer in Bangla or English>"
                
                You MUST return strictly a valid JSON object without markdown fences:
                {
                   "action": "SCROLL_DOWN"|"SCROLL_UP"|"SCROLL_TOP"|"SCROLL_BOTTOM"|"NAVIGATE"|"SEARCH"|"NEW_TAB"|"CLOSE_TAB"|"RELOAD"|"BACK"|"FORWARD"|"SUMMARIZE"|"TRANSLATE"|"SPEAK",
                   "target": "<target url or param or empty>",
                   "voice_response": "<Natural conversational Bengali or English voice speech to be spoken aloud>"
                }
            """.trimIndent()

            val aiService = GeminiAiService()
            val result = aiService.generateContent(
                prompt = "User said: \"$query\"",
                customApiKey = customKey,
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

    private fun checkImmediateLocalAction(raw: String): Pair<JarvisAction, String>? {
        val text = raw.lowercase().trim()
        val isBn = containsBengali(raw)

        // 1. Scrolling detection (handles Bengali speech-to-text variations like 'স্কুল', 'স্ক্রোল', 'স্ক্রল', 'নামাও', 'তোলো')
        if (text.contains("scroll down") || text.contains("স্ক্রোল ডাউন") || text.contains("স্ক্রল ডাউন") ||
            text.contains("নিচে যাও") || text.contains("নিচে নামাও") || text.contains("নিচে নামো") ||
            text.contains("স্কুল কর") || text.contains("স্ক্রল কর") || text.contains("স্ক্রোল কর") ||
            text.contains("আর একটু নিচে") || text.contains("scroll") && text.contains("down")) {
            return JarvisAction.Scroll("down") to if (isBn) "নিচে স্ক্রোল করা হচ্ছে" else "Scrolling down"
        }

        if (text.contains("scroll up") || text.contains("স্ক্রোল আপ") || text.contains("স্ক্রল আপ") ||
            text.contains("উপরে যাও") || text.contains("উপরে তোলো") || text.contains("উপরে ওঠাও") ||
            text.contains("scroll") && text.contains("up")) {
            return JarvisAction.Scroll("up") to if (isBn) "উপরে স্ক্রোল করা হচ্ছে" else "Scrolling up"
        }

        if (text.contains("scroll top") || text.contains("একদম উপরে") || text.contains("শুরুতে যাও")) {
            return JarvisAction.Scroll("top") to if (isBn) "পৃষ্ঠার শুরুতে নেওয়া হলো" else "Scrolling to top"
        }

        if (text.contains("scroll bottom") || text.contains("একদম নিচে") || text.contains("শেষে যাও")) {
            return JarvisAction.Scroll("bottom") to if (isBn) "পৃষ্ঠার শেষে নেওয়া হলো" else "Scrolling to bottom"
        }

        // 2. Navigation quick actions
        if (text.startsWith("open ") || text.startsWith("go to ") || text.contains("ওপেন করো") || text.contains("খুলো") || text.contains("যাও")) {
            if (text.contains("youtube") || text.contains("ইউটিউব")) {
                return JarvisAction.Navigate("https://www.youtube.com", "YouTube") to if (isBn) "YouTube ওপেন করা হচ্ছে" else "Opening YouTube"
            }
            if (text.contains("facebook") || text.contains("ফেসবুক")) {
                return JarvisAction.Navigate("https://www.facebook.com", "Facebook") to if (isBn) "Facebook ওপেন করা হচ্ছে" else "Opening Facebook"
            }
            if (text.contains("google") || text.contains("গুগল")) {
                return JarvisAction.Navigate("https://www.google.com", "Google") to if (isBn) "Google ওপেন করা হচ্ছে" else "Opening Google"
            }
            if (text.contains("wikipedia") || text.contains("উইকিপিডিয়া")) {
                return JarvisAction.Navigate("https://www.wikipedia.org", "Wikipedia") to if (isBn) "Wikipedia ওপেন করা হচ্ছে" else "Opening Wikipedia"
            }
        }

        // 3. Tab and Page Control
        if (text.contains("reload") || text.contains("refresh") || text.contains("রিফ্রেশ") || text.contains("রিলোড")) {
            return JarvisAction.RefreshPage to if (isBn) "পেজটি রিফ্রেশ করা হচ্ছে" else "Refreshing page"
        }
        if (text == "back" || text == "go back" || text.contains("পিছনে যাও") || text.contains("আগের পেজে যাও")) {
            return JarvisAction.GoBack to if (isBn) "পূর্ববর্তী পেজে ফিরে যাওয়া হলো" else "Going back"
        }
        if (text == "forward" || text == "go forward" || text.contains("সামনে যাও") || text.contains("পরের পেজে যাও")) {
            return JarvisAction.GoForward to if (isBn) "পরবর্তী পেজে যাওয়া হলো" else "Going forward"
        }
        if (text.contains("close tab") || text.contains("ট্যাব বন্ধ") || text.contains("কেটে দাও")) {
            return JarvisAction.CloseTab to if (isBn) "ট্যাবটি বন্ধ করা হলো" else "Closing tab"
        }
        if (text.contains("new tab") || text.contains("নতুন ট্যাব")) {
            return JarvisAction.NewTab() to if (isBn) "নতুন ট্যাব খোলা হলো" else "Opening new tab"
        }

        return null
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
            var actionType = ""
            var target = ""
            var voiceReply = ""

            try {
                val mapAdapter = moshi.adapter(Map::class.java)
                val map = mapAdapter.fromJson(clean) as? Map<*, *>
                if (map != null) {
                    actionType = (map["action"] as? String) ?: ""
                    target = (map["target"] as? String) ?: ""
                    voiceReply = (map["voice_response"] as? String) ?: (map["voiceResponse"] as? String) ?: (map["response"] as? String) ?: ""
                }
            } catch (e: Exception) {
                // Regex fallback for malformed JSON
                val actionMatch = Regex(""""action"\s*:\s*"([^"]+)"""").find(clean)
                actionType = actionMatch?.groupValues?.get(1) ?: ""

                val targetMatch = Regex(""""target"\s*:\s*"([^"]*)"""").find(clean)
                target = targetMatch?.groupValues?.get(1) ?: ""

                val voiceMatch = Regex(""""voice_response"\s*:\s*"([^"]+)"""").find(clean)
                voiceReply = voiceMatch?.groupValues?.get(1) ?: ""
            }

            val effectiveReply = if (voiceReply.isNotBlank()) voiceReply else if (containsBengali(originalCommand)) "সম্পন্ন করা হলো" else "Done."

            val action: JarvisAction = when (actionType.uppercase().trim()) {
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
                "SPEAK" -> JarvisAction.SpeakOnly(effectiveReply)
                else -> {
                    // If the response is pure conversational text
                    if (voiceReply.isNotBlank()) JarvisAction.SpeakOnly(voiceReply)
                    else parseLocalFallback(originalCommand).first
                }
            }
            action to effectiveReply
        } catch (e: Exception) {
            parseLocalFallback(originalCommand)
        }
    }

    private fun parseLocalFallback(raw: String): Pair<JarvisAction, String> {
        val text = raw.lowercase().trim()
        val isBn = containsBengali(raw)

        // Check immediate browser actions
        val instant = checkImmediateLocalAction(raw)
        if (instant != null) return instant

        // Conversational greetings & general chit-chat (Speak instead of Search!)
        if (text.contains("কেমন আছো") || text.contains("how are you")) {
            return JarvisAction.SpeakOnly(if (isBn) "আমি ভালো আছি! আপনাকে কীভাবে সাহায্য করতে পারি?" else "I am doing great! How can I assist you today?") to if (isBn) "আমি ভালো আছি! আপনাকে কীভাবে সাহায্য করতে পারি?" else "I am doing great! How can I assist you today?"
        }
        if (text.contains("তুমি কে") || text.contains("তোমার নাম কি") || text.contains("who are you") || text.contains("what is your name")) {
            return JarvisAction.SpeakOnly(if (isBn) "আমি অরেন ব্রাউজারের ভয়েস এআই অ্যাসিস্ট্যান্ট। আমি আপনার নির্দেশে ব্রাউজিং ও যেকোনো প্রশ্নের উত্তর দিতে পারি।" else "I am Auren Browser Voice AI Assistant, ready to help you browse and answer questions.") to if (isBn) "আমি অরেন ব্রাউজারের ভয়েস এআই অ্যাসিস্ট্যান্ট।" else "I am Auren Browser Voice AI Assistant."
        }
        if (text.contains("হ্যালো") || text.contains("হাই") || text.contains("hello") || text.contains("hey")) {
            return JarvisAction.SpeakOnly(if (isBn) "হ্যালো! বলুন কী করতে পারি?" else "Hello! How can I help you?") to if (isBn) "হ্যালো! বলুন কী করতে পারি?" else "Hello! How can I help you?"
        }

        // Search only when user asks to search or explicitly looks up something
        if (text.startsWith("search") || text.contains("সার্চ") || text.contains("খুঁজে বের করো") || text.contains("গুগলে দেখো")) {
            val q = raw.replace(Regex("""(?i)search( for)?|সার্চ করো|খুঁজে বের করো|গুগলে দেখো"""), "").trim()
            val effectiveQ = q.ifBlank { raw }
            return JarvisAction.Search(effectiveQ) to if (isBn) "\"$effectiveQ\" অনুসন্ধান করা হচ্ছে" else "Searching for $effectiveQ"
        }

        // For any other voice query, speak back friendly response
        val spokenReply = if (isBn) "\"$raw\" সম্পর্কে বিস্তারিত জানতে সার্চ করতে পারেন অথবা আবার বলুন।" else "Understood: \"$raw\". How would you like to proceed?"
        return JarvisAction.SpeakOnly(spokenReply) to spokenReply
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
