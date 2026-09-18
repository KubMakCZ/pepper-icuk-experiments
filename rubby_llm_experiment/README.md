# 🤖 Rubby LLM Experiment (Pepper Robot)

Tento projekt je inovativní Android aplikace (tzv. "wrapper") pro humanoidního robota **Pepper** (platforma QiSDK). Cílem je propojit robota s moderními velkými jazykovými modely (LLM) z rodiny **Google Gemini** a open-source modely přes **OpenRouter**, aby mohl vést plynulou, inteligentní a kontextovou konverzaci v češtině.

## 🌟 Klíčové vlastnosti

- **Inteligentní mozek (Přepínání modelů):** Projekt v sobě integruje hned několik AI modelů s možností **dynamického přepínání za běhu** (pomocí UI dropdown menu). Podporuje:
  - **Google Gemini 1.5 Flash** (oficiální API).
  - Open-source modely zdarma přes **OpenRouter API** (skvělé pro vývoj a testování bez nákladů): *Gemma 2 9B*, *Llama 3.1 8B* a *Mistral 7B*.
- **Plynulá čeština:** Nativní integrace českého rozpoznávání řeči (STT) s optimalizovaným rychlým startem naslouchání a syntézou hlasu (TTS).
- **Wake Word Detekce:** Integrovaný systém **QiChat**, který lokálně naslouchá na jméno *"Rubby"* a jakmile je vysloveno, automaticky robota probudí do stavu naslouchání, aby byl hned připraven na dotaz.
- **Obecné znalosti & Persona:** Rubby vystupuje jako maskot *Inovačního centra Ústeckého kraje (ICUK)*, ovšem díky chytře postavenému systémovému promptu naplno využívá svých obecných LLM znalostí a dokáže odpovědět na jakýkoliv dotaz i mimo své hlavní zacílení.
- **Robotická gestikulace:** Během mluvení může LLM dynamicky spouštět animace robota přes speciální příkazové tagy v textu (které pak robot nečte nahlas, ale fyzicky vykoná).
- **Konverzační paměť:** Pamatuje si posledních 10 výměn (turnů).
- **Hybridní režim:**
  - Na robotu Pepper plně využívá nativní **QiSDK** (hlas a pohyby robota, dotyk na hlavě).
  - V emulátoru nebo na telefonu využívá standardní **Android TTS** (pro testování).

## 🛠️ Technické parametry

- **Platforma:** Android 6.0 (API 23) - nutné pro kompatibilitu s Pepperem.
- **Jazyk:** Java (z důvodu maximální stability se starým SDK).
- **Sítě:** Retrofit 2 + OkHttp 3 (se zapnutým speciálním ručním **TLS 1.2 patchem** pro připojování k moderním serverům přes starý Android).

## 🚀 Jak začít

1. **API Klíče:** 
   - Pro Google API získej klíč na [Google AI Studio](https://aistudio.google.com/).
   - Pro bezplatné testování získej klíč na [OpenRouter](https://openrouter.ai/).
2. **Konfigurace:** V kořenovém adresáři projektu vytvoř (nebo uprav) skrytý soubor `local.properties` a vlož své klíče:
   ```properties
   GEMINI_API_KEY=tvůj_google_api_klíč_zde
   OPENROUTER_API_KEY=tvůj_openrouter_api_klíč_zde
   ```
3. **Build:** Otevři projekt v Android Studiu. Nech synchronizovat Gradle s nastaveným JRE (doporučeno JDK 11 z Android Studia, aby plně kooperovalo s Gradle 7.2) a klikni na Run.
4. **Spuštění:**
   - **Na robotovi:** Robot po instalaci aplikace načte QiChat engine pro zachycení jména "Rubby". Jakmile ji oslovíš (nebo ji pohladíš po hlavě), začne tě poslouchat.
   - **V emulátoru:** Klikni na mikrofon a zkoušej LLM konverzaci v testovacím režimu. V horní liště si přes Spinner můžeš pohodlně přepínat modely podle toho, komu chceš ulevit od rate limitů.

## 📍 Nastavení identity a chování

Základní instrukce a osobnost robota se nastavují v souboru:
`app/src/main/java/cz/kubmak/rubby_llm_exp/llm/GeminiService.java` a `OpenRouterService.java`.

- Kód zpracovává raw odpovědi z LLM, aby robot nečetl např. ztučněný markdown (`**`) nebo speciální tagy pro animace (např. `[ANIMACE:wave]`). Zajišťuje to utilita `TextCleaner.java`.

## 📂 Struktura klíčových modulů

- `MainActivity.java` - Jádro systému: řízení stavů `IDLE -> LISTENING -> PROCESSING -> SPEAKING`, obsluha hardwarových tlačítek, QiSDK animací a naslouchání na Wake word.
- **`llm/`** - Abstraktní vrstva pro AI služby (`ILlmService.java`). Najdeš zde konkrétní implementace jak pro `GeminiService.java`, tak pro `OpenRouterService.java`.
- **`res/raw/wakeword.top`** - Pravidla QiChatu v češtině, definující slovník probouzecích frází (např. *Ahoj Rubby*, *Rubi*).

---
*Vytvořeno pro demonstrační a výukové účely na robotu Pepper (ICUK).*
