# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app (Java) for the **Pepper robot** (SoftBank Robotics) that connects it to **Google Gemini 2.5 Flash** for conversational AI in Czech. The robot is called **Rubby** and lives at ICUK (Inovacni centrum Usteckeho kraje) in Usti nad Labem.

## Build Commands

```bash
# Java compilation only (fast check, bypasses D8 dex step)
./gradlew compileDebugJavaWithJavac

# Full debug APK
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest
```

**Known issue:** The D8 dexer may fail with a NullPointerException on some JDK versions (unrelated to code changes). Use `compileDebugJavaWithJavac` for quick compilation verification.

## Build Constraints

- **minSdkVersion 23** (Android 6.0) - required for Pepper compatibility. Do NOT use APIs above API 30.
- **AGP 7.1.2**, **Gradle 7.2** - pinned for QiSDK compatibility. Do not upgrade.
- **Java 8** source/target compatibility. No Kotlin.
- **OkHttp 3.12.x** (last version supporting API 23). Do NOT upgrade to OkHttp 4.x.
- QiSDK maven repo: `https://qisdk.softbankrobotics.com/sdk/maven`

## Configuration

API key goes in `local.properties` (not committed):
```
GEMINI_API_KEY=your_key_here
```
Accessed at runtime via `BuildConfig.GEMINI_API_KEY`.

## Architecture

All source is under `app/src/main/java/cz/kubmak/rubby_llm_exp/`.

### Conversation Flow
```
User input (text/voice) → MainActivity.processUserInput()
  → ChatHistoryManager.addUserMessage()
  → GeminiService.generateResponse() [background thread]
  → TextCleaner.extractAnimationTag() + clean()
  → speakResponse() with animation category
    → QiSDK Say + AnimationManager (on Pepper)
    → Android TTS fallback (emulator/phone)
```

### Key Packages

- **`llm/`** - LLM integration. `ILlmService` interface allows swapping providers. `GeminiService` implements it with retry logic (3 attempts, exponential backoff for 429/503). API endpoint defined in `GeminiApiInterface` (Retrofit).
- **`animation/`** - `AnimationManager` maps category names (dance, hello, wave) to `.qianim` resource IDs. LLM triggers animations via `[ANIMACE:category]` tags in responses. To add animations: put `.qianim` in `res/raw/`, register in `AnimationManager.CATEGORIES`, add description in `getCategoryDescription()`.
- **`conversation/`** - `ChatHistoryManager` is a FIFO buffer (max 10 turns = 20 messages). `ConversationState` enum drives UI state machine (IDLE → LISTENING → PROCESSING → SPEAKING → IDLE).
- **`network/`** - `NetworkClient` singleton with TLS 1.2 patch for API 23. `Tls12SocketFactory` wraps SSLSocketFactory.
- **`util/`** - `TextCleaner` strips Markdown and `[ANIMACE:xxx]` tags from LLM output so Pepper doesn't read them aloud.

### Hybrid Mode

`MainActivity` extends `RobotActivity` (QiSDK). On Pepper, it gets `QiContext` via `onRobotFocusGained()` and uses native TTS + animations. On emulator/phone, `qiContext` stays null and it falls back to Android `SpeechRecognizer` + `TextToSpeech`.

### System Prompt

Robot personality and behavior rules are in `GeminiService.SYSTEM_PROMPT`. This includes animation category list (auto-generated from `AnimationManager.getCategoriesForPrompt()`). Edit this to change Rubby's persona, location, or event context.

## Language

All code comments, UI strings, log messages, and commit messages are in Czech. Follow this convention.
