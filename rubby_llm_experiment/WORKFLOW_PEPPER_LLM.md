# 🤖 Pepper LLM Wrapper – Implementační Workflow & Instrukce pro Agenty

## Přehled projektu

**Cíl:** Vytvořit Android aplikaci (wrapper) pro robota **SoftBank Pepper (QiSDK)**, která propojí robota s **Google Gemini 1.5 Flash** API a umožní mu vést plynulou konverzaci v češtině.

**Stav projektu:** Existuje bare-bones Android Java projekt (`cz.kubmak.rubby_llm_exp`) s prázdnou `MainActivity` a `minSdk 23`. Vše je třeba implementovat od nuly.

---

## Kritická omezení (MUSÍ být respektována ve VŠECH fázích)

| Omezení | Detail |
|---|---|
| **Android API** | API Level 23 (Android 6.0 Marshmallow) – ŽÁDNÉ Java 8 Streams, ŽÁDNÉ moderní SDK |
| **Jazyk kódu** | Java (projekt je v Javě, NE Kotlin) |
| **OkHttp verze** | Maximálně `3.12.13` (řada 3.12.x) – novější 4.x+ může být problematická |
| **Retrofit verze** | `2.9.0` s Gson konvertorem |
| **TLS 1.2** | Android 6.0 má TLS 1.2 vypnutý ve výchozím nastavení – MUSÍ se patchovat ručně |
| **QiSDK** | `com.aldebaran:qisdk` a `com.aldebaran:qisdk-design` – robot focus lifecycle |
| **RAM** | Omezená paměť tabletu – max 10 turnů konverzační historie, žádné velké buffery |
| **Package** | `cz.kubmak.rubby_llm_exp` |
| **LLM služba** | Google Gemini 1.5 Flash (Free Tier, endpoint `generativelanguage.googleapis.com`) |
| **Jazyk robota** | Čeština (cs-CZ) pro STT i TTS |

---

## Architektura aplikace

```
┌─────────────────────────────────────────────────┐
│                  MainActivity                    │
│          implements RobotLifecycleCallbacks       │
├─────────────────────────────────────────────────┤
│                                                   │
│  ┌──────────┐   ┌──────────┐   ┌──────────────┐ │
│  │ QiChat   │──▶│ Speech   │──▶│ GeminiService│ │
│  │ WakeWord │   │Recognizer│   │ (Retrofit)   │ │
│  │ Listener │   │ (Android)│   │              │ │
│  └──────────┘   └──────────┘   └──────┬───────┘ │
│                                        │         │
│                                        ▼         │
│  ┌──────────┐   ┌──────────┐   ┌──────────────┐ │
│  │ Animate  │◀──│ QiSDK    │◀──│ ChatHistory  │ │
│  │ Builder  │   │   Say    │   │ (max 10 turns│ │
│  └──────────┘   └──────────┘   └──────────────┘ │
│                                                   │
├─────────────────────────────────────────────────┤
│  Tls12SocketFactory  │  ConversationManager      │
│  (SSL patch)         │  (state machine)           │
└─────────────────────────────────────────────────┘
```

### Datový tok (Stavový automat)

```
IDLE ──(wake word)──▶ LISTENING ──(STT result)──▶ PROCESSING
  ▲                                                    │
  │                                                    ▼
  └──────────(loop/timeout)──── SPEAKING ◀────── API RESPONSE
```

1. **IDLE:** QiChat naslouchá na klíčové slovo ("Pepper" / "Haló")
2. **LISTENING:** Android `SpeechRecognizer` zachytí volný text v cs-CZ
3. **PROCESSING:** Text → přidání do historie → JSON payload → Gemini API POST
4. **SPEAKING:** Odpověď očištěna od Markdown → `SayBuilder` + `AnimateBuilder` paralelně
5. **Zpět do LISTENING** (aktivní konverzace) nebo **IDLE** (timeout)

---

## Implementační fáze (Workplan)

### FÁZE 0: Příprava projektu (build.gradle + manifest)
- [ ] Přidat QiSDK repository a závislosti do `settings.gradle` a `app/build.gradle`
- [ ] Přidat závislosti: OkHttp 3.12.13, Retrofit 2.9.0, Gson converter 2.9.0
- [ ] Povolit Java 8 `compileOptions` (source/target compatibility) – už je
- [ ] Přidat permissions do `AndroidManifest.xml`: `INTERNET`, `RECORD_AUDIO`
- [ ] Přidat metadata pro QiSDK robot app do manifestu
- [ ] Ověřit že se projekt buildí

### FÁZE 1: TLS 1.2 síťová vrstva
- [ ] Vytvořit třídu `Tls12SocketFactory` – custom `SSLSocketFactory` co forcuje TLS 1.2
- [ ] Vytvořit třídu `NetworkClient` – singleton poskytující nakonfigurovaný `OkHttpClient` s TLS patchem
- [ ] Přidat `ProviderInstaller.installIfNeeded()` volání v `onCreate` jako pojistku
- [ ] Přidat OkHttp logging interceptor pro debug

### FÁZE 2: Gemini API služba
- [ ] Vytvořit rozhraní `ILlmService` s metodou `generateResponse(List<Message>): String`
- [ ] Vytvořit data třídy pro Gemini API request/response (Gson modely):
  - `GeminiRequest` (contents, systemInstruction, generationConfig)
  - `GeminiResponse` (candidates → content → parts → text)
  - `Content` (role, parts)
  - `Part` (text)
- [ ] Vytvořit `GeminiApiInterface` (Retrofit interface) – POST endpoint
- [ ] Vytvořit `GeminiService implements ILlmService` – sestavení requestu, parsování odpovědi
- [ ] System prompt: *"Jsi robot Pepper. Odpovídej česky, stručně a spisovně (max 2-3 věty). Jsi fyzický humanoidní robot, ne AI asistent. Nepoužívej emotikony ani Markdown formátování."*
- [ ] API klíč: načítat z `BuildConfig` nebo `local.properties` (NIKDY hardcoded v kódu)

### FÁZE 3: Konverzační historie
- [ ] Vytvořit třídu `ChatHistoryManager`
- [ ] FIFO buffer – max 10 výměn (20 zpráv: 10 user + 10 model)
- [ ] Metody: `addUserMessage(String)`, `addModelMessage(String)`, `getHistory(): List<Content>`, `clear()`
- [ ] Žádná perzistence (pouze in-memory)

### FÁZE 4: Audio Pipeline – Speech-to-Text (STT)
- [ ] Implementovat volání Android `SpeechRecognizer` s locale `cs-CZ`
- [ ] Spouštění přes `Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)`
- [ ] Callback `onResults` → předání textu do processing pipeline
- [ ] Callback `onError` → robot řekne "Nerozuměl jsem, můžeš to zopakovat?"
- [ ] Vizuální indikace na tabletu (UI) že robot poslouchá

### FÁZE 5: Audio Pipeline – Text-to-Speech (TTS) + Animace
- [ ] Použít **nativní QiSDK TTS** – `SayBuilder.with(qiContext).withText(text).build()`
- [ ] Vytvořit helper pro čištění textu od Markdown značek (`*`, `#`, `` ` ``, `_`)
- [ ] Implementovat paralelní spuštění animace (`AnimateBuilder`) během mluvení
- [ ] Pool animací: `BodyTalk/BodyTalk_1` až `BodyTalk_10` – náhodný výběr
- [ ] Filler fráze během čekání na API: "Hmm...", "Moment...", "To je zajímavá otázka..."

### FÁZE 6: Wake Word + Hlavní smyčka
- [ ] Vytvořit QiChat `.top` soubor pro detekci klíčového slova
- [ ] Implementovat `RobotLifecycleCallbacks` (onRobotFocusGained/Lost/Refused)
- [ ] Stavový automat: IDLE → LISTENING → PROCESSING → SPEAKING → loop
- [ ] Timeout neaktivity (30s) → návrat do IDLE
- [ ] Zvuková indikace (pípnutí) při přechodu z IDLE do LISTENING

### FÁZE 7: Error handling & UX
- [ ] API chyba / timeout → robot řekne "Omlouvám se, došlo k chybě spojení."
- [ ] `OutOfMemoryError` catch → graceful degradace (vyčistit historii, pokračovat)
- [ ] Ztráta/znovuzískání Robot Focus → obnovení stavu konverzace
- [ ] "Thinking" animace během API volání (vizuální feedback)
- [ ] UI na tabletu: zobrazení posledních zpráv + stav robota

### FÁZE 8: Konfigurace & bezpečnost API klíče
- [ ] API klíč v `local.properties` (gitignored): `GEMINI_API_KEY=xxx`
- [ ] Čtení klíče přes `BuildConfig.GEMINI_API_KEY` (gradle buildConfigField)
- [ ] Ověřit že `.gitignore` obsahuje `local.properties`

### FÁZE 9: Testování a ladění
- [ ] Ověřit TLS spojení s Gemini API (OkHttp logging)
- [ ] Otestovat celý conversational loop
- [ ] Ověřit český STT přesnost
- [ ] Ověřit latenci (cíl: < 4 sekundy end-to-end)
- [ ] Memory profiling – ověřit stabilitu při dlouhém běhu

---

## Struktura souborů (cílový stav)

```
app/src/main/
├── AndroidManifest.xml
├── java/cz/kubmak/rubby_llm_exp/
│   ├── MainActivity.java              # Hlavní Activity + RobotLifecycleCallbacks
│   ├── network/
│   │   ├── Tls12SocketFactory.java    # TLS 1.2 SSL patch
│   │   └── NetworkClient.java         # OkHttpClient singleton
│   ├── llm/
│   │   ├── ILlmService.java           # Interface pro LLM služby
│   │   ├── GeminiService.java         # Implementace pro Gemini API
│   │   ├── GeminiApiInterface.java    # Retrofit endpoint definice
│   │   └── models/                    # Gson data třídy
│   │       ├── GeminiRequest.java
│   │       ├── GeminiResponse.java
│   │       ├── Content.java
│   │       └── Part.java
│   ├── conversation/
│   │   ├── ChatHistoryManager.java    # FIFO buffer konverzace
│   │   ├── ConversationState.java     # Enum: IDLE, LISTENING, PROCESSING, SPEAKING
│   │   └── ConversationManager.java   # Stavový automat řídící flow
│   ├── audio/
│   │   ├── SpeechInput.java           # Android SpeechRecognizer wrapper
│   │   └── RobotSpeaker.java         # QiSDK Say + Animate helper
│   └── util/
│       └── TextCleaner.java           # Markdown stripping
├── res/
│   ├── layout/activity_main.xml       # UI: status + chat log
│   └── raw/                           # Zvuky (pípnutí)
└── assets/
    └── wakeword.top                   # QiChat trigger soubor
```

---

## Klíčové dependencies (app/build.gradle)

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

// Google Play Services (pro ProviderInstaller – TLS patch)
implementation 'com.google.android.gms:play-services-base:18.2.0'
```

> ⚠️ **QiSDK repository** musí být přidán do `settings.gradle`:
> ```groovy
> maven { url 'https://qisdk.softbankrobotics.com/sdk/maven' }
> ```

---

## System Prompt (Persona robota)

```
Jsi robot Pepper. Jsi fyzický, humanoidní robot vyrobený firmou SoftBank Robotics.
Nacházíš se v kancelářském prostředí. Jsi nápomocný, zdvořilý a přátelský.
Mluvíš plynně česky, spisovně.
Tvé odpovědi MUSÍ být stručné – maximálně 2-3 věty, protože tvá syntéza řeči je pomalá.
Nepoužívej emotikony, hvězdičky, hashtahy ani žádné formátování.
Pokud se tě někdo zeptá na tvé tělo, odkazuj na své senzory, tablet na hrudi a pohyblivé ruce.
Pokud neznáš odpověď, přiznej to upřímně.
```

---

## Gemini API – vzorový request

```
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=API_KEY

{
  "system_instruction": {
    "parts": [{"text": "Jsi robot Pepper..."}]
  },
  "contents": [
    {"role": "user", "parts": [{"text": "Jaké je hlavní město Austrálie?"}]},
    {"role": "model", "parts": [{"text": "Hlavní město Austrálie je Canberra."}]},
    {"role": "user", "parts": [{"text": "A kolik tam žije lidí?"}]}
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 150,
    "topP": 0.9
  }
}
```

---

## Vzor: Tls12SocketFactory (kritická třída)

```java
/**
 * Nutné pro Android 6.0 (API 23), kde TLS 1.2 není ve výchozím
 * nastavení povolený na všech socketech. Bez tohoto patche se
 * připojení ke Google API nezdaří (SSLHandshakeException).
 */
public class Tls12SocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;

    public Tls12SocketFactory(SSLSocketFactory base) {
        this.delegate = base;
    }

    private Socket enableTls12(Socket socket) {
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledProtocols(
                new String[]{"TLSv1.2"}
            );
        }
        return socket;
    }

    // ... delegovat všechny metody SSLSocketFactory s wrapem enableTls12()
}
```

---

## UX / Latence mitigace

| Technika | Implementace |
|---|---|
| **Thinking animace** | Ihned po konci STT spustit `Animate` s animací přemýšlení |
| **Filler fráze** | Paralelně s API voláním: `Say("Moment...")` – subjektivně nulová latence |
| **Timeout** | Max 10s na API odpověď, pak fallback chybová hláška |
| **Streaming** | Gemini API podporuje streaming – v budoucnu lze říkat odpověď po částech |

---

## Budoucí rozšíření (mimo scope první verze)

- [ ] DeepSeek V3 jako backup `ILlmService` implementace
- [ ] OpenAI Whisper API jako alternativní STT (lepší přesnost v hluku)
- [ ] Streaming response z Gemini (říkat odpověď po částech)
- [ ] Perzistence konverzací do SQLite
- [ ] Rozpoznávání obličejů + personalizace konverzace
- [ ] Multi-language podpora (přepínání EN/CZ)

---

## Poznámky pro agenta/vývojáře

1. **NIKDY nepoužívej knihovny vyžadující API > 23** – vždy ověř `minSdkVersion` kompatibilitu
2. **NIKDY nepoužívej Java 8 Streams API** – preferuj klasické for-loop
3. **NIKDY nehardcoduj API klíč** – vždy přes `BuildConfig`
4. **QiSDK akce MUSÍ běžet mimo UI thread** – používej `AsyncTask` nebo `Executor`
5. **Robot Focus je klíčový** – bez něj nefunguje Say, Listen, Animate
6. **Testuj TLS jako první věc** – 99 % problémů s konektivitou je SSL handshake
7. **Čeština v SpeechRecognizer** – ověř že tablet má stažený offline cs-CZ balíček
8. **Odpovědi z LLM čisti od Markdown** – Pepper říká hvězdičky nahlas
