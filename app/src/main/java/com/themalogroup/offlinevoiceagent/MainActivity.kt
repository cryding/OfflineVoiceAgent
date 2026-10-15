package com.themalogroup.offlinevoiceagent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var answer: TextView
    private lateinit var memory: LocalMemory
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var clearPending = false
    private var wakeModeEnabled = false
    private var awaitingWakeCommand = false
    private var recognitionInProgress = false
    @Volatile private var chatEpoch = 0L
    private val wakeHandler = Handler(Looper.getMainLooper())
    @Volatile private var llmEngine: LocalLlmEngine? = null
    private val requestAudio = 41
    private val requestFile = 42
    private val requestModel = 43
    private val prefs by lazy { getSharedPreferences("offline_agent_model", MODE_PRIVATE) }
    private val chatHistory by lazy { ChatHistoryStore(getSharedPreferences("offline_agent_chat", MODE_PRIVATE)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memory = LocalMemory(this)
        tts = TextToSpeech(this, this)
        buildUi()
        restoreModelIfPresent()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition service is unavailable."
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status.text = if (wakeModeEnabled) "Wake mode active · say Hey Maya" else "Listening…" }
                    override fun onBeginningOfSpeech() { status.text = "Hearing your voice…" }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { status.text = "Processing speech…" }
                    override fun onError(error: Int) {
                        recognitionInProgress = false
                        if (wakeModeEnabled) scheduleWakeListen(800L)
                        else status.text = "Recognition error $error. Try again or type a command."
                    }
                    override fun onResults(results: Bundle?) {
                        recognitionInProgress = false
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        transcript.text = text.ifBlank { "No speech recognized." }
                        if (wakeModeEnabled) processWakeResult(text)
                        else {
                            if (text.isNotBlank()) handleCommand(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!partial.isNullOrBlank()) transcript.text = partial
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(0xFFFFF8F0.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Your Voice AI Agent"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(0xFF171411.toInt())
        }
        status = TextView(this).apply {
            text = "Ready · Import a compatible local model to enable AI replies"; textSize = 15f
            gravity = Gravity.CENTER; setPadding(0, 14, 0, 20)
        }
        val modelButton = Button(this).apply {
            text = "📦 Import local AI model (.task)"
            setOnClickListener {
                val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                }
                try { startActivityForResult(picker, requestModel) }
                catch (_: Exception) { status.text = "Could not open the system file picker." }
            }
        }
        val unloadButton = Button(this).apply {
            text = "Unload local model"
            setOnClickListener {
                Thread {
                    try { llmEngine?.close() } catch (_: Exception) {}
                    llmEngine = null
                    runOnUiThread {
                        status.text = "Model unloaded"
                        answer.text = "Local model unloaded. Basic commands remain available."
                    }
                }.start()
            }
        }
        val clearChatButton = Button(this).apply {
            text = "Clear local chat history"
            setOnClickListener {
                chatEpoch += 1L
                chatHistory.clear()
                status.text = "Local conversation history cleared"
                answer.text = "Previous AI conversation context has been deleted from this phone."
            }
        }
        val mic = Button(this).apply {
            text = "🎙  Tap to speak"; textSize = 18f
            setOnClickListener { beginListening() }
        }
        val wakeButton = Button(this).apply {
            text = "Enable Hey Maya wake mode"
            setOnClickListener {
                if (wakeModeEnabled) {
                    wakeModeEnabled = false
                    awaitingWakeCommand = false
                    wakeHandler.removeCallbacksAndMessages(null)
                    try { speechRecognizer?.cancel() } catch (_: Exception) {}
                    recognitionInProgress = false
                    text = "Enable Hey Maya wake mode"
                    status.text = "Wake mode stopped · microphone is not listening"
                } else {
                    wakeModeEnabled = true
                    awaitingWakeCommand = false
                    text = "Stop Hey Maya wake mode"
                    status.text = "Starting wake mode… say Hey Maya"
                    beginListening()
                }
            }
        }
        transcript = TextView(this).apply {
            text = "Your speech will appear here."; textSize = 18f
            setPadding(16, 18, 16, 18); setBackgroundColor(0xFFFFE9DC.toInt())
        }
        answer = TextView(this).apply {
            text = "Try: time, date, open settings, or ask a question after importing a compatible MediaPipe .task model."
            textSize = 17f; setTextColor(0xFF4B4038.toInt()); setPadding(16, 18, 16, 18)
        }
        val input = EditText(this).apply { hint = "Ask or type a command…"; textSize = 16f }
        val run = Button(this).apply {
            text = "Run command"
            setOnClickListener {
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) { transcript.text = value; handleCommand(value) }
            }
        }
        listOf(title, status, modelButton, unloadButton, clearChatButton, mic, wakeButton, label("YOU SAID"), transcript,
            label("ASSISTANT"), answer, input, run,
            label("Model files stay on this phone. Use a MediaPipe LLM Inference compatible .task model; ordinary GGUF files are not supported by this runtime."),
            label("Offline speech depends on your installed Android speech service and language pack.")).forEach {
            content.addView(it, matchWrap())
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 12f; setTextColor(0xFFB65332.toInt()); setPadding(0, 18, 0, 8)
    }
    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 10 }

    private fun beginListening() {
        if (recognitionInProgress || isFinishing) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), requestAudio)
            return
        }
        val recognizer = speechRecognizer ?: run { status.text = "Speech recognition is unavailable."; return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            recognitionInProgress = true
            recognizer.startListening(intent)
        } catch (e: Exception) {
            recognitionInProgress = false
            status.text = "Could not start microphone: ${e.localizedMessage ?: "unknown error"}"
            if (wakeModeEnabled) scheduleWakeListen(1500L)
        }
    }

    private fun processWakeResult(recognizedText: String) {
        val spoken = recognizedText.trim()
        if (spoken.isBlank()) {
            status.text = "Wake mode active · say Hey Maya"
            scheduleWakeListen(500L)
            return
        }
        if (awaitingWakeCommand) {
            awaitingWakeCommand = false
            handleCommand(spoken)
            scheduleWakeListen(1800L)
            return
        }
        if (WakeWordParser.containsWakePhrase(spoken)) {
            val command = WakeWordParser.commandAfterWake(spoken)
            if (command.isNotBlank()) {
                handleCommand(command)
                scheduleWakeListen(1800L)
            } else {
                awaitingWakeCommand = true
                status.text = "Hey Maya activated · speak your command"
                if (ttsReady) tts?.speak("Yes? I am listening.", TextToSpeech.QUEUE_FLUSH, null, "wake-acknowledgement")
                scheduleWakeListen(1400L)
            }
        } else {
            status.text = "Wake mode active · say Hey Maya"
            scheduleWakeListen(500L)
        }
    }

    private fun scheduleWakeListen(delayMs: Long) {
        wakeHandler.removeCallbacksAndMessages(null)
        if (!wakeModeEnabled || isFinishing) return
        wakeHandler.postDelayed({
            if (wakeModeEnabled && !isFinishing) beginListening()
        }, delayMs)
    }

    private fun restoreModelIfPresent() {
        val path = prefs.getString("model_path", null) ?: return
        if (!File(path).isFile) {
            prefs.edit().remove("model_path").apply()
            return
        }
        loadModel(path)
    }

    private fun importModel(uri: Uri) {
        status.text = "Copying model into app-private storage…"
        Thread {
            var copied: File? = null
            try {
                val name = queryDisplayName(uri) ?: "local-model.task"
                if (!name.lowercase(Locale.ROOT).endsWith(".task")) {
                    throw IllegalArgumentException("Choose a MediaPipe-compatible .task model. GGUF is not supported by this runtime.")
                }
                val modelDir = File(filesDir, "models").apply { mkdirs() }
                copied = File(modelDir, "assistant-model-${System.currentTimeMillis()}.task")
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Could not read the selected file." }
                    FileOutputStream(copied).use { output -> input.copyTo(output) }
                }
                runOnUiThread { status.text = "Model copied. Initializing on-device inference…" }
                initializeModel(copied.absolutePath)
            } catch (e: Exception) {
                copied?.delete()
                runOnUiThread { status.text = "Model import failed: ${e.localizedMessage ?: "unsupported model"}" }
            }
        }.start()
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun loadModel(path: String) {
        status.text = "Loading saved local model…"
        Thread { initializeModel(path) }.start()
    }

    private fun initializeModel(path: String) {
        try {
            val next = LocalLlmEngine(this, path)
            val oldPath = prefs.getString("model_path", null)
            val old = llmEngine
            llmEngine = next
            try { old?.close() } catch (_: Exception) {}
            prefs.edit().putString("model_path", path).apply()
            if (oldPath != null && oldPath != path) File(oldPath).delete()
            runOnUiThread {
                status.text = "Local LLM loaded · inference runs on this device"
                answer.text = "AI model loaded. Ask a question or give a command."
            }
        } catch (e: Exception) {
            if (prefs.getString("model_path", null) == path) {
                prefs.edit().remove("model_path").apply()
            }
            File(path).delete()
            runOnUiThread {
                status.text = "Could not load this model. It may be incompatible or too large: ${e.localizedMessage ?: "initialization error"}"
            }
        }
    }

    private fun handleCommand(raw: String) {
        val command = CommandRouter.route(raw)
        if (command == AssistantCommand.UNKNOWN) {
            val engine = llmEngine
            if (engine != null) {
                answer.text = "Thinking locally…"
                status.text = "Generating response on device…"
                val requestEpoch = chatEpoch
                Thread {
                    try {
                        val prompt = chatHistory.buildPrompt(raw, memory.all())
                        val response = engine.generate(prompt).trim().ifBlank { "The local model returned an empty response. Try a shorter question." }
                        if (requestEpoch == chatEpoch) chatHistory.append(raw, response)
                        runOnUiThread {
                            if (requestEpoch == chatEpoch) {
                                answer.text = response
                                status.text = "Local model response ready"
                                if (ttsReady) tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "offline-agent-response")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            if (requestEpoch == chatEpoch) {
                                answer.text = "Local inference failed: ${e.localizedMessage ?: "unknown error"}"
                                status.text = "Model inference error"
                            }
                        }
                    }
                }.start()
                return
            }
        }
        val response = when (command) {
            AssistantCommand.TIME -> "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            AssistantCommand.DATE -> "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            AssistantCommand.HELLO -> "Hello! I can answer time and date questions, open phone tools, and remember notes locally."
            AssistantCommand.OPEN_SETTINGS -> launchTool(Intent(android.provider.Settings.ACTION_SETTINGS), "Opening Android Settings.")
            AssistantCommand.OPEN_CONTACTS -> launchTool(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS), "Opening Contacts.")
            AssistantCommand.OPEN_CALENDAR -> launchTool(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR), "Opening Calendar.")
            AssistantCommand.PICK_FILE -> {
                try {
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                    }, requestFile)
                    "Choose a file in Android's system file picker."
                } catch (_: Exception) { "No compatible file picker was found." }
            }
            AssistantCommand.REMEMBER -> {
                val note = CommandRouter.memoryText(raw)
                if (memory.add(note)) "I saved this note on your phone: $note" else "Tell me what you want me to remember."
            }
            AssistantCommand.RECALL, AssistantCommand.LIST_MEMORY -> {
                val notes = memory.all()
                if (notes.isEmpty()) "I don't have any saved notes yet."
                else "Here are your saved notes: " + notes.joinToString("; ")
            }
            AssistantCommand.CLEAR_MEMORY -> {
                clearPending = true
                "This will delete all saved notes on this phone. To confirm, type: confirm clear memory."
            }
            AssistantCommand.CONFIRM_CLEAR -> {
                if (clearPending) {
                    memory.clear(); clearPending = false
                    "All saved notes have been deleted from this device."
                } else "No memory deletion is pending. Say forget all memories first if you want to delete them."
            }
            AssistantCommand.STOP_SPEAKING -> { tts?.stop(); "Speech stopped." }
            AssistantCommand.HELP -> "Try time/date, open settings, open contacts, open calendar, choose file, remember a note, or ask a question with a compatible local model loaded."
            AssistantCommand.UNKNOWN -> "No local model is loaded. Import a compatible MediaPipe .task model to enable on-device AI responses."
        }
        answer.text = response
        status.text = if (command == AssistantCommand.UNKNOWN) "No matching local command" else "Command complete"
        if (ttsReady && command != AssistantCommand.STOP_SPEAKING) {
            tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "offline-agent-response")
        }
    }

    private fun launchTool(intent: Intent, successMessage: String): String {
        return try {
            if (intent.resolveActivity(packageManager) == null) "No compatible app is installed for this action."
            else { startActivity(intent); successMessage }
        } catch (_: Exception) { "Android could not open that app." }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        when (requestCode) {
            requestModel -> importModel(data.data!!)
        }
    }

    override fun onInit(code: Int) {
        ttsReady = code == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts?.setLanguage(Locale.getDefault())
            status.text = if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                "Ready · Install a supported TTS voice for speech output."
            else "Ready · TTS engine available; offline voice depends on installed voice data."
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudio && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) beginListening()
        else if (requestCode == requestAudio) status.text = "Microphone permission is required for voice input."
    }

    override fun onPause() {
        if (wakeModeEnabled) {
            wakeModeEnabled = false
            awaitingWakeCommand = false
            wakeHandler.removeCallbacksAndMessages(null)
            try { speechRecognizer?.cancel() } catch (_: Exception) {}
            recognitionInProgress = false
            status.text = "Wake mode paused when app left the screen"
        }
        super.onPause()
    }

    override fun onDestroy() {
        wakeHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        try { llmEngine?.close() } catch (_: Exception) {}
        llmEngine = null
        super.onDestroy()
    }
}
