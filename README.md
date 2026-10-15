# OfflineVoiceAgent

A free native Android voice-assistant project targeting Android 8.0+ and Motorola Edge 50 Fusion.

## Implemented
- Kotlin Android app with microphone permission flow.
- Android SpeechRecognizer with offline preference requested; actual offline availability depends on the installed speech provider and language pack.
- Android TextToSpeech output when a compatible voice is installed.
- Basic command router for time/date, greeting, help, Settings, Contacts, Calendar and Android file picker.
- Local notes: say or type `remember that I prefer Bengali`, `list memories`, or `what do you remember`. Data is stored in this app's private preferences on the device; it is not cloud-synced. To delete notes, use `forget all memories`, then type `confirm clear memory`.
- Unit tests for command routing and memory command parsing.
- GitHub Actions workflow to run tests, build a debug APK, and upload the artifact.

## Not yet implemented
This is **not yet a complete local-LLM agent**. No Gemma/Llama/Qwen inference runtime, Whisper runtime, Piper runtime, WhatsApp messaging automation, or general app-control agent is included. Android SpeechRecognizer may use network services despite the offline preference. Real offline operation must be tested with airplane mode on a physical device.

## Build APK on a phone
1. Open the Actions tab.
2. Select **Android APK** and run it, or wait for the workflow after a push.
3. Wait for a successful run.
4. Open that run and download **OfflineVoiceAgent-debug-apk** under Artifacts.
5. Extract the ZIP and install `app-debug.apk`. Android may ask permission to install from that source.

This is a debug APK and does not bundle an LLM model. Do not treat it as a production release.

## Privacy
No custom backend or analytics is configured. Microphone permission is requested only for voice input. Saved notes remain in app-private local preferences unless the user clears them or uninstalls the app. Speech processing location depends on the installed Android speech provider.

## Roadmap
1. Verify CI build and install on a physical device.
2. Integrate an on-device LLM runtime and model import/download flow.
3. Integrate fully local speech recognition.
4. Add a dependable offline TTS voice.
5. Add permission-gated tools with confirmation for external actions.
