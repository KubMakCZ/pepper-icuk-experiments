# 🤖 Rubby LLM Experiment (Pepper Robot)

Tento projekt je inovativní Android aplikace (tzv. "wrapper") pro humanoidního robota **Pepper** (platforma QiSDK). Cílem je propojit robota s moderními velkými jazykovými modely (LLM) z rodiny **Google Gemini** a open-source modely přes **OpenRouter**, aby mohl vést plynulou, inteligentní a kontextovou konverzaci v češtině.

## 🌟 Klíčové vlastnosti

- **Inteligentní mozek (Přepínání modelů):** Projekt v sobě integruje hned několik AI modelů s možností **dynamického přepínání za běhu** (pomocí UI dropdown menu). Podporuje:
  - **Google Gemini Flash** (oficiální API).
  - Open-source modely zdarma přes **OpenRouter API** (skvělé pro vývoj a testování bez nákladů): automatický bezplatný router (*openrouter/free*), *Llama 3.1 8B*, *Qwen 3 8B* a *Mistral 7B*.
- **Plynulá čeština:** Nativní integrace českého rozpoznávání řeči (STT) s optimalizovaným rychlým startem naslouchání a syntézou hlasu (TTS).
- **Wake Word Detekce:** Integrovaný systém **QiChat**, který lokálně naslouchá na jméno *"Rubby"* a jakmile je vysloveno, automaticky robota probudí do stavu naslouchání, aby byl hned připraven na dotaz.
- **Obecné znalosti & Persona:** Rubby vystupuje jako maskot *Inovačního centra Ústeckého kraje (ICUK)*, ovšem díky chytře postavenému systémovému promptu naplno využívá svých obecných LLM znalostí a dokáže odpovědět na jakýkoliv dotaz i mimo své hlavní zacílení.
- **Robotická gestikulace:** Během mluvení může LLM dynamicky spouštět animace robota přes speciální příkazové tagy v textu (které pak robot nečte nahlas, ale fyzicky vykoná).
- **Konverzační paměť:** Pamatuje si posledních 10 výměn (turnů).
- **Sledování spotřeby tokenů:** Po každé odpovědi se v chatu zobrazí kolik tokenů daný dotaz spotřeboval (vidí jen člověk na tabletu, robot to neříká nahlas).
- **Debug logging:** Automaticky zapisuje logy do souboru `/sdcard/Android/data/cz.kubmak.rubby_llm_exp/files/RubbyLogs/` – dostupné i offline po faktu přes ADB nebo Device File Explorer.
- **Easter Egg:** Pokud Rubby pohladíš po hlavě (pat pat), náhodně vybere roztomilou odpověď před tím než začne poslouchat 🤖
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
   - **V emulátoru:** Klikni na mikrofon a zkoušej LLM konverzaci v testovacím režimu. V horní liště si přes Spinner můžeš pohodlně přepínat modely.

## 🖥️ Deploy skript (záloha za Android Studio)

Pokud Android Studio hlásí timeout při spouštění přes Wi-Fi, nebo chceš nasadit APK rychle z příkazové řádky, použij přiložený skript [`deploy.ps1`](./deploy.ps1).

### Předpoklady
- `adb` musí být dostupné v PATH (součást Android SDK Platform Tools).
- Robot/tablet musí být připojený přes **Wi-Fi ADB** nebo USB kabel.

### Wi-Fi ADB – první připojení
```powershell
# Připoj zařízení přes IP (zjistíš v Nastavení → O zařízení → IP adresa):
adb connect 192.168.0.36:5555

# Ověř připojení:
adb devices
```

### Použití deploy.ps1

```powershell
# Jen nasadit APK (build musí být hotový z Android Studia):
.\deploy.ps1

# Zkompilovat a nasadit (nevyžaduje Android Studio):
.\deploy.ps1 -Build

# Zkompilovat, nasadit a rovnou spustit aplikaci:
.\deploy.ps1 -Build -Launch

# Zkompilovat, nasadit, spustit + zobrazit live Logcat:
.\deploy.ps1 -Build -Launch -Logcat

# Konkrétní zařízení (při více připojených):
.\deploy.ps1 -Device 192.168.0.36:5555 -Launch
```

> **Poznámka k Execution Policy:** Pokud Windows odmítá spustit skript, spusť jednou v PowerShellu jako správce:
> ```powershell
> Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
> ```

### Ruční build z příkazové řádky (bez Android Studia)
```powershell
# Musíš nastavit JAVA_HOME na JRE z Android Studia:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jre"
.\gradlew assembleDebug
```

### Kde najít debug logy z robota
Po provozu robota na ICUKu si logy stáhneš přes:
```powershell
# Stáhnout log soubory:
adb -s 192.168.0.36:5555 pull /sdcard/Android/data/cz.kubmak.rubby_llm_exp/files/RubbyLogs/ ./logy

# Nebo live sledovat při připojeném kabelu/WiFi:
adb logcat RubbyLLM:D OpenRouterService:D GeminiService:D *:S
```

## 📍 Nastavení identity a chování

Základní instrukce a osobnost robota (`SYSTEM_PROMPT`) se nastavují centrálně v souboru:
`app/src/main/java/cz/kubmak/rubby_llm_exp/llm/ILlmService.java`.

- Kód zpracovává raw odpovědi z LLM, aby robot nečetl např. ztučněný markdown (`**`) nebo speciální tagy pro animace (např. `[ANIMACE:wave]`). Zajišťuje to utilita `TextCleaner.java`.

## 📂 Struktura klíčových modulů

- `MainActivity.java` - Jádro systému: řízení stavů `IDLE -> LISTENING -> PROCESSING -> SPEAKING`, obsluha hardwarových tlačítek, QiSDK animací a naslouchání na Wake word.
- **`llm/`** - Abstraktní vrstva pro AI služby (`ILlmService.java`). Najdeš zde konkrétní implementace jak pro `GeminiService.java`, tak pro `OpenRouterService.java`.
- **`util/FileLogger.java`** - Automatický zápis logů do souboru – funguje paralelně s Logcatem.
- **`res/raw/wakeword.top`** - Pravidla QiChatu v češtině, definující slovník probouzecích frází (např. *Ahoj Rubby*, *Rubi*).
- **`deploy.ps1`** - PowerShell skript pro nasazení APK bez Android Studia.

---
*Vytvořeno pro demonstrační a výukové účely na robotu Pepper (ICUK).*
