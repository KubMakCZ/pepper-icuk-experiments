# Copilot Instructions — Pepper ICUK Experiments

## Architecture

This is a **monorepo of Android apps for the SoftBank Pepper robot**, built with the **QiSDK** (Pepper SDK). Each top-level folder is an independent Android/Gradle project — there is no shared root build system.

| Project | Language | Purpose |
|---------|----------|---------|
| `testRubby2` | Java | Stable reference template — start new projects from this |
| `rubby_llm_experiment` | Java | WIP experiment integrating Google Gemini LLM with Pepper |
| `ICUK_Rubby_app-5_dod_ujep_02_24` | Kotlin | Production ICUK promotional app with fragment-based navigation |

All projects target **minSdk 23** (Android 6.0 — Pepper's tablet). The robot runs a locked-down Android, so Java 8 Streams and newer APIs are unavailable.

## Build

Each project is built independently. Open the specific project folder in Android Studio (not the repo root).

```sh
# Build from project root (e.g. testRubby2/)
./gradlew assembleDebug

# Deploy to Pepper emulator or connected robot
./gradlew installDebug
```

There are no shared test suites or linting configurations across the repo.

## Pinned Toolchain — Do Not Upgrade

| Tool | Version |
|------|---------|
| Android Studio | 2021.1.1 (Bumblebee) Patch 3 |
| Java (project level) | 8 |
| QiSDK | 1.7.5 |
| Gradle | 7.0.2 (template); varies per project |
| AGP | 7.0.4 (template); varies per project |

Newer Android Studio or AGP versions break the Pepper SDK plugin. Do not suggest upgrades.

## Key Conventions

### Language & Locale
All user-facing text and speech is in **Czech**. Always initialize locale as:
```java
new Locale(Language.CZECH, Region.CZECH_REPUBLIC)
```

### Asynchronous Robot Actions
Never block the main thread. Always use `buildAsync()` / `async()` for QiSDK actions (Say, Animate, Listen, Chat). The pattern is:
```java
SayBuilder.with(qiContext)
    .withText("Ahoj!")
    .buildAsync()
    .thenConsume(say -> say.async().run());
```

### Robot Lifecycle
- Extend `RobotActivity` or implement `RobotLifecycleCallbacks`.
- Only interact with the robot inside `onRobotFocusGained(QiContext)` — the `qiContext` is null before this callback.
- Release resources in `onRobotFocusLost()`.

### QiChat Dialogue
Rule-based conversations use `.top` files (QiChat scripting language) stored in `app/src/main/res/raw/`. Custom executors (`FragmentExecutor`, `VariableExecutor`) bridge QiChat actions to Android UI.

### Emulator vs. Real Robot
`testRubby2` detects the emulator and disables listening (speech recognition is unstable in the emulator). Keep this pattern when developing new features — test speech only on real hardware.

### API Keys (LLM experiment)
Store API keys in `local.properties` (gitignored) and access via `BuildConfig`. Never hardcode credentials.

### LLM Response Cleaning
Pepper's TTS reads markdown literally (e.g., says "asterisk"). Strip all markdown formatting (`*`, `#`, `` ` ``) from LLM responses before passing to `SayBuilder`.

### TLS 1.2 on API 23
Android 6.0 has TLS 1.2 disabled by default. The LLM experiment requires a custom `Tls12SocketFactory` and `ProviderInstaller.installIfNeeded()` for HTTPS to work. Without this, all API calls fail with `SSLHandshakeException`.

### Library Version Constraints (LLM experiment)
- OkHttp: max **3.12.13** (4.x requires API 26+)
- Retrofit: **2.9.0** with Gson converter
- Chat history: max **10 turns** in-memory FIFO (tablet RAM is limited)

## Further Reading

See the root [README.md](../README.md) for full project details, architecture diagrams, API key setup, implementation workplan, and troubleshooting.
