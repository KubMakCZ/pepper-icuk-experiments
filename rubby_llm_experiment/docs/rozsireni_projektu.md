# Plán rozšíření Rubby LLM Experiment

> Vytvořeno: 2026-09-18
> Projekt: `rubby_llm_experiment` — Android aplikace pro robota Pepper s LLM (Gemini/OpenRouter)
> Soubor pro další vývojáře / asistenty.

---

## Současný stav (quick recap)

- Android Java app (minSdk 23) pro robota Pepper přes QiSDK
- Gemini 2.5 Flash (primární) + OpenRouter (Gemma 2, Llama 3.1, Mistral 7B — vše free tier)
- Hybridní TTS (QiSDK na Pepperu, Android TTS na telefonu)
- SpeechRecognizer (cs-CZ) pro vstup
- Stavový automat: IDLE → LISTENING → PROCESSING → SPEAKING → IDLE
- Animace přes `[ANIMACE:xxx]` tagy v odpovědích LLM
- Head touch sensor pro ruční probuzení
- Chat historie: max 10 turnů, in-memory FIFO

---

## Seznam rozšíření

### 🟢 Low Priority — jednoduché, 1–2 týdny

---

#### 1. Perzistence konverzací (SQLite/Room)

**Proč:** Aktuálně se historie mazí po restartu robota. S perzistencí by Rubby mohl pokračovat v předchozí konverzaci.

**Co změnit:**
- Vytvořit Room databázi s tabulkou `conversations` (id, timestamp, role, message)
- `ChatHistoryManager` rozšířit o načívání z DB a ukládání do DB
- Při startu načíst poslední N zpráv z DB do in-memory historie
- Oldest message still trimmed when exceeding limit, but persisted in DB

**Soubory:**
- Nové: `app/src/main/java/cz/kubmak/rubby_llm_exp/conversation/ChatDatabase.java`
- Nové: `app/src/main/java/cz/kubmak/rubby_llm_exp/conversation/ChatDao.java`
- Nové: `app/src/main/java/cz/kubmak/rubby_llm_exp/conversation/ConversationEntity.java`
- Modify: `ChatHistoryManager.java` — přidej DB read/write

**Poznámky:**
- Room je součást AndroidX, kompatibilní s API 23
- Použít `@Insert`, `@Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")`
- Trimovat starší zprávy nad limit (např. posledních 100 zpráv)

---

#### 2. Streaming odpobědí z Gemini

**Proč:** Aktuálně Rubby čeká na celou odpověď, pak začne mluvit. Streaming by umožnil začít mluvit po částech, což dramaticky sníží subjektivní latenci.

**Co změnit:**
- Gemini API podporuje `streamGenerateContent` endpoint
- Vytvořit novou metodu v `GeminiService` pro streaming
- JiT ukazovat částečné odpovědi na UI a říkat je postupně přes QiSDK Say

**Soubory:**
- Modify: `GeminiApiInterface.java` — přidat `@POST("v1beta/models/...:streamGenerateContent")`
- Modify: `GeminiService.java` — přidat `generateResponseStream()` metodu
- Modify: `MainActivity.java` — zpracovat streaming callback, říkat dílčí texty

**Poznámky:**
- Streaming vrací SSE (Server-Sent Events) nebo chunkovaný JSON
- Hlavní výzva: spravovat částečné řeči (přerušit předchozí Say, začít nový)
- Alternativa: nasbírat celý text, ale ukazovat na UI postupně (jednodušší)

---

#### 3. Další animace

**Proč:** Rubby teď umí dance, hello, wave. Více animací = přirozenější chování.

**Co přidat:**
- `nod` — přikývnutí hlavou (ano/souhlas)
- `think` — pohled dolů, přemýšlení (během processing)
- `look_at_user` — pohled na uživatele (po přivítání)
- `sad` — smutek (pokud uživatel prosí o pomoc a neví odpověď)
- `surprised` — překvapení (něco nečekaného)

**Co změnit:**
- Vytvořit nové `.qianim` soubory v `app/src/main/res/raw/`
- Registrovat v `AnimationManager.CATEGORIES`
- Aktualizovat `getCategoriesForPrompt()` a system prompt

**Soubory:**
- Modify: `AnimationManager.java` — přidej nové kategorie
- Nové: `.qianim` soubory v `res/raw/`
- Modify: `GeminiService.java` SYSTEM_PROMPT — aktualizovat seznam animací

---

### 🟡 Medium Priority — 2–4 týdny

---

#### 4. OpenAI Whisper pro STT (alternativa/rozšíření)

**Proč:** V hlučném prostředí ( ICUK veřejný prostor ) je Android SpeechRecognizer nespolehlivý. Whisper API má lepší přesnost.

**Co změnit:**
- Vytvořit `WhisperService implements ILlmService` — ne, to není LLM. Vytvořit `SpeechRecognizer` wrapper, který volá Whisper API.
- Princip: nahrát audio → POST na `https://api.openai.com/v1/audio/transcriptions` → vrátí text
- V `MainActivity` přepínat mezi Android SpeechRecognizer a Whisper přes spinner nebo automaticky (použít Whisper když se Android STT splete)

**Soubory:**
- Nové: `app/src/main/java/cz/kubmak/rubby_llm_exp/audio/WhisperSTT.java`
- Modify: `MainActivity.java` — integrace do STT pipeline

**Poznámky:**
- Cena: $0.006/minuta zvuku
- Audio formát: WAV nebo M4A, max 25 minut
- Na Android 6.0 nahrávání audio přes `MediaRecorder` nebo `AudioRecord`
- Musí běžet na background thread

---

#### 5. DeepSeek jako plnohodnotná záloha

**Proč:** Gemini Free Tier (15 RPM) by mohl být limitující. DeepSeek V3 je ~10× levnější.

**Co změnit:**
- Vytvořit `DeepSeekService implements ILlmService`
- Endpoint: `POST https://api.deepseek.com/chat/completions`
- Přidat do spinneru v `MainActivity` jako třetí option
- Přidat `DEEPSEEK_API_KEY` do `local.properties` a `build.gradle`

**Soubory:**
- Nové: `app/src/main/java/cz/kubmak/rubby_llm_exp/llm/DeepSeekService.java`
- Nové: `app/src/main/java/cz/kubmak/rubby_llm_exp/llm/DeepSeekApiInterface.java`
- Nové: model třídy `DeepSeekRequest`, `DeepSeekResponse`
- Modify: `MainActivity.java` — přidat do spinneru
- Modify: `build.gradle` — `buildConfigField "String", "DEEPSEEK_API_KEY"`

---

#### 6. Web admin panel

**Proč:** ICUK personál (bez programátorů) může měnit persona Rubbyho, odpovědi na časté otázky, sledovat chat logy.

**Co vytvořit:**
- Jednoduché webové rozhraní (Python Flask / Spring Boot / Node Express)
- Endpointy:
  - `GET /api/logs` — posledních N chatů (z mobily/online DB)
  - `GET /api/persona` — aktuální system prompt
  - `PUT /api/persona` — editovat system prompt (údržba Rubbyho)
  - `GET /api/faq` — časté otázky a odpovědi
  - `PUT /api/faq` — přidat/upravit FAQ

**Co změnit v apce:**
- System prompt načíst z API (pokud dostupný) místo hardcodedu v `GeminiService`
- Chat logy posílat na endpoint (asynchronně, v background threadu)

**Soubory:**
- Nový projekt/repozitář (např. `rubby-admin-web/`) — není součást Android APK
- Modify: `GeminiService.java` — načíst system prompt z API (s fallback na lokální)

---

#### 7. Dotykové gesty na hrudi (tablet)

**Proč:** Lidé, kteří neznají dotyk hlavy, by mohli ovládat Robota přitahováním tabletu.

**Co změnit:**
- Zachytit dotykové eventy na tabletové UI
- Přitáhnutí nahoru → zobrazit hlavní menu
- Přitáhnutí dolů → skrýt UI, robot mluví
- Přitáhnutí doprava → další strana
- Využít `GestureDetector` z Android SDK

**Soubory:**
- Modify: `MainActivity.java` — přidat `GestureDetector` a `OnGestureListener`
- Modify: `activity_main.xml` — pomalovat dotykovou oblast

---

### 🔴 High Priority — 1–3 měsíce

---

#### 8. 🔥 Rozpoznávání obličejů a personalizace (OpenCV / ML Kit)

**Proč:** Toto je klíčové rozšíření pro budoucnost — Rubby by měl poznávat lidi a oslovovat je jmenem.

**Vize:**
- Když přijde Jakub → Rubby řekne "Ahoj Jakube!"
- Když přijde Tamara → Rubby řekne "Ahoj Tamaro!"
- Nová osoba → "Ahoj! Ještě mě neznám, jak se jmenuješ?"
- Po naučení → při dalším setkání mělo oslovit jménem

**Technický přístup (dvě možnosti):**

##### Možnost A: Google ML Kit Face Detection (doporučeno pro začátek)
- Knihovna: `com.google.mlkit:face-detection:16.1.6`
- Detekce obličejů z camera frame
- Klíčový problém: **jak přiřadit obličej jméno?**
  - Řešení: Uložit hash/encodings obličejů + jména lokálně
  - Při detekci porovnat s uloženými zápisy
  - ML Kit vrací `Face` objekt s contурem obličej — lze extrahovat "face embedding" (vektor znaků)
  - Porovnávat cosinou similarity

##### Možnost B: OpenCV + dnn (pokročilé)
- Knihovna: OpenCV Android SDK (`org.opencv:opencv`)
- Face detection: Haar cascade nebo DNN model (Caffe SSD)
- Face recognition: ArcFace / FaceNet model (`.tflite` nebo `.onnx`)
- Uložit face embeddings do SQLite
- Porovnávat při každém novém framu

**Co vytvořit:**

```
app/src/main/java/cz/kubmak/rubby_llm_exp/face/
├── FaceManager.java          # Hlavní třída — detekce, rozpoznávání, registry
├── FaceDatabase.java         # SQLite úložiště obličejů + jmen
├── FaceEncoder.java          # Extrahování embeddingu z obličejového signálu
└── FaceRecognitionListener.java  # Callback — "rozpoznán: Jakub" / "neznámá osoba"
```

**Co změnit:**
- Modify: `MainActivity.java`
  - Při rozpoznání obličej → zavolat `FaceManager.onFaceRecognized(name)`
  - V konverzační smyčce: pokud je rozpoznán člověk, přidat jeho jméno do systémového kontextu
  - Při IDLE + rozpoznání známého člověka → automaticky pozdravit
- Modify: `GeminiService.SYSTEM_PROMPT` — dynamicky doplnit: "Dnes v místnosti je Jakub a Tamara."
- Modify: `ChatHistoryManager` — přidat úroveň "pamatuj si jména"

**Datová struktura:**

```java
// FaceDatabase entity
@Entity(tableName = "known_faces")
public class KnownFace {
    @PrimaryKey(autoGenerate = true) int id;
    String name;           // "Jakub", "Tamara", etc.
    float[] embedding;     // face embedding vector (128 or 512 floats)
    long firstSeen;        // timestamp
    long lastSeen;         // timestamp
    int encounterCount;    // kolikrát viděn
    String photoPath;      // optional — úložiště obrázku
}
```

**Workflow nového uživatele:**
1. Rubby řekne: "Ahoj! Ještě mě neznám, jak se jmenuješ?"
2. Uživatel řekne své jméno (přes STT) nebo ho zadá na tabletu
3. Rubby pozdraví jménem a uloží face embedding do DB
4. Při dalším setkání: Rubby pozná a osloví jménem

**Hardware poznamenání:**
- Pepper má kameru na hlavě (2 MP) — dostatečná pro detekci
- Tablety mají omezený výkon — face recognition běží na serveru nebo edge TPU
- Alternativa: když je Rubby připojen k internetu, přeslat frame na server pro lepší akcelerace

---

#### 9. Zobrazování obrázků a spouštění videí

**Proč:** Rubby by měl ukazovat obrázky (např. fotografie ICUK, akcí, lidí) a spouštět krátká videa (pozdravní, prezentace).

**Technický přístup:**

##### Obrázky:
- QiSDK podporuje zobrazování obrázků na tablet Pepper přes `Qicanvas` nebo custom tablet layout
- Android `ImageView` v aktivitě může ukazovat obrázky z `res/drawable/` nebo z URL
- Použití: při konverzaci Rubby řekne "Podívej se na fotku ICUK" → zobrazí obrázek na tabletu

##### Videa:
- Pepper podporuje přehrávání videí přes QiSDK `PlayVideo` action nebo `Animation` s video souborem
- Format: MP4, maximální délka doporučena 30 sekund (pozornost uživatele)
- Android `VideoView` pro emulator/telefon

**Co vytvořit:**

```
app/src/main/java/cz/kubmak/rubby_llm_exp/media/
├── MediaManager.java        # Říší přehrávání videí a obrázků
├── ImageSlideShow.java      # Ukazuje sérii obrázků
└── VideoPlayer.java         # Přehrává videa přes QiSDK
```

**Co změnit:**
- Modify: `GeminiService.SYSTEM_PROMPT` — povolit LLM vracet `[OBRAZE:URL]` nebo `[VIDEO:URL]` tagy
- Modify: `TextCleaner.java` — extrahovat média tagy (podobně jako `[ANIMACE:xxx]`)
- Modify: `MainActivity.java` — spustit přehrávání po uklizení textu

**Soubory:**
- Nové: MediaManager.java, ImageSlideShow.java, VideoPlayer.java
- Modify: TextCleaner.java, MainActivity.java, GeminiService.java

---

#### 10. RAG (Retrieval-Augmented Generation) — znalosti ICUK

**Proč:** Rubby aktuálně používá system prompt s pevnými znalostmi. ICUK se mění — nové akce, noví členové, nové ceny. RAG by mu umožnil odpovídat na základě aktuálních dokumentů.

**Technický přístup:**
1. Parsovat dokumenty ICUK (web, PDF, stránky) → chunkovat text
2. Vytvořit embeddings (local — TensorFlow Lite, nebo server)
3. Uložit do SQLite s metadaty (zdroj, datum)
4. Při dotazu: najít top-K podobných chunků → přidat do Gemini kontextu jako "doplňující znalosti"

**Co vytvořit:**

```
app/src/main/java/cz/kubmak/rubby_llm_exp/rag/
├── DocumentProcessor.java    # Chunkování dokumentů
├── EmbeddingGenerator.java   # Vytváření vektorů (local nebo server)
├── RagDatabase.java          # SQLite úložiště embeddings
└── RagRetriever.java         # Nařízení relevantních chunků
```

**Co změnit:**
- Modify: `GeminiService.generateResponse()` — předat retrieved chunks jako soustava instrukcí

---

## Shrnutí priorit

| # | Nápada | Priorita | Odhad | Hlavní výzva |
|---|--------|----------|-------|--------------|
| 1 | Perzistence konverzací | 🟢 Low | 1 týden | SQLite schema, thread safety |
| 2 | Streaming odpovědí | 🟢 Low | 2 týdny | SSE parsing, částečné řeči |
| 3 | Víc animací | 🟢 Low | 1 týden | Nové .qianim + registrace |
| 4 | Whisper STT | 🟡 Med | 3 týdny | Audio nahrávání, API integrace |
| 5 | DeepSeek záloha | 🟡 Med | 2 týdny | Retrofit interface, error handling |
| 6 | Web admin panel | 🟡 Med | 4 týdny | Samostatný projekt, API |
| 7 | Dotykové gesty | 🟡 Med | 2 týdny | GestureDetector, UX |
| **8** | **Rozpoznávání obličejů** | **🔴 High** | **2–3 měsíce** | **OpenCV/ML Kit, embedding DB, UX workflow** |
| 9 | Obrázky a videa | 🔴 High | 2–3 měsíce | QiSDK media, tag parsing |
| 10 | RAG | 🔴 High | 3–4 měsíce | Embeddings, chunkování, DB |

---

## Shrnutí klíčového: Personalizace a vize

Cíl: **Rubby poznává lidi, ukazuje obrázky, spouští videa, a chová se jako přátelský mascot ICUK, který ví, kdo je kdo.**

### Klíčový workflow (oslovení jménem):
```
1. Camera frame → FaceManager.detect()
2. Face encoding → porovnat s KnownFace DB
3. Identifikace: "Jakub" (confidence > 0.85)
4. Trigger: MainActivity.onFaceRecognized("Jakub")
5. Rubby řekne: "Ahoj Jakube!" (přes QiSDK Say nebo Android TTS)
6. Spustí se přítomnostní animace (hello/wave)
7. Log uložen do DB (kdy, kdo, jak často)
```

### Klíčový workflow (zobrazení média):
```
1. Gemini odpověď obsahuje [OBRAZE:icuk_event.jpg]
2. TextCleaner.extractMediaTags() → extrahuje tagy
3. MediaManager.showImage("icuk_event.jpg")
4. Obrázek se zobrazí na tabletu Peppera
5. Po zobrazení → Rubby pokračuje v řeči
```

---

## Poznámky k implementaci

1. **Všechna rozšíření musí respektovat API 23** — žádné Java 8 Streams, žádné moderní SDK
2. **Všechno na background threadu** — hlavní thread nesmí blokovat
3. **QiSDK akce přes `buildAsync()` / `async()`** — nikdy synchronně na UI threadu
4. **API klíče přes BuildConfig** — nikdy hardcoded
5. **Face recognition je citlivé na soukromí** — doporučuji:
   - Používat face embeddings (ne čisté obrázky)
   - Obrázky mazat po natočení embeddingu
   - GDPR compliant — povolení uživatelů, informace o zpracování
6. **Testovat na reálném Pepperovi** — emulator nemá kameru, dotykový displej,QiSDK TTS

---

## Souvislosti mezi rozšířeními

```
Perzistence ──────────────► RAG (potřebuje DB pro dokumenty)
     │
     ▼
Rozpoznávání obličej ────► Personalizace (oslovení jménem)
     │                         │
     ▼                         ▼
Web admin panel ◄────────────┘ (sledovat, kdo přichází)
     │
     ▼
Obrázky / videa ──────► Zobrazování médií (ukazovat fotky)
```
