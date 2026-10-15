# OfflineVoiceAgent

A free native Android voice-assistant project targeting Android 8.0+ and Motorola Edge 50 Fusion.

## Implemented so far
- Kotlin Android app and warm-colour interface.
- Microphone permission flow and Android SpeechRecognizer with offline preference requested.
- Android TextToSpeech response when a compatible voice is installed.
- Tested command router for time/date, greeting, help, Settings, Contacts, Calendar, file picker, and stop-speaking commands.
- External app actions launch Android system/app intents; choosing a file uses Android's system document picker.
- GitHub Actions workflow runs unit tests, builds a debug APK, and uploads the APK as an artifact.

## Important limitations
This is **not yet a complete local-LLM agent**. Android SpeechRecognizer may use a network service despite the offline preference; offline recognition depends on the installed provider and language pack. TTS is offline only if a suitable voice is installed. No Gemma/Llama/Qwen model, Whisper runtime, Piper runtime, WhatsApp message automation, or general app-control agent is integrated yet. Those require further implementation and real-device/offline testing.

## Build and download APK using a phone
1. Open the Actions tab.
2. Select **Android APK**. Use **Run workflow**, or wait for the automatic run after a push.
3. Wait for a successful run.
4. Open that run and download **OfflineVoiceAgent-debug-apk** under Artifacts.
5. Extract the ZIP on your phone and install `app-debug.apk`. Allow installation from your browser/files app if Android asks.

This is a debug APK and does not include an LLM model. Do not describe it as a production release or as fully offline until the model runtime and offline tests are completed.

## Privacy
No custom backend or analytics is configured. Microphone access is requested when you tap the mic. Android's speech provider determines whether speech is processed locally; check its offline language-pack settings.

## Roadmap
1. Verify CI build and install on a physical device.
2. Integrate an on-device LLM and a downloadable quantized model.
3. Integrate fully local speech recognition.
4. Add a dependable offline TTS voice.
5. Add permission-gated tools with confirmation for external actions.
