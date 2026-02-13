# Project Overview: Pepper Robot Stable Template (Czech)

This project is a specialized Android application designed for the **Pepper humanoid robot** by SoftBank Robotics. It serves as a highly stable template optimized for older development environments (Android Studio 2021.1.1) and reliable execution in both the Robot Emulator and on physical hardware.

The primary goal of the project is to demonstrate basic robot interactions:
*   **Speech (Czech):** Real-time text-to-speech using the Czech locale.
*   **Animations:** Executing QiSDK animations (e.g., dancing) from resource files.
*   **Manual Control:** A simple UI with buttons to trigger actions, which improves stability in emulator environments compared to fully autonomous chat logic.

### Key Technologies:
*   **Platform:** Android (specifically optimized for Pepper's tablet running Android 6.0 Marshmallow).
*   **SDK:** SoftBank Robotics QiSDK (version 1.7.5).
*   **Languages:** Java 8 (source compatibility) / Java 11 (build environment).
*   **Build System:** Gradle (7.0.2) with Android Gradle Plugin (7.0.4).

---

## Building and Running

### Building the Project:
To build the project and generate a debug APK, use the following command from the root directory:
```powershell
./gradlew assembleDebug
```

### Running the Project:
1.  **Preparation:** Open the project in **Android Studio 2021.1.1 (Bumblebee) Patch 3**.
2.  **Emulator Setup:** 
    *   Create or start a **Robot Emulator** from `Tools -> Pepper SDK`.
    *   Ensure the emulator's graphics are set to **Software - GLES 2.0** for stability.
3.  **Connection:** Click the **Connect** icon in the Pepper toolbar to link the IDE with the running emulator.
4.  **Deployment:** Click the green **Run** button and select the emulator as the target device.

---

## Development Conventions

*   **Asynchronous Actions:** Always use `.buildAsync()` when creating robot actions (Say, Animate, Chat) to avoid `NetworkOnMainThreadException`.
*   **Life-cycle Awareness:** Implement `RobotLifecycleCallbacks` and only trigger robot-specific actions after `onRobotFocusGained` has been called.
*   **Localization:** The project is hardcoded to support the **Czech** language. Ensure `.top` files and `SayBuilder` locales match (`Language.CZECH`, `Region.CZECH_REPUBLIC`).
*   **Resource Management:** Animations should be stored in `res/raw` as `.qianim` files. Simple greetings should be handled via `SayBuilder` for maximum reliability.

---

## Troubleshooting Instruction Context

When assisting with this project, keep these critical environmental constraints in mind:
*   **Memory Issues:** If Android Studio crashes during emulator startup, increase the IDE's heap size (`-Xmx`) to at least **4096m** in `studio64.exe.vmoptions`.
*   **Compatibility:** Do not upgrade AGP or Gradle beyond the current versions (7.0.4 / 7.0.2) as the user's specific Android Studio version (2021.1.1) will fail to synchronize the project.
*   **Deadlocks:** If the robot seems unresponsive or the app freezes, it is likely a deadlock in the QiSDK thread pool. Prefer manual triggers (buttons) over continuous background chat tasks during emulator testing.
