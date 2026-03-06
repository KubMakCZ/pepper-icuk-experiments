# 🤖 Rubby LLM Experiment (Pepper Robot)

This project is a specialized Android "wrapper" application for the **SoftBank Robotics Pepper** robot (QiSDK). It integrates **Google Gemini 1.5 Flash** to transform the robot's deterministic behavior into a dynamic, context-aware conversational experience in Czech.

## 🏗️ Project Architecture & Technologies

- **Platform:** Android 6.0 (Marshmallow) / API Level 23.
- **Language:** Java (Required for compatibility with legacy QiSDK components).
- **Core SDKs:**
  - `com.aldebaran:qisdk`: Primary robotics SDK for Pepper's sensors, movements, and native TTS.
  - `com.aldebaran:qisdk-design`: Design components for the robot's tablet.
- **Networking:**
  - `Retrofit 2.9.0` & `OkHttp 3.12.13`: Targeted versions for API 23 compatibility.
  - `Tls12SocketFactory`: A critical manual patch to enable TLS 1.2 support on Android 6.0 for secure connection to Google APIs.
- **AI Integration:**
  - `Google Gemini 1.5 Flash`: Selected for its low latency and excellent Czech language support.
  - `ChatHistoryManager`: Manages a sliding window of the last 10 conversation turns (20 messages) to maintain context within RAM constraints.

## 🚀 Key Features

- **Czech Conversation Pipeline:**
  - **Input:** Uses Android's native `SpeechRecognizer` (cs-CZ) for open dictation.
  - **Processing:** Sends user prompts to Gemini with a specialized system instruction (Persona: "Rubby").
  - **Output:** Combines QiSDK native TTS (Eliska) with synchronized animations (e.g., hello, waving).
- **State Machine:** Manages states (`IDLE`, `LISTENING`, `PROCESSING`, `SPEAKING`) to ensure coherent interaction.
- **Hybrid TTS Fallback:** If the app loses "Robot Focus" or runs on a non-Pepper device, it automatically switches to the standard Android `TextToSpeech` engine.
- **Robust Error Handling:** Includes a retry mechanism (max 3 attempts) for API calls and handles `OutOfMemoryError` by clearing the conversation history.

## 🛠️ Building and Running

### Prerequisites
- **Android Studio:** Recommended version for API 30 target and API 23 min SDK.
- **Pepper Robot / Emulator:** Requires the QiSDK plugin for Android Studio.
- **Gemini API Key:** Obtainable from [Google AI Studio](https://aistudio.google.com/).

### Configuration
1.  Add your Gemini API key to `local.properties` (this file is gitignored):
    ```properties
    GEMINI_API_KEY=your_api_key_here
    ```
2.  The `app/build.gradle` will automatically read this key and provide it via `BuildConfig.GEMINI_API_KEY`.

### Deployment
- Build and run as a standard Android application.
- On the Pepper robot, ensure "Autonomous Life" is managed or disabled if it conflicts with the app's microphone usage.

## 📜 Development Conventions

- **Compatibility First:** Never use Java 8+ features (like Streams) or modern Android SDKs (minSdk 26+) without verifying compatibility with API 23.
- **Threading:** All network and heavy AI processing **must** occur on background threads (see `executor` in `MainActivity.java`).
- **Clean Output:** Responses from Gemini must be stripped of Markdown (e.g., `*`, `#`) using `TextCleaner` before being spoken by the robot, as the TTS engine will read these symbols literally.
- **Persona Consistency:** The robot identifies as "Rubby", a helpful mascot for ICUK s.r.o. in Ostrava. Maintain this tone in the system prompt.

## 📂 Key Files
- `MainActivity.java`: The central hub managing the robot lifecycle, UI, and conversation flow.
- `GeminiService.java`: Handles Retrofit calls, retries, and the complex system instruction.
- `Tls12SocketFactory.java`: Essential network security patch for Android 6.0.
- `ChatHistoryManager.java`: Implements the FIFO buffer for multi-turn memory.
- `wakeword.top`: (Planned/In Assets) QiChat trigger file for wake-word detection.
