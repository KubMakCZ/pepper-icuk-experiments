# Pepper ICUK Experiments 🤖

Repozitář pro experimenty a projekty s humanoidním robotem **Pepper** (SoftBank Robotics) pro **Inovační centrum Ústeckého kraje (ICUK)**. Centrální místo pro vývoj aplikací rozšiřujících schopnosti robota v oblasti interakce s lidmi, vzdělávání a prezentace.

---

> Pro AI asistenty / vibecoding: začněte v [`AGENTS.md`](./AGENTS.md). Mapa projektů: [`docs/PROJEKTY.md`](./docs/PROJEKTY.md).

## 📌 Struktura repozitáře

Každá složka je **samostatný Android/Gradle projekt** – otevírejte v Android Studiu vždy konkrétní složku, ne kořen repozitáře.

| Projekt | Jazyk | Stav | Popis |
|---------|-------|------|-------|
| **[testRubby2](./testRubby2)** | Java | ✅ Stabilní šablona | Referenční projekt v češtině – ukázky řeči, animací a životního cyklu. **Nové projekty zakládejte kopií této složky.** |
| **[rubby_llm_experiment](./rubby_llm_experiment)** | Java | 🚧 Aktivní vývoj | Experiment: propojení Peppera s LLM (Gemini 2.5 Flash a OpenRouter) pro plynulou konverzaci v češtině, wakeword „Rubby“. |
| **[ICUK_Rubby_app-5_dod_ujep_02_24](./ICUK_Rubby_app-5_dod_ujep_02_24)** | Java | ✅ Produkce | Prezentační aplikace ICUK – robot představuje 10 témat (coworking, sály, kanceláře…) pomocí fragmentové navigace a QiChat. |

---

## 🛠️ Jednotné vývojové prostředí

Všechny projekty jsou optimalizovány pro specifickou kombinaci nástrojů zaručující stabilitu na reálném robotovi i v emulátoru. **Neměňte verze Gradle ani AGP**, pokud k tomu není vážný důvod – novější verze rozbíjí Pepper SDK plugin.

### Požadavky (Hard Requirements)

| Nástroj | Verze |
|---------|-------|
| **Android Studio** | **2021.1.1 (Bumblebee) Patch 3** |
| **Pepper SDK Plugin** | Instalace přes *File → Settings → Plugins* |
| **Java** | JDK 11 pro běh Studia, **Java 8** v Project Structure (kompatibilita s robotem) |
| **QiSDK** | 1.7.5 (`qisdk` + `qisdk-design`) |
| **Gradle** | 7.0.2 (šablona) – liší se dle projektu |
| **AGP** | 7.0.4 (šablona) – liší se dle projektu |
| **Target platforma** | Android 6.0 Marshmallow (API 23) – tablet robota |

> ⚠️ **API 23 omezení:** Žádné Java 8 Streams, žádné moderní Android SDK. Knihovny musí podporovat minSdk 23.

### Build a spuštění

```sh
# Otevřete konkrétní projekt (např. testRubby2/) v Android Studiu, NEBO:
cd testRubby2
./gradlew assembleDebug      # sestavení debug APK
./gradlew installDebug        # nasazení na emulátor / připojeného robota
```

### Spuštění emulátoru
1. V Android Studiu: *Tools → Pepper SDK* → vytvořte/spusťte Robot Emulator.
2. Klikněte na **Connect** v Pepper toolbaru.
3. Spusťte aplikaci zeleným tlačítkem Run → vyberte emulátor.

---

## 📐 Klíčové konvence

### Jazyk a lokalizace
Veškerý user-facing text a řeč je v **češtině**. Vždy inicializujte:
```java
new Locale(Language.CZECH, Region.CZECH_REPUBLIC)
```

### Asynchronní akce robota
Nikdy neblokujte hlavní vlákno. Vždy používejte `buildAsync()` / `async()`:
```java
SayBuilder.with(qiContext)
    .withText("Ahoj!")
    .buildAsync()
    .thenConsume(say -> say.async().run());
```

### Životní cyklus (Robot Focus)
- Implementujte `RobotLifecycleCallbacks` nebo děděte z `RobotActivity`.
- Akce robota (Say, Animate, Listen, Chat) spouštějte **výhradně** v `onRobotFocusGained(QiContext)` – před tím je `qiContext` null.
- Uvolněte zdroje v `onRobotFocusLost()`.
- Pokud robot ztratí focus (např. dotyk na notifikační lištu), konverzace se přeruší. Aplikace musí přežít ztrátu a znovuzískání focusu.

### QiChat (pravidlová konverzace)
- Konverzační soubory `.top` v `res/raw/` nebo `assets/`.
- Custom executory (`FragmentExecutor`, `VariableExecutor`) propojují QiChat akce s Android UI.
- Varianty slov pro rozpoznávání v češtině: `["Coworking" "Koworking" "Kowrkin"]`.

### Animace
- Soubory `.qianim` uložené v `res/raw/`.
- Pro LLM experiment: pool animací `BodyTalk/BodyTalk_1` až `BodyTalk_10` s náhodným výběrem.

### Emulátor vs. reálný robot
- V emulátoru je speech recognition nestabilní → testujte řeč pouze na reálném hardware.
- `testRubby2` detekuje emulátor a automaticky vypíná listening.
- Deadlocky v emulátoru: preferujte manuální triggery (tlačítka) nad kontinuálním chatem.

### Rozlišení tabletu
Pro správné zobrazení v Layout Editoru nastavte profil:
- **Resolution:** 1280 × 800 px
- **Density:** 213 (tvdpi)

---

## 🔬 Projekty – podrobnosti

### testRubby2 – Stabilní šablona

Demonstrace core funkcí robota v češtině s minimálním rizikem pádu.

**Funkce:**
- **Manuální řeč:** Tlačítko pro pozdrav pomocí `SayBuilder`.
- **Animace:** Tanec ze souboru `res/raw/dance_b003.qianim`.
- **Chat:** Základní QiChat v `assets/conversation.top` (pro rozšíření).
- **Životní cyklus:** Vzorové ošetření `RobotLifecycleCallbacks`.

**Důležité soubory:**
- `MainActivity.java` – asynchronní inicializace akcí
- `res/raw/dance_b003.qianim` – ukázkový taneček
- `assets/conversation.top` – konverzační soubor

**Jak založit nový projekt z šablony:**
1. Zkopírujte celou složku `testRubby2` a přejmenujte ji.
2. Upravte názvy v `settings.gradle` a `app/build.gradle`.
3. Změňte `app_name` v `res/values/strings.xml`.

---

### rubby_llm_experiment – Pepper × LLM

Wrapper aplikace propojující Peppera s **Google Gemini 1.5 Flash** API pro dynamickou konverzaci v češtině. Projekt je funkční (LLM, wakeword, animace, logování); detaily viz `rubby_llm_experiment/README.md` a `CLAUDE.md`.

**Architektura (cílový stav):**

```
┌────────────────────────────────────────────────┐
│                 MainActivity                    │
│         implements RobotLifecycleCallbacks      │
├────────────────────────────────────────────────┤
│  QiChat      → SpeechRecognizer → GeminiService │
│  WakeWord      (Android, cs-CZ)   (Retrofit)    │
│                                        │        │
│  AnimateBuilder ← QiSDK Say  ← ChatHistory      │
│                                (max 10 turnů)   │
├────────────────────────────────────────────────┤
│  Tls12SocketFactory  │  ConversationManager     │
│  (SSL patch API 23)  │  (stavový automat)        │
└────────────────────────────────────────────────┘
```

**Stavový automat:**
```
IDLE ──(wake word)──▶ LISTENING ──(STT)──▶ PROCESSING
  ▲                                            │
  └──────(loop/timeout 30s)── SPEAKING ◀── API RESPONSE
```

**Kritická omezení:**
- OkHttp max `3.12.13` (řada 3.12.x) – novější 4.x nefunguje na API 23
- Retrofit `2.9.0` s Gson convertorem
- TLS 1.2 je na Android 6.0 ve výchozím stavu vypnutý → nutný custom `Tls12SocketFactory`
- Max 10 turnů konverzační historie (RAM limit tabletu)
- LLM odpovědi čistit od Markdown (`*`, `#`, `` ` ``) – Pepper je čte nahlas

**Dependencies (app/build.gradle):**
```groovy
// QiSDK
implementation 'com.aldebaran:qisdk:1.7.5'
implementation 'com.aldebaran:qisdk-design:1.7.3'

// Networking (API 23 kompatibilní)
implementation 'com.squareup.okhttp3:okhttp:3.12.13'
implementation 'com.squareup.okhttp3:logging-interceptor:3.12.13'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.google.code.gson:gson:2.10.1'

// Google Play Services (ProviderInstaller – TLS patch)
implementation 'com.google.android.gms:play-services-base:18.2.0'
```

> QiSDK repository musí být v `settings.gradle`:
> ```groovy
> maven { url 'https://qisdk.softbankrobotics.com/sdk/maven' }
> ```

**System prompt (persona robota):**
```
Jsi robot Pepper. Jsi fyzický, humanoidní robot vyrobený firmou SoftBank Robotics.
Nacházíš se v kancelářském prostředí. Jsi nápomocný, zdvořilý a přátelský.
Mluvíš plynně česky, spisovně.
Tvé odpovědi MUSÍ být stručné – maximálně 2-3 věty, protože tvá syntéza řeči je pomalá.
Nepoužívej emotikony, hvězdičky, hashtahy ani žádné formátování.
Pokud neznáš odpověď, přiznej to upřímně.
```

**Cílová adresářová struktura:**
```
app/src/main/java/cz/kubmak/rubby_llm_exp/
├── MainActivity.java              # Hlavní Activity + RobotLifecycleCallbacks
├── network/
│   ├── Tls12SocketFactory.java    # TLS 1.2 SSL patch
│   └── NetworkClient.java         # OkHttpClient singleton
├── llm/
│   ├── ILlmService.java           # Interface pro LLM služby
│   ├── GeminiService.java         # Implementace pro Gemini API
│   ├── GeminiApiInterface.java    # Retrofit endpoint
│   └── models/                    # Gson data třídy
├── conversation/
│   ├── ChatHistoryManager.java    # FIFO buffer (max 10 výměn)
│   ├── ConversationState.java     # Enum: IDLE, LISTENING, PROCESSING, SPEAKING
│   └── ConversationManager.java   # Stavový automat
├── audio/
│   ├── SpeechInput.java           # Android SpeechRecognizer wrapper
│   └── RobotSpeaker.java         # QiSDK Say + Animate helper
└── util/
    └── TextCleaner.java           # Markdown stripping
```

**Latence mitigace:**
| Technika | Detail |
|----------|--------|
| Thinking animace | Ihned po konci STT spustit animaci přemýšlení |
| Filler fráze | Paralelně s API: `Say("Moment...")` |
| Timeout | Max 10s na API, pak fallback chybová hláška |

---

### ICUK_Rubby_app – Produkční prezentační aplikace

Fragmentová aplikace v **Kotlinu** – robot představuje služby ICUK Space Hradební.

**Fragmenty (navigace):**

| # | Fragment | Téma | Kontakt |
|---|---------|------|---------|
| 3 | `frag_screen_three` | Úvodní řeč | — |
| 4 | `frag_screen_four` | Eventový sál | fronc@icuk.cz |
| 5 | `frag_screen_five` | Coworking | soukupova@icuk.cz |
| 6 | `frag_screen_sex` | Kanceláře a zasedací místnosti | soukupova@icuk.cz |
| 7 | `frag_screen_seven` | Virtuální sídlo | cavdarova@icuk.cz |
| 8 | `frag_screen_eight` | Pod jednou střechou | — |
| 9 | `frag_screen_nine` | Co chystáme v ICUK Space | — |
| 10 | `frag_screen_ten` | Co dělá ICUK | icuk.cz, space.icuk.cz |

**Speciální funkce:**
- **Human awareness** – detekce přítomnosti člověka, reset timeru nečinnosti.
- **Inactivity timeout** – 5 min bez interakce → návrat na SplashFragment.
- **QiChat executory:** `FragmentExecutor` (přepínání fragmentů), `VariableExecutor` (nastavení QiVariables).
- Varianty slov pro rozpoznávání: `["Coworking" "Koworking" "Kowrkin" "Kovrking"]`.

---

## 🔑 API klíče (LLM experiment)

Klíče se ukládají do `local.properties` (gitignored) a čtou přes `BuildConfig`:

```properties
# local.properties
GEMINI_API_KEY=AIzaSy...tvůj_klíč...
DEEPSEEK_API_KEY=sk-...        # volitelné (backup)
OPENAI_API_KEY=sk-...          # volitelné (lepší STT)
```

V `app/build.gradle` (`defaultConfig`):
```groovy
def localProps = new Properties()
def localPropsFile = rootProject.file('local.properties')
if (localPropsFile.exists()) {
    localProps.load(new FileInputStream(localPropsFile))
}
buildConfigField "String", "GEMINI_API_KEY",
    "\"${localProps.getProperty('GEMINI_API_KEY', '')}\""
```

V Java kódu: `String apiKey = BuildConfig.GEMINI_API_KEY;`

### Dostupné LLM služby

| Služba | Nutnost | Cena | Free Tier |
|--------|---------|------|-----------|
| **Google Gemini 1.5 Flash** | ✅ Povinné | Zdarma | 15 RPM, 1M TPM, 1500 RPD |
| **DeepSeek V3** | ⬜ Backup | ~$0.14/1M tokenů | Ne |
| **OpenAI Whisper** | ⬜ Lepší STT | $0.006/min | Ne |

**Pro začátek stačí JEN Gemini klíč.**

Získání: [aistudio.google.com](https://aistudio.google.com/) → Get API key → Create API key.

Ověření:
```sh
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=TVŮJ_KLÍČ" \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"Řekni ahoj česky"}]}]}'
```

> **Poznámka:** Gemini Premium předplatné (Google One AI) ≠ API kredity. Premium je pro chat na webu, API má vlastní Free Tier, který pro jednoho robota bohatě stačí.

---

## ⚠️ Troubleshooting (Řešení problémů)

### Android Studio padá při startu emulátoru
Zvyšte paměť Studia: v `studio64.exe.vmoptions` změňte `-Xmx` na **`-Xmx4096m`**.

### Emulátor je černý nebo extrémně pomalý
V Device Manageru: **Graphics → Software - GLES 2.0**.

### Chyba "qiContext is null"
Robot se ještě „neprobudil". Počkejte na log `Robot focus gained`. Pokud se neobjeví, zkontrolujte připojení přes ikonku Pepper v liště.

### SSLHandshakeException (LLM experiment)
Android 6.0 má TLS 1.2 ve výchozím stavu vypnutý. Nutný custom `Tls12SocketFactory` – viz sekci rubby_llm_experiment výše.

### Deadlocky / zamrznutí aplikace
QiSDK akce musí běžet mimo UI thread. Pokud robot nereaguje, jde pravděpodobně o deadlock v thread poolu. V emulátoru preferujte tlačítka místo kontinuálního chatu.

### STT nefunguje v češtině
Ověřte, že tablet má stažený offline cs-CZ balíček pro rozpoznávání řeči.

---

## 📋 Implementační workplan – LLM experiment

Pro kompletní plán implementace viz [`rubby_llm_experiment/WORKFLOW_PEPPER_LLM.md`](./rubby_llm_experiment/WORKFLOW_PEPPER_LLM.md).

Stručný přehled fází:
1. **Příprava projektu** – dependencies, manifest, permissions
2. **TLS 1.2 síťová vrstva** – `Tls12SocketFactory`, `NetworkClient`
3. **Gemini API služba** – Retrofit interface, Gson modely, `ILlmService`
4. **Konverzační historie** – FIFO buffer, max 10 výměn
5. **STT pipeline** – Android `SpeechRecognizer` (cs-CZ)
6. **TTS + animace** – QiSDK `SayBuilder` + paralelní `AnimateBuilder`
7. **Wake word + hlavní smyčka** – QiChat trigger, stavový automat
8. **Error handling & UX** – timeouty, filler fráze, thinking animace
9. **Konfigurace API klíčů** – `local.properties` → `BuildConfig`

---

*Spravováno pro ICUK. V případě dotazů kontaktujte správce repozitáře.*
