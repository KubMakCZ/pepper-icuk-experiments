# Head Touch Sensor Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the incorrect Pepper head touch sensor implementation causing compilation errors.

**Architecture:** Update imports to use the correct `com.aldebaran.qi.sdk.object.touch` package and initialize via `qiContext.getTouch()`.

**Tech Stack:** Android API 23, QiSDK (Java).

---

### Task 1: Update Imports and Variable Declarations

**Files:**
- Modify: `app/src/main/java/cz/kubmak/rubby_llm_exp/MainActivity.java`

- [ ] **Step 1: Replace sensor imports with touch imports**

```java
// Remove
import com.aldebaran.qi.sdk.object.sensor.TouchSensor;
import com.aldebaran.qi.sdk.object.sensor.SensorManager;

// Add
import com.aldebaran.qi.sdk.object.touch.Touch;
import com.aldebaran.qi.sdk.object.touch.TouchSensor;
```

- [ ] **Step 2: Update the member variable type**

The type name `TouchSensor` is the same, but it now refers to the correct package.

### Task 2: Update Initialization and Cleanup

**Files:**
- Modify: `app/src/main/java/cz/kubmak/rubby_llm_exp/MainActivity.java`

- [ ] **Step 1: Fix initialization in `onRobotFocusGained`**

```java
// Old way
SensorManager sensorManager = qiContext.getSensorManager();
headMiddleSensor = sensorManager.getTouchSensor("Head/Middle");

// New way
Touch touch = qiContext.getTouch();
headTouchSensor = touch.getSensor("Head/Touch");
```

- [ ] **Step 2: Update cleanup in `onRobotFocusLost`**

Use `removeAllOnStateChangedListeners()` on the new sensor variable.

### Task 3: Verification

- [ ] **Step 1: Run build**
Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL
