# OfflineVoiceAgent

A free native Android voice assistant for Android 8.0+ and devices such as Motorola Edge 50 Fusion.

## Implemented
- Kotlin Android app with microphone permission flow.
- Android SpeechRecognizer with offline preference requested; actual offline availability depends on the installed speech provider and language pack.
- Foreground **Hey Maya** wake mode while the app stays visible.
- Explicitly started background wake service using Android's microphone foreground-service type, persistent notification with a Stop action, and a wake-detected notification that opens the app. Start it with **Start background Hey Maya** while the app is open; grant microphone and notification permissions. It may continue when the app is backgrounded or removed from Recents, subject to Android/OEM battery and process-management policies. It does not auto-start after reboot and will not survive force-stop.
- Android TextToSpeech output when a compatible voice is installed.
- Basic command router for time/date, greeting, help, Settings, Contacts, Calendar and Android file picker. Contact and calendar apps can be opened, but the assistant does not yet perform full contact search or event management.
- Local notes stored in app-private preferences, with confirmation before clearing.
- Bounded conversation history (up to 20 turns) stored in app-private preferences; the latest six turns and user-approved saved notes are included as context for local LLM prompts. Use **Clear local chat history** to erase this history.
- Optional on-device LLM integration through MediaPipe LLM Inference. Use the **Import local AI model (.task)** button to select a compatible model file. The app copies it into app-private storage, initializes inference, and routes general questions to the local model.
- Conservative English/Bengali command routing with regression tests for accidental substring matches.
- Ordered, deduplicated, bounded local notes with migration from the earlier preferences format.
- Model import checks known file size, available storage, and a 6 GiB safety cap before initializing inference.
- System file picker supports local TXT, MD, CSV, JSON and LOG preview; with a loaded local model, it can summarize the selected text file on-device (maximum 1 MiB).
- Speech-recognition state guard to reduce overlapping listen attempts; command completion status is no longer overwritten immediately by the recognition callback.
- Background wake uses `SpeechRecognizer` in a user-started microphone foreground service. On Android 12+ it prefers the on-device recognition service when available; otherwise it falls back to the installed recognition provider with an offline preference request. A wake match produces a notification; a command spoken in the same recognition result is passed to the app when that notification is opened.
- Wake-mode recognition errors use capped exponential retry delays (up to 12 seconds), and the wake button resets when the app pauses.
- Model inference, replacement, unload and Activity destruction are serialized through a lifecycle lock to reduce engine close/use races.
- Unit tests for command routing and wake-phrase parsing.
- GitHub Actions tests, debug APK build, and artifact upload.

## Model compatibility and device requirements
The model must be a model package compatible with MediaPipe LLM Inference, with a `.task` extension. An arbitrary GGUF file is not compatible with this runtime. Model files can be several gigabytes; use a model that fits your phone's available RAM and storage. The app does not download models or use paid APIs.

The runtime integration is implemented in source but must still be validated with a compatible model on the physical phone. The current CI checks compile/tests and debug APK packaging; it does not prove model inference, wake-word accuracy, background service survival, or offline speech on a real device. Initialization can fail for unsupported model packaging, insufficient memory, or unsupported operators. Do not assume every model is compatible.

## Offline limitations
Background wake is transcript-based and is not equivalent to Google Assistant's low-power acoustic hotword DSP. Android's `SpeechRecognizer` is not designed as a guaranteed continuous always-on detector; repeated sessions can be interrupted, drain battery, or behave differently by device/provider. Android 14+ requires the microphone foreground-service permission/type, and Android restricts starting microphone services from the background, so the user must start wake mode from the visible app. The foreground notification is intentionally persistent and has a Stop action. Speech recognition may use network services despite the offline preference; true offline speech input depends on the installed provider and language pack. TTS is offline only if an appropriate voice is installed. Verify airplane-mode behavior on the physical device. No WhatsApp message automation or general app-control agent is included.

## Build APK on a phone
1. Open the Actions tab.
2. Select **Android APK** and run it, or wait for the workflow after a push.
3. Wait for a successful run.
4. Open that run and download **OfflineVoiceAgent-debug-apk** under Artifacts.
5. Extract the ZIP and install `app-debug.apk`. Android may ask permission to install from that source.

This is a debug APK, not a signed production release.

## Privacy
No custom backend or analytics is configured. Microphone permission is requested only for voice input. Saved notes and imported model files remain in app-private local storage unless deleted or the app is uninstalled. Speech processing location depends on the installed Android speech provider.
