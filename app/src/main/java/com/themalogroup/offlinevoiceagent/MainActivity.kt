package com.themalogroup.offlinevoiceagent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var answer: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val requestAudio = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition service is unavailable."
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…" }
                    override fun onBeginningOfSpeech() { status.text = "Hearing your voice…" }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { status.text = "Processing speech…" }
                    override fun onError(error: Int) { status.text = "Recognition error $error. Try again or type a command." }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        transcript.text = text.ifBlank { "No speech recognized." }
                        if (text.isNotBlank()) handleCommand(text)
                        status.text = "Ready"
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
            text = "Ready · Basic commands run on-device"; textSize = 15f
            gravity = Gravity.CENTER; setPadding(0, 14, 0, 20)
        }
        val mic = Button(this).apply {
            text = "🎙  Tap to speak"; textSize = 18f
            setOnClickListener { beginListening() }
        }
        transcript = TextView(this).apply {
            text = "Your speech will appear here."; textSize = 18f
            setPadding(16, 18, 16, 18); setBackgroundColor(0xFFFFE9DC.toInt())
        }
        answer = TextView(this).apply {
            text = "Try: What time is it? · What is today's date? · Open settings."
            textSize = 17f; setTextColor(0xFF4B4038.toInt()); setPadding(16, 18, 16, 18)
        }
        val input = EditText(this).apply { hint = "Or type a command…"; textSize = 16f }
        val run = Button(this).apply {
            text = "Run command"
            setOnClickListener {
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) { transcript.text = value; handleCommand(value) }
            }
        }
        listOf(title, status, mic, label("YOU SAID"), transcript, label("ASSISTANT"), answer, input, run,
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
        try { recognizer.startListening(intent) } catch (e: Exception) {
            status.text = "Could not start microphone: ${e.localizedMessage ?: "unknown error"}"
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT).trim()
        val response = when {
            command.contains("time") || command.contains("সময়") || command.contains("সময়") ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            command.contains("date") || command.contains("তারিখ") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            command.contains("hello") || command.contains("হ্যালো") ->
                "Hello! I can tell you the time or date, and open Android Settings."
            command.contains("open settings") || command.contains("settings") || command.contains("সেটিংস") -> {
                try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)); "Opening Android Settings." }
                catch (_: Exception) { "I could not open Settings." }
            }
            command.contains("stop speaking") -> { tts?.stop(); "Speech stopped." }
            else -> "This first build supports basic time/date questions and opening Settings. Local LLM integration is not added yet."
        }
        answer.text = response
        if (ttsReady) tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "offline-agent-response")
    }

    override fun onInit(code: Int) {
        ttsReady = code == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudio && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) beginListening()
        else if (requestCode == requestAudio) status.text = "Microphone permission is required for voice input."
    }
    override fun onDestroy() {
        speechRecognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy()
    }
}
