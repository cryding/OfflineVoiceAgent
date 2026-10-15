# OfflineVoiceAgent

A free native Android voice assistant for Android 8.0+ and devices such as Motorola Edge 50 Fusion.

## Implemented
- Kotlin Android app with microphone permission flow.
- Android SpeechRecognizer with offline preference requested; actual offline availability depends on the installed speech provider and language pack.
- Android TextToSpeech output when a compatible voice is installed.
- Basic command router for time/date, greeting, help, Settings, Contacts, Calendar and Android file picker.
- Local notes stored in app-private preferences, with confirmation before clearing.
- Bounded conversation history (up to 20 turns) stored in app-private preferences; the latest six turns and user-approved saved notes are included as context for local LLM prompts. Use **Clear local chat history** to erase this history.
- Optional on-device LLM integration through MediaPipe LLM Inference. Use the **Import local AI model (.task)** button to select a compatible model file. The app copies it into app-private storage, initializes inference, and routes general questions to the local model.
- Unit tests for command routing and memory commands.
- GitHub Actions tests, debug APK build, and artifact upload.

## Model compatibility and device requirements
The model must be a model package compatible with MediaPipe LLM Inference, with a `.task` extension. An arbitrary GGUF file is not compatible with this runtime. Model files can be several gigabytes; use a model that fits your phone's available RAM and storage. The app does not download models or use paid APIs.

The runtime integration is implemented in source but must still be validated with a compatible model on the physical phone. Initialization can fail for unsupported model packaging, insufficient memory, or unsupported operators. Do not assume every model is compatible.

## Offline limitations
Speech recognition may use network services despite the offline preference; true offline speech input depends on the installed provider and language pack. TTS is offline only if an appropriate voice is installed. Verify airplane-mode behavior on the physical device. No WhatsApp message automation or general app-control agent is included.

## Build APK on a phone
1. Open the Actions tab.
2. Select **Android APK** and run it, or wait for the workflow after a push.
3. Wait for a successful run.
4. Open that run and download **OfflineVoiceAgent-debug-apk** under Artifacts.
5. Extract the ZIP and install `app-debug.apk`. Android may ask permission to install from that source.

This is a debug APK, not a signed production release.

## Privacy
No custom backend or analytics is configured. Microphone permission is requested only for voice input. Saved notes and imported model files remain in app-private local storage unless deleted or the app is uninstalled. Speech processing location depends on the installed Android speech provider.
