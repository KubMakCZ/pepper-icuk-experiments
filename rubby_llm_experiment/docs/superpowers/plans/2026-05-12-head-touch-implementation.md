# Head Touch Interaction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Head Touch sensor interaction on Pepper robot to trigger speech recognition.

**Architecture:** Use QiSDK `Touch` service to get the `Head/Touch` sensor, add a listener to detect touch events, and trigger the existing `startSpeechRecognition()` method when touched.

**Tech Stack:** Java, QiSDK (Touch API), Android.

---

### Task 1: Add Imports and Member Variable

**Files:**
- Modify: `app/src/main/java/cz/kubmak/rubby_llm_exp/MainActivity.java`

- [ ] **Step 1: Add imports for Touch and TouchSensor**
```java
import com.aldebaran.qi.sdk.object.touch.Touch;
import com.aldebaran.qi.sdk.object.touch.TouchSensor;
```

- [ ] **Step 2: Add headTouchSensor member variable**
```java
    // QiSDK sensors
    private TouchSensor headTouchSensor;
```

### Task 2: Initialize Sensor in onRobotFocusGained

**Files:**
- Modify: `app/src/main/java/cz/kubmak/rubby_llm_exp/MainActivity.java`

- [ ] **Step 1: Initialize headTouchSensor and add listener**
```java
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        
        // Initialize Head Touch Sensor
        Touch touch = qiContext.getTouch();
        headTouchSensor = touch.getSensor("Head/Touch");
        headTouchSensor.addOnStateChangedListener(touchState -> {
            if (touchState.getTouched()) {
                Log.i(TAG, "Head sensor touched!");
                runOnUiThread(this::onHeadTouched);
            }
        });

        Log.i(TAG, "Robot Focus GAINED");
        // ... (rest of existing code)
    }
```

### Task 3: Implement onHeadTouched Reaction Method

**Files:**
- Modify: `app/src/main/java/cz/kubmak/rubby_llm_exp/MainActivity.java`

- [ ] **Step 1: Add onHeadTouched method**
```java
    private void onHeadTouched() {
        if (currentState == ConversationState.PROCESSING || currentState == ConversationState.LISTENING) {
            return;
        }

        executor.execute(() -> {
            try {
                // Feedback: Say "Ano?"
                if (qiContext != null) {
                    Say say = SayBuilder.with(qiContext)
                            .withText("Ano?")
                            .build();
                    say.run();
                }

                // Start listening
                runOnUiThread(this::startSpeechRecognition);
            } catch (Exception e) {
                Log.e(TAG, "Error in head touch response: " + e.getMessage());
            }
        });
    }
```

### Task 4: Cleanup in onRobotFocusLost

**Files:**
- Modify: `app/src/main/java/cz/kubmak/rubby_llm_exp/MainActivity.java`

- [ ] **Step 1: Remove listeners in onRobotFocusLost**
```java
    @Override
    public void onRobotFocusLost() {
        if (headTouchSensor != null) {
            headTouchSensor.removeAllOnStateChangedListeners();
            headTouchSensor = null;
        }
        this.qiContext = null;
        // ... (rest of existing code)
    }
```

### Task 5: Verification

- [ ] **Step 1: Verify compilation**
Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Manual verification (simulated or real robot)**
When the robot has focus and is IDLE, touching the head should make it say "Ano?" and start listening.
