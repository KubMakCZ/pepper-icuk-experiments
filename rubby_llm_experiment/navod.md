# 🔑 Návod: Jak získat API klíče pro Pepper LLM Wrapper

---

## 1. Google Gemini API (Primární – ZDARMA)

Gemini 1.5 Flash má **trvalý Free Tier** – 15 požadavků/min, 1M tokenů/min. Pro jednoho robota naprosto stačí.

### Krok za krokem:

1. **Otevři Google AI Studio**
   - Jdi na: [https://aistudio.google.com/](https://aistudio.google.com/)
   - Přihlas se svým Google účtem (stačí ten, co máš na Gemini Premium)

2. **Získej API klíč**
   - V levém menu klikni na **"Get API key"** (nebo "Získat klíč API")
   - Klikni na **"Create API key"**
   - Vyber existující Google Cloud projekt, nebo nech vytvořit nový (automaticky)
   - Zkopíruj vygenerovaný klíč (formát: `AIzaSy...` – cca 39 znaků)

3. **Ověř že klíč funguje**
   - Otevři terminál/PowerShell a spusť:
   ```
   curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=TVŮJ_KLÍČ" -H "Content-Type: application/json" -d "{\"contents\":[{\"parts\":[{\"text\":\"Řekni ahoj česky\"}]}]}"
   ```
   - Měl bys dostat JSON odpověď s českým textem

4. **Vlož klíč do projektu**
   - Otevři soubor `local.properties` v kořenu projektu (ten je v `.gitignore`, takže se necommitne)
   - Přidej řádek:
   ```properties
   GEMINI_API_KEY=AIzaSy...tvůj_klíč...
   ```

### ⚠️ Důležité poznámky ke Gemini:
- **Gemini Premium předplatné ≠ API kredity** – Premium je jen pro chat na webu, API je zvlášť
- Free Tier je ale tak štědrý, že platit nemusíš
- Pokud bys překročil limity, automaticky se aktivuje pay-as-you-go ($0.075/1M input tokenů)
- Limity Free Tier: **15 RPM** (requests per minute), **1M TPM** (tokens per minute), **1500 RPD** (requests per day)

---

## 2. DeepSeek API (Záložní – velmi levný)

DeepSeek V3 je ~10× levnější než GPT-4o. Dobrý jako backup.

### Krok za krokem:

1. **Registrace**
   - Jdi na: [https://platform.deepseek.com/](https://platform.deepseek.com/)
   - Klikni **"Sign Up"** a zaregistruj se (email + heslo)

2. **Nabij kredit**
   - Po přihlášení jdi do **"Billing"** / **"Top Up"**
   - Minimální nabití je typicky $2–5 (platba kartou)
   - Za $2 máš cca 14 milionů input tokenů – to je hodně konverzací

3. **Vytvoř API klíč**
   - Jdi do **"API Keys"** v dashboardu
   - Klikni **"Create new API key"**
   - Pojmenuj ho (např. "pepper-robot")
   - Zkopíruj klíč (zobrazí se jen jednou!)

4. **Ověření**
   ```
   curl https://api.deepseek.com/chat/completions -H "Content-Type: application/json" -H "Authorization: Bearer TVŮJ_KLÍČ" -d "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"Ahoj\"}]}"
   ```

5. **Do projektu**
   ```properties
   # v local.properties
   DEEPSEEK_API_KEY=sk-...tvůj_klíč...
   ```

---

## 3. OpenAI Whisper API (Volitelné – lepší STT)

Jen pokud by Android SpeechRecognizer nestačil. Cena: $0.006/minuta zvuku.

### Krok za krokem:

1. **Registrace**
   - Jdi na: [https://platform.openai.com/](https://platform.openai.com/)
   - Vytvoř si účet nebo se přihlas

2. **Nabij kredit**
   - Jdi do **"Settings"** → **"Billing"**
   - Klikni **"Add payment method"** a přidej kartu
   - Nabij alespoň $5 (při $0.006/min to je ~830 minut nahrávek)

3. **Vytvoř API klíč**
   - Jdi na: [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
   - Klikni **"Create new secret key"**
   - Pojmenuj (např. "pepper-whisper")
   - Zkopíruj (zobrazí se jen jednou!)

4. **Ověření**
   - Whisper vyžaduje audio soubor, takže otestuj jednoduchým API voláním:
   ```
   curl https://api.openai.com/v1/models -H "Authorization: Bearer TVŮJ_KLÍČ"
   ```
   - Měl bys dostat seznam dostupných modelů (ověření že klíč funguje)

5. **Do projektu**
   ```properties
   # v local.properties
   OPENAI_API_KEY=sk-...tvůj_klíč...
   ```

---

## 4. Jak klíče bezpečně dostat do Android kódu

V `app/build.gradle` přidej do bloku `defaultConfig`:

```groovy
android {
    defaultConfig {
        // ... existující konfigurace ...

        // Načtení klíčů z local.properties
        def localProps = new Properties()
        def localPropsFile = rootProject.file('local.properties')
        if (localPropsFile.exists()) {
            localProps.load(new FileInputStream(localPropsFile))
        }

        buildConfigField "String", "GEMINI_API_KEY",
            "\"${localProps.getProperty('GEMINI_API_KEY', '')}\""
        buildConfigField "String", "DEEPSEEK_API_KEY",
            "\"${localProps.getProperty('DEEPSEEK_API_KEY', '')}\""
        buildConfigField "String", "OPENAI_API_KEY",
            "\"${localProps.getProperty('OPENAI_API_KEY', '')}\""
    }
}
```

Pak v Java kódu:
```java
String apiKey = BuildConfig.GEMINI_API_KEY;
```

### ⚠️ Bezpečnost:
- `local.properties` je standardně v `.gitignore` – klíče se NIKDY nedostanou do Gitu
- Klíče jsou "zapečené" do APK při buildu – pro produkci ideálně použij obfuskaci
- Pro experimentální robota v kanceláři je toto řešení naprosto dostačující

---

## Shrnutí: Co potřebuješ minimálně

| Služba | Nutnost | Cena | Kde získat |
|---|---|---|---|
| **Gemini API** | ✅ Povinné | Zdarma (Free Tier) | [aistudio.google.com](https://aistudio.google.com/) |
| **DeepSeek API** | ⬜ Volitelné (backup) | ~$0.14/1M tokenů | [platform.deepseek.com](https://platform.deepseek.com/) |
| **OpenAI Whisper** | ⬜ Volitelné (lepší STT) | $0.006/min | [platform.openai.com](https://platform.openai.com/) |

**Pro začátek stačí JEN Gemini klíč** – zbytek můžeš přidat kdykoli později.
