# 🤖 Rubby LLM Experiment (Pepper Robot)

Tento projekt je inovativní Android aplikace (tzv. "wrapper") pro humanoidního robota **Pepper** (platforma QiSDK). Cílem je propojit robota s modelem **Google Gemini 2.5 Flash**, aby mohl vést plynulou, inteligentní a kontextovou konverzaci v češtině.

## 🌟 Klíčové vlastnosti

- **Inteligentní mozek:** Využívá Google Gemini API pro generování odpovědí v reálném čase.
- **Plynulá čeština:** Podporuje české rozpoznávání řeči (STT) i syntézu hlasu (TTS).
- **Robotická gestikulace:** Rubby během mluvení automaticky a náhodně gestikuluje, aby působila přirozeně.
- **Konverzační paměť:** Pamatuje si posledních 10 výměn (turnů), takže ví, o čem jste mluvili před chvílí.
- **Hybridní režim:**
  - Na robotu Pepper využívá nativní **QiSDK** (hlas a pohyby robota).
  - V emulátoru nebo na telefonu využívá standardní **Android TTS** (pro testování).

## 🛠️ Technické parametry

- **Platforma:** Android 6.0 (API 23) - nutné pro kompatibilitu s Pepperem.
- **Jazyk:** Java.
- **Sítě:** Retrofit 2 + OkHttp 3 (včetně TLS 1.2 patche pro starý Android).
- **AI Model:** `gemini-2.5-flash` (volán přes `v1beta` endpoint).
- **Generační parametry:** temperature `0.7`, maxOutputTokens `200`, topP `0.9`.

## 🚀 Jak začít

1. **API Klíč:** Získej svůj API klíč pro Gemini v [Google AI Studio](https://aistudio.google.com/).
2. **Konfigurace:** V kořenovém adresáři projektu vytvoř (nebo uprav) soubor `local.properties` a přidej do něj svůj klíč:
   ```properties
   GEMINI_API_KEY=tvůj_api_klíč_zde
   ```
3. **Build:** Otevři projekt v Android Studiu a nechej Gradle synchronizovat závislosti.
4. **Spuštění:**
   - **Na robotovi:** Připoj se k Pepperovi přes IP adresu a spusť aplikaci.
   - **V emulátoru:** Spusť aplikaci v emulátoru (ideálně API 23-30). Pro hlasové ovládání klikni na tlačítko "MIC".

## 📍 Nastavení identity (Persona)

Pokud chceš změnit to, co Rubby ví o akci nebo o sobě, uprav proměnnou `SYSTEM_PROMPT` v souboru:
`app/src/main/java/cz/kubmak/rubby_llm_exp/llm/GeminiService.java`

Můžeš tam Rubby naučit:
- Kde se právě nachází (aktuálně ICUK v Ústí nad Labem).
- Jaké jsou její úkoly.
- Specifické odpovědi na časté otázky.

## 📂 Struktura projektu

- `MainActivity.java` - Hlavní logika, řízení stavů (poslech, přemýšlení, mluvení), QiSDK lifecycle.
- **`llm/`** - Komunikace s AI modelem:
  - `GeminiService.java` - Implementace volání Google Gemini API (s retry logikou).
  - `GeminiApiInterface.java` - Retrofit definice API endpointu.
  - `ILlmService.java` - Rozhraní pro snadné přepnutí LLM providera.
  - `models/` - Datové třídy (`GeminiRequest`, `GeminiResponse`, `Content`, `Part`).
- **`conversation/`** - Správa konverzace:
  - `ChatHistoryManager.java` - FIFO buffer krátkodobé paměti robota (max 10 turnů).
  - `ConversationState.java` - Enum stavů konverzace (IDLE, LISTENING, PROCESSING, SPEAKING).
- **`network/`** - Síťová vrstva:
  - `NetworkClient.java` - Singleton OkHttpClient s TLS 1.2 patchem.
  - `Tls12SocketFactory.java` - Bezpečnostní patch pro připojení ke Google serverům na starém Androidu.
- **`util/`** - Utility:
  - `TextCleaner.java` - Čištění Markdown značek z odpovědí LLM (aby je Pepper nečetl nahlas).

---
Vytvořeno pro **Inovační centrum Ústeckého kraje (ICUK)**.
