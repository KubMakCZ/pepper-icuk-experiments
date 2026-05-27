# Spec: Head Touch Interaction for Rubby

## 1. Goal
Implement a natural interaction where touching the middle head sensor of the Pepper robot triggers a feedback response and starts the speech recognition process.

## 2. Interaction Flow
1. **User Action:** Touches the middle head sensor (`TouchSensor.Head/Middle`).
2. **Robot Feedback (Parallel):**
    - Play a short "notification" sound (Beep).
    - Robot says "Ano?" (using QiSDK Say).
3. **Action:** Once the robot finishes saying "Ano?", start the Android `SpeechRecognizer` (the same logic as clicking the Mic button).

## 3. Technical Approach
- **Sensor Monitoring:** Use `TouchSensor` from `QiSDK` within `MainActivity`.
- **Concurrency:** Ensure that touching the head cancels any ongoing `Say` or `Chat` actions to prioritize the new command.
- **State Management:** The interaction should only trigger if the robot is in `IDLE` or `SPEAKING` state (to allow interruption).
- **Audio Feedback:** Use `Say` for "Ano?" and investigate if a native Pepper beep is available via QiSDK or if a simple audio file is needed.

## 4. Components Involved
- `MainActivity.java`: Register the touch sensor listener and handle the logic transition to `startSpeechRecognition()`.
- `ConversationState.java`: Ensure state transitions remain consistent.

## 5. Verification Plan
- **Manual Test:** Deploy to Pepper/Emulator and simulate/perform a touch on the middle head sensor.
- **Expectation:** Robot says "Ano?", the UI status changes to "Listening...", and it captures speech.
