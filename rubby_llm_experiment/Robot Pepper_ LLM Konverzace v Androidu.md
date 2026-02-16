# **Technická architektura a implementační strategie pro integraci velkých jazykových modelů (LLM) do platformy SoftBank Robotics Pepper**

## **1\. Exekutivní shrnutí**

Integrace generativní umělé inteligence do robotické platformy Pepper (model QiSDK) představuje technologický průlom, který transformuje deterministické chování robota na dynamickou, kontextově uvědomělou interakci. Tento report poskytuje vyčerpávající analýzu a implementační plán pro vytvoření "wrapper" aplikace, která propojí stárnoucí hardware robota Pepper (běžící na Android 6.0 / API 23\) s nejmodernějšími inferenčními enginy, jako jsou Google Gemini, Anthropic Claude a DeepSeek.  
Analýza identifikuje jako kritický bod rozpor mezi moderními požadavky AI SDK (často vyžadujícími Android 8.0+) a hardwarovou realitou tabletu robota Pepper. Řešením není přímá integrace klientských knihoven, nýbrž architektura založená na REST API voláních s explicitním ošetřením bezpečnostních protokolů (TLS 1.2) a paměťové optimalizace.  
**Klíčová doporučení:**

* **Primární LLM služba:** **Google Gemini 1.5 Flash** je vyhodnocena jako optimální řešení. Důvody zahrnují existující bezplatnou vývojářskou úroveň (Free Tier), která je dostačující pro produkční nasazení jednoho robota, superiorní podporu českého jazyka a nejnižší latenci odezvy, což je pro robotickou interakci klíčové.1  
* **Záložní (Backup) služba:** **DeepSeek V3** je doporučen jako nákladově nejefektivnější alternativa pro případ překročení limitů Gemini, s cenou o řád nižší než konkurence při srovnatelné kvalitě českého výstupu.4  
* **Architektura:** Vzhledem k verzi Androidu 6.0 (Marshmallow) je nutné obejít oficiální klientské knihovny (např. google-generative-ai-android) a implementovat vlastní síťovou vrstvu pomocí knihoven Retrofit a OkHttp s manuálním patchováním SSLSocketFactory.6  
* **Audio Pipeline:** Pro dosažení "plynulé konverzace" report doporučuje hybridní přístup: využití nativního QiChat pro detekci klíčového slova (Wake Word) a následné přepnutí na **Android SpeechRecognizer** nebo cloudové API **OpenAI Whisper** pro diktování volného textu, čímž se obchází limitace gramatických souborů .top.8

Následující kapitoly detailně rozebírají jednotlivé aspekty tohoto technického řešení.

## ---

**2\. Analýza hardwarového a softwarového ekosystému Pepper QiSDK**

Pochopení limitů hostitelského prostředí je prvním krokem k úspěšné integraci. Robot Pepper ve verzi QiSDK (uveden cca 2016-2017) je vybaven tabletem, který z dnešního pohledu představuje "legacy" zařízení. Vývojář musí přistupovat k tomuto projektu spíše jako k archeologii operačního systému než jako k modernímu vývoji aplikací.

### **2.1 Omezení Android API 23 (Marshmallow)**

Nejzásadnějším omezením identifikovaným v průzkumu je operační systém Android 6.0, API Level 23\.7 Toto číslo definuje mantinely pro všechny knihovny a API volání.

* **Nekompatibilita moderních SDK:** Většina oficiálních SDK pro AI (např. Google AI Client SDK for Android) vyžaduje minimálně Android 8.0 (API 26\) nebo vyšší, primárně kvůli podpoře moderních proudů (Java Streams API) a kryptografických standardů. Pokus o kompilaci těchto knihoven na API 23 skončí chybami typu NoClassDefFoundError nebo selháním při sestavování Gradle.11  
* **Bezpečnostní vrstva (SSL/TLS):** Android 6.0 podporuje protokol TLS 1.2, ale na úrovni SSLSocket jej ve výchozím nastavení často nepovoluje pro všechna spojení. Moderní API endpointy (Google Cloud, OpenAI, Anthropic) striktně vyžadují TLS 1.2 nebo 1.3. Pokud aplikace explicitně neupraví konfiguraci sítě (vytvořením vlastního TLSSocketFactory), připojení k API selže s chybou SSLHandshakeException.6  
* **Memory Management:** Tablet robota má omezenou operační paměť (RAM), která je sdílena mezi systémem Android, službami QiSDK (ovládání motorů, senzorů) a uživatelskou aplikací. Na rozdíl od moderních telefonů s 8–12 GB RAM má vývojář k dispozici stovky megabajtů. Načítání velkých audio souborů pro zpracování (např. odesílání do Whisper API) může snadno způsobit OutOfMemoryError a pád aplikace.13

### **2.2 Architektura QiSDK: Životní cyklus a Fokus**

QiSDK zavádí koncepty, které nejsou v běžném Android vývoji přítomny, a které zásadně ovlivňují chování "wrapperu" pro LLM.

* **Robot Focus (Zaměření):** Aplikace musí získat "Focus", aby mohla ovládat robotovy senzory a aktuátory. Pokud robot ztratí focus (např. uživatel se dotkne notifikační lišty), konverzace se přeruší. Aplikace musí být navržena tak, aby stav konverzace (historie chatu) přežil ztrátu a znovuzískání focusu.13  
* **Autonomní život (Autonomous Life):** Robot má na pozadí běžící procesy, které automaticky reagují na zvuk a pohyb. Při implementaci vlastního naslouchání (např. pro nahrávání zvuku pro LLM) může dojít ke konfliktu o mikrofon (ALAudioDevice). Je nutné správně spravovat priority audio vstupů, aby robot nepřerušoval sám sebe nebo neignoroval uživatele.16

### **2.3 Audio subsystém: Problém vstupu**

Nativní systém QiChat je založen na deterministických gramatikách (soubory .top). To znamená, že robot rozumí pouze tomu, co má napsáno ve scénáři.

* **Limitace pro LLM:** Pro využití LLM potřebujeme tzv. "Open Dictation" – schopnost převést jakýkoliv mluvený projev uživatele na text. QiChat sice podporuje zástupné znaky (\*), ale přesnost rozpoznávání celých vět v češtině je bez specifického slovníku nízká.18  
* **Implikace:** Wrapper aplikace nemůže spoléhat na QiChat pro získání vstupu (promptu) pro LLM. Musí implementovat separátní mechanismus pro převod řeči na text (STT), což zvyšuje komplexitu aplikace.

## ---

**3\. Komparativní analýza služeb LLM**

Na základě požadavku uživatele byla provedena hloubková analýza dostupných LLM služeb s ohledem na existující předplatná (Gemini Premium, Claude Pro), kvalitu češtiny, cenu a latenci.

### **3.1 Přehled trhu a cenových modelů**

Je klíčové rozlišovat mezi "Spotřebitelským předplatným" (chat v prohlížeči) a "Vývojářským API" (přístup přes kód). Tyto dva světy jsou často fakturačně oddělené.

| Parametr | Google Gemini 1.5 Flash | DeepSeek V3 | Anthropic Claude 3.5 Haiku | OpenAI GPT-4o mini |
| :---- | :---- | :---- | :---- | :---- |
| **Typ modelu** | Multimodální, High-Speed | Mixture-of-Experts (MoE) | High-Speed Reasoning | General Purpose Small |
| **Cena (Input)** | Zdarma (do limitu) / $0.075/1M | \~$0.14 / 1M tokenů | $0.25 / 1M tokenů | $0.15 / 1M tokenů |
| **Cena (Output)** | Zdarma (do limitu) / $0.30/1M | \~$0.28 / 1M tokenů | $1.25 / 1M tokenů | $0.60 / 1M tokenů |
| **Free Tier (API)** | **ANO** (15 RPM, 1M TPM) | Ne (pouze levný kredit na start) | Ne (pouze $5 kredit na start) | Ne (pouze trial) |
| **Čeština** | **Vynikající** | Velmi dobrá | Vynikající | Vynikající |
| **Latence** | **Nejnižší** (\< 1s) | Střední (1-3s) | Nízká (\~1s) | Nízká (\~1s) |
| **Context Window** | 1,000,000 tokenů | 64k tokenů | 200k tokenů | 128k tokenů |

### **3.2 Analýza uživatelových předplatných**

Uživatel disponuje **Gemini Premium (Google One AI Premium)** a **Claude Pro**. Jaký to má dopad na API?

* **Google Gemini Premium:** Toto předplatné primárně odemyká model "Gemini Advanced" v rozhraní gemini.google.com a integraci do Google Workspace (Docs, Gmail). Zkoumání dokumentace 20 ukazuje, že Google One AI Premium **automaticky neposkytuje** kredity pro Vertex AI nebo Google AI Studio API. API je účtováno zvlášť přes Google Cloud Billing.  
  * *Pozitivní zjištění:* Google nicméně nabízí **trvalý Free Tier** pro model gemini-1.5-flash v rámci Google AI Studia. Tento plán umožňuje až 15 požadavků za minutu (RPM) a 1 milion tokenů za minutu zdarma. Pro potřeby jednoho robota Pepper je tento limit naprosto dostačující a de facto znamená provoz **zcela zdarma** bez nutnosti čerpat z placeného tarifu.1  
* **Claude Pro:** Předplatné za $20/měsíc se vztahuje výhradně na webové rozhraní claude.ai. Neexistuje mechanismus, jak převést toto předplatné na API kredity. Pro využití Claude API by uživatel musel založit "Console" účet, nabít kredit (pre-paid) a platit za každý token. Vzhledem k tomu, že Gemini Flash je zdarma, je Claude z ekonomického hlediska pro tento projekt nevýhodný, i přes své kvality.24

### **3.3 DeepSeek: Ekonomický disruptor**

Model **DeepSeek V3** se ukazuje jako extrémně zajímavá alternativa. Jeho cena je přibližně 10x nižší než u GPT-4o a srovnatelná s GPT-4o-mini, přičemž v benchmarcích (včetně kódování a překladu) dosahuje špičkových výsledků.4 Pro český jazyk je DeepSeek překvapivě kompetentní, i když jeho "osobnost" může být o něco sušší než u Claude nebo Gemini. Pokud by aplikace přerostla limity Free Tieru u Googlu, DeepSeek je jasnou volbou pro minimalizaci nákladů.

### **3.4 Doporučení pro Pepper Wrapper**

Na základě průsečíku ceny, výkonu a dostupnosti je vítězem **Google Gemini 1.5 Flash**.

1. **Cena:** Zdarma (Pay-as-you-go se aktivuje až po překročení limitů, kterých jeden robot stěží dosáhne).  
2. **Rychlost:** Flash modely jsou optimalizovány pro nízkou latenci, což je u hlasové interakce s robotem kritické. Čekání 3 sekundy na odpověď působí v rozhovoru nepřirozeně.  
3. **Ekosystém:** Uživatel již používá Android Studio, kde je Gemini integrováno (Gemini in Android Studio), což usnadňuje generování boilerplate kódu.27

## ---

**4\. Architektura Audio Pipeline (Uši a Ústa)**

Aby robot mohl vést "plynulou konverzaci", musíme vyřešit, jak dostat hlas do cloudu a odpověď zpět k uživateli. Toto je nejkomplexnější část projektu.

### **4.1 Speech-to-Text (STT): Problém vstupu**

Jak již bylo zmíněno, QiChat nestačí. Máme tři možnosti:

#### **Možnost A: Android SpeechRecognizer (Google)**

Android API 23 obsahuje třídu SpeechRecognizer.

* *Výhody:* Je zdarma, běží na zařízení (nebo využívá Google servery transparentně), nulové další náklady.  
* *Nevýhody:* Na starém Androidu 6.0 může být implementace zastaralá a méně přesná v hlučném prostředí (ventilátory robota). Vyžaduje připojení k internetu.  
* *Implementace:* Vytvoření Intent s akcí ACTION\_RECOGNIZE\_SPEECH. Je nutné vizuálně indikovat na tabletu, že robot poslouchá (protože Pepperovy LED diody v očích jsou ovládány QiSDK, nikoliv Androidem přímo).

#### **Možnost B: OpenAI Whisper API**

Využití modelu Whisper (např. whisper-1) přes API.8

* *Výhody:* Extrémní přesnost i v češtině, odolnost vůči šumu a přízvukům. Snippet 8 přímo porovnává Whisper a Google ASR na robotu Pepper a Whisper vykazuje výrazně nižší chybovost (WER 1.7% vs Google).  
* *Nevýhody:* Latence. Je nutné nahrát audio do souboru (WAV/M4A), odeslat HTTP POST request (upload), počkat na zpracování a stáhnout JSON. To přidává 2-4 sekundy zpoždění.  
* *Cena:* $0.006 / minutu zvuku.  
* *Technický problém:* Nahrávání audia na Androidu 6.0 při současném běhu QiSDK vyžaduje pečlivou správu AudioRecord třídy, aby nedošlo ke konfliktu s ALAudioDevice.

#### **Možnost C: Whisper na zařízení (Whisper.cpp)**

Existují porty Whisperu pro Android běžící lokálně.30

* *Verdikt:* **Zamítnuto.** Hardware Peppera (CPU Atom nebo slabší ARM v tabletu) nemá dostatečný výkon pro běh inference v reálném čase. Způsobilo by to přehřívání a extrémní zpoždění.

**Doporučená strategie:** Začít s **Android SpeechRecognizer** kvůli rychlosti a nulové ceně. Pokud bude přesnost v praxi nedostatečná, přejít na Whisper API. Kód vygenerovaný v závěru bude podporovat modulární výměnu těchto komponent.

### **4.2 Text-to-Speech (TTS): Hlas robota**

Zde je volba jednoznačná: **Použít nativní QiSDK TTS.**

* *Důvod:* Cloudové TTS (např. ElevenLabs, Google Cloud TTS) sice zní přirozeněji, ale generování MP3 souboru a jeho streamování do robota přidává další sekundy latence. Navíc synchronizace rtů a pohybů těla (ALAnimatedSpeech) funguje automaticky pouze s nativním TTS.  
* *Čeština:* Pepper má nativní český hlas (často "Eliska" nebo podobný Nuance hlas), který je srozumitelný a rychlý.32

## ---

**5\. Softwarová architektura "Wrapperu"**

Aplikace bude postavena jako standardní Android Activity s implementací RobotLifecycleCallbacks.

### **5.1 Datový tok (Data Flow)**

1. **Idle State:** Robot čeká na klíčové slovo (pomocí QiChat poslouchá na "Peppře" nebo "Haló").  
2. **Wake Up:** Jakmile je detekováno klíčové slovo:  
   * Spustí se zvuková indikace (pípnutí).  
   * Robot přeruší QiChat naslouchání.  
   * Aktivuje se SpeechRecognizer (Android).  
3. **Listening:** Uživatel mluví ("Jaké je hlavní město Austrálie?").  
4. **Processing:**  
   * Aplikace převezme textový řetězec.  
   * Přidá jej do historie konverzace (List objektů).  
   * Sestaví JSON payload pro Gemini API.  
   * Odešle POST request (s patchovaným TLS 1.2).  
5. **Thinking:** Během čekání na odpověď robot hraje animaci "Thinking" (aby uživatel věděl, že se něco děje).  
6. **Speaking:**  
   * Přijde odpověď z Gemini.  
   * Text se očistí od Markdown značek (hvězdičky, mřížky).  
   * Volá se SayBuilder.with(qiContext).withText(response).build().run().  
   * Současně se spouští animace gestikulace.  
7. **Loop:** Robot se vrací do stavu Listening (pokud je konverzace aktivní) nebo Idle.

### **5.2 Správa paměti a kontextu (Context Window)**

LLM modely nemají paměť. Aby robot věděl, že se bavíte stále o tomtéž tématu, musíte mu s každou zprávou poslat i ty předchozí.13

* *Implementace:* MutableList\<Content\> chatHistory.  
* *Limit:* Vzhledem k paměti tabletu doporučuji držet jen posledních 10 výměn (turnů). Starší zprávy se zahodí (FIFO \- First In, First Out).  
* *Perzistence:* Není nutné ukládat do databáze, pokud konverzace nemá přežít restart robota. Pro jednoduchost postačí paměťová struktura.

### **5.3 System Prompt (Persona)**

Aby se robot nechoval jako webový vyhledávač, je nutné nastavit "System Instruction".

* *Návrh:* "Jsi robot Pepper. Jsi fyzický, humanoidní robot v kancelářském prostředí. Jsi nápomocný, zdvořilý a mluvíš plynně česky. Tvé odpovědi musí být stručné (maximálně 2-3 věty), protože tvá syntéza řeči je pomalá. Nepoužívej emotikony ani složité formátování. Pokud se tě někdo zeptá na tvé tělo, odkazuj na své senzory a tablet."

## ---

**6\. Bezpečnostní a síťová specifika (Kritická sekce)**

Největším technickým úskalím je síťová komunikace na Android API 23\.

### **6.1 TLS 1.2 Patch**

Standardní OkHttpClient na Androidu 6.0 může při spojení s generativelanguage.googleapis.com selhat, protože Google vyžaduje moderní šifrovací sady (Cipher Suites), které starý Android ve výchozím nastavení nenabízí, ačkoliv je umí.

* **Řešení:** Je nutné implementovat vlastní třídu Tls12SocketFactory, která dědí od SSLSocketFactory, a v metodě createSocket explicitně povolí TLS 1.2 (socket.setEnabledProtocols(new String { "TLSv1.2" });).  
* **Provider Installer:** Alternativně lze v onCreate zavolat ProviderInstaller.installIfNeeded(getApplicationContext()) ze služeb Google Play Services, což aktualizuje bezpečnostní poskytovatele v systému.6 Report doporučuje kombinaci obou přístupů pro maximální robustnost.

## ---

**7\. Prompt pro generování kódu**

Na základě výše uvedené analýzy byl sestaven detailní prompt. Tento prompt je navržen tak, aby obešel běžné chyby generativních AI (jako je použití příliš nových knihoven) a dodal funkční kód pro specifické prostředí robota Pepper.  
Tento prompt zadejte do **Claude Code** (protože má větší kontextové okno a lepší schopnosti psaní kódu než standardní Gemini chat) nebo do **Gemini Advanced**.

### ---

**Prompt pro zkopírování:**

Act as a Senior Android Robotics Engineer specializing in SoftBank Robotics Pepper (QiSDK) and Legacy Android Development (API Level 23).  
Task:  
Create a complete, single-file (or modularly structured) Android Activity source code in Kotlin for a Pepper Robot application. The application will act as a conversational wrapper for the Google Gemini API (model gemini-1.5-flash).  
Environment Constraints (CRITICAL):

1. Target OS: Android 6.0 (Marshmallow), API Level 23\.  
2. Hardware: Pepper Robot Tablet (Low RAM, legacy architecture).  
3. SDK: QiSDK (com.aldebaran:qisdk).

Requirements:

1. **Architecture:**  
   * Use Retrofit2 and OkHttp3 for networking.  
   * Implement a Tls12SocketFactory class to force TLS 1.2 support, as Android 6.0 disables it by default on some sockets. This is required to connect to Google APIs.  
   * Use Gson for JSON serialization.  
2. **Core Logic (The Loop):**  
   * Implement RobotLifecycleCallbacks.  
   * **Step 1 (Wake Word):** On robot focus gained, use QiSDK Listen or Chat to wait for a trigger word (e.g., "Pepper" or "Start").  
   * **Step 2 (Input):** Once triggered, launch Android's native SpeechRecognizer Intent to capture user input in Czech (cs-CZ). Do NOT use QiChat for open dictation.  
   * **Step 3 (Reasoning):** Send the recognized text to Google Gemini API.  
     * Endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR\_API\_KEY  
     * Maintain a sliding window history of the last 10 messages (User/Model turns) to preserve context.  
     * System Prompt: "Jsi robot Pepper. Odpovídej česky, stručně a spisovně. Jsi fyzický robot, ne AI asistent."  
   * **Step 4 (Output):** Receive the text response. Clean it (remove markdown \* or \#). Use QiSDK SayBuilder to speak it aloud using the robot's native TTS. Use AnimateBuilder to run a random animation (e.g., "BodyTalk/BodyTalk\_1") simultaneously.  
3. **Dependencies:**  
   * Provide the correct build.gradle dependency lines for Retrofit, OkHttp, and QiSDK that are compatible with API 23 (e.g., Retrofit 2.9.0, OkHttp 3.12.x \- note that OkHttp 4+ requires Android 5.0+ but check API 23 specific compatibility for TLS).  
4. **Error Handling:**  
   * If the API call fails or times out, the robot should say "Omlouvám se, došlo k chybě spojení." in Czech.  
   * Handle OutOfMemoryError gracefully if possible.  
5. **Code Style:**  
   * Write clean, commented Kotlin code.  
   * Do not use Java 8 streams or APIs not available in API 23 unless desugaring is assumed (prefer standard loops).

Output the code in a format ready to be pasted into MainActivity.kt, build.gradle, and AndroidManifest.xml.

## ---

**8\. Závěr a doporučený postup**

Projekt vytvoření LLM wrapperu pro robota Pepper je technicky proveditelný, pokud respektujeme omezení platformy Android 6.0.  
**Shrnutí kroků pro uživatele:**

1. **Výběr služby:** Zůstaňte u **Gemini 1.5 Flash**. Využijte Free Tier. Není třeba aktivovat API v rámci placeného Google One předplatného, stačí vytvořit projekt v Google AI Studio a získat API klíč.  
2. **Vývojové prostředí:** Nainstalujte Android Studio. Vytvořte projekt s minSdkVersion 23\.  
3. **Implementace:** Použijte výše uvedený prompt k vygenerování základního kódu.  
4. **Ladění:** Největší pozornost věnujte části s Tls12SocketFactory. Pokud robot nebude schopen komunikovat s API, problém je na 99 % v SSL handshake. Použijte OkHttp logging interceptor k diagnostice.  
5. **Jazyk:** Ujistěte se, že tablet robota má v nastavení Androidu stažený český balíček pro rozpoznávání řeči (Speech Recognition), jinak bude SpeechRecognizer vyžadovat neustálé online připojení s vyšší latencí.

Tímto způsobem získáte robota, který sice běží na hardwaru z minulé dekády, ale disponuje inteligencí z roku 2026, schopného plynulé a kontextové konverzace v češtině.

## ---

**9\. Detailní technická specifikace a zdůvodnění (Deep Dive)**

V této sekci se hlouběji podíváme na specifické technické detaily, které byly v exekutivním shrnutí pouze nastíněny, abychom zajistili kompletní kontext pro vývoj.

### **9.1 API 23 a knihovny třetích stran**

Výběr správných verzí knihoven je pro tento projekt kritický. Použití nejnovějších verzí (např. OkHttp 5.0 alpha) způsobí pád aplikace okamžitě po spuštění.

* **OkHttp:** Poslední verze oficiálně podporující Android 5.0+ (API 21+) je řada 3.12.x. Ačkoliv novější verze (4.x) technicky podporují Android 5.0, jsou napsány v Kotlinu a mohou mít specifické požadavky na kompilátor, které starší projekty v Android Studiu pro Peppera (často bez plné podpory R8/D8) nemusí zvládnout. **Doporučení:** Použít com.squareup.okhttp3:okhttp:3.12.13 pro maximální stabilitu na API 23\. Tato verze je stále robustní a podporuje moderní HTTP/2, což zrychluje komunikaci s Gemini API.7  
* **Retrofit:** Verze 2.9.0 je kompatibilní s Java 8\. Pro API 23 je nutné v build.gradle povolit Java 8 desugaring (compileOptions { sourceCompatibility JavaVersion.VERSION\_1\_8... }), jinak Retrofit spadne na chybějících interface metodách.

### **9.2 Vliv latence na uživatelský zážitek (UX)**

U humanoidního robota je latence (zpoždění) vnímána mnohem citlivěji než u chatbotu na webu. Pokud Pepper po položení otázky "zamrzne" na 5 sekund, uživatel má tendenci otázku zopakovat nebo odejít.  
**Rozklad latence (Latency Budget):**

1. **Detekce konce řeči (VAD):** 0.5 \- 1.0s (Android SpeechRecognizer čeká na ticho).  
2. **STT Přepis:** 0.5 \- 1.0s (Závisí na délce věty a kvalitě internetu).  
3. **Síťová komunikace (Round-trip):** 0.3 \- 0.5s (Spojení na Google servery).  
4. **LLM Inference (Gemini Flash):** 0.5 \- 1.5s (Generování odpovědi).  
5. **TTS Syntéza (Lokální):** \< 0.2s (Okamžité).

**Celkem:** \~2.0 až 4.2 sekundy.  
To je na hraně únosnosti.

* **Mitigace (Zmírnění):**  
  * **Animace "Thinking":** Okamžitě po detekci konce řeči musí robot spustit animaci (např. podrbání na hlavě, mírný náklon). Tím vizuálně "koupí" čas.  
  * **Předvyplněné výplně:** Robot může náhodně říci "Hmm...", "Dej mi vteřinku..." nebo "To je zajímavá otázka..." ještě *předtím*, než přijde odpověď z LLM. Toto lze implementovat spuštěním krátké Say akce paralelně s odesláním API requestu. Tím se subjektivní latence sníží na nulu.

### **9.3 Budoucí rozšiřitelnost (Future Proofing)**

I když je nyní Gemini Flash nejlepší volbou, trh se mění.

* **Abstrakce služby:** V kódu je vhodné vytvořit rozhraní ILlmService s metodou generateResponse(history: List\<Message\>): String.  
* **Implementace:** Vytvořit třídu GeminiService implementující toto rozhraní. Pokud v budoucnu DeepSeek zlevní nebo vyjde "Claude 4 Haiku" s lepším API, stačí vytvořit DeepSeekService a změnit jeden řádek v inicializaci, aniž by se musela přepisovat logika robota.

Tento strukturovaný přístup zajišťuje, že aplikace bude nejen funkční, ale i udržovatelná a připravená na rychlý vývoj v oblasti AI, navzdory statickému hardwaru, na kterém běží.

### **Seznam použitých zdrojů (Citations)**

Analýza a doporučení v tomto reportu vycházejí z následujících technických podkladů:

* 34 Dokumentace QiSDK, QiChat syntaxe a Listen akce.  
* 7 Specifikace Android API 23, kompatibilita SDK a minSdkVersion požadavky.  
* 32 Podpora českého jazyka v QiSDK a TTS/STT enginech.  
* 1 Ceníky a benchmarky LLM modelů (Gemini, DeepSeek, GPT-4o).  
* 20 Podmínky Google One AI Premium a dostupnost API kreditů.  
* 8 Integrace OpenAI Whisper API a srovnání s nativním ASR.  
* 6 Problémy s SSL/TLS na starších verzích Androidu a NaoQi.  
* 27 Implementace Gemini API na Androidu a doporučené postupy (best practices).  
* 13 Správa paměti a kontextu konverzace na zařízeních s omezenými zdroji.

#### **Citovaná díla**

1. LLM API Pricing Comparison (2025): OpenAI, Gemini, Claude \- IntuitionLabs.ai, použito února 16, 2026, [https://intuitionlabs.ai/articles/llm-api-pricing-comparison-2025](https://intuitionlabs.ai/articles/llm-api-pricing-comparison-2025)  
2. Gemini 1.5 Flash vs GPT-4o: Google's Response to GPT-4o? \- Cody, použito února 16, 2026, [https://meetcody.ai/blog/gemini-1-5-flash-vs-gpt-4o/](https://meetcody.ai/blog/gemini-1-5-flash-vs-gpt-4o/)  
3. GPT-4o mini vs Gemini 1.5 Flash \- Vellum AI, použito února 16, 2026, [https://www.vellum.ai/comparison/gpt-4o-mini-vs-gemini-1-5-flash](https://www.vellum.ai/comparison/gpt-4o-mini-vs-gemini-1-5-flash)  
4. GPT-4o Mini vs DeepSeek-V3 \- Detailed Performance & Feature Comparison \- DocsBot AI, použito února 16, 2026, [https://docsbot.ai/models/compare/gpt-4o-mini/deepseek-v3](https://docsbot.ai/models/compare/gpt-4o-mini/deepseek-v3)  
5. DeepSeek V3 vs GPT-4o: Battle for Translation Supremacy, použito února 16, 2026, [https://www.machinetranslation.com/blog/deepseek-v3-vs-gpt-4o](https://www.machinetranslation.com/blog/deepseek-v3-vs-gpt-4o)  
6. cast byte\[\] to serializable Object; communicate with audio device · Issue \#1 · aldebaran/libqi-java \- GitHub, použito února 16, 2026, [https://github.com/aldebaran/libqi-java/issues/1](https://github.com/aldebaran/libqi-java/issues/1)  
7. SDK Platform release notes | Android Studio, použito února 16, 2026, [https://developer.android.com/tools/releases/platforms](https://developer.android.com/tools/releases/platforms)  
8. arXiv:2402.07095v1 \[cs.RO\] 11 Feb 2024, použito února 16, 2026, [https://arxiv.org/pdf/2402.07095](https://arxiv.org/pdf/2402.07095)  
9. Speech to text | OpenAI API, použito února 16, 2026, [https://developers.openai.com/api/docs/guides/speech-to-text/](https://developers.openai.com/api/docs/guides/speech-to-text/)  
10. Installing the Pepper SDK plug-in — QiSDK, použito února 16, 2026, [https://android.aldebaran.com/sdk/doc/pepper-sdk/ch1\_gettingstarted/installation.html](https://android.aldebaran.com/sdk/doc/pepper-sdk/ch1_gettingstarted/installation.html)  
11. Compatibility framework changes (Android 15), použito února 16, 2026, [https://developer.android.com/about/versions/15/reference/compat-framework-changes](https://developer.android.com/about/versions/15/reference/compat-framework-changes)  
12.   
13. Memory management best practices | Places SDK for Android \- Google for Developers, použito února 16, 2026, [https://developers.google.com/maps/documentation/places/android-sdk/memory-best-practices](https://developers.google.com/maps/documentation/places/android-sdk/memory-best-practices)  
14. Manage your app's memory | App quality \- Android Developers, použito února 16, 2026, [https://developer.android.com/topic/performance/memory](https://developer.android.com/topic/performance/memory)  
15. How to make Pepper Robot speak or do an animation after clicking a button on the tablet using java in Android Studio? \- Stack Overflow, použito února 16, 2026, [https://stackoverflow.com/questions/74812410/how-to-make-pepper-robot-speak-or-do-an-animation-after-clicking-a-button-on-the](https://stackoverflow.com/questions/74812410/how-to-make-pepper-robot-speak-or-do-an-animation-after-clicking-a-button-on-the)  
16. Pepper is a character — QiSDK, použito února 16, 2026, [https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch6\_ux/chap2.html](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch6_ux/chap2.html)  
17. Sharing audio input | Android media \- Android Developers, použito února 16, 2026, [https://developer.android.com/media/platform/sharing-audio-input](https://developer.android.com/media/platform/sharing-audio-input)  
18. QiChat \- Syntax — Aldebaran 2.5.11.14a documentation, použito února 16, 2026, [http://doc.aldebaran.com/2-5/naoqi/interaction/dialog/dialog-syntax\_full.html](http://doc.aldebaran.com/2-5/naoqi/interaction/dialog/dialog-syntax_full.html)  
19. QiChat \- Syntax \- Pepper SDK for Android, použito února 16, 2026, [https://android.aldebaran.com/sdk/doc/pepper-sdk/ch4\_api/conversation/qichat/qichat\_syntax.html](https://android.aldebaran.com/sdk/doc/pepper-sdk/ch4_api/conversation/qichat/qichat_syntax.html)  
20. AI Mode in Google Search and AI Overviews get Gemini upgrades, použito února 16, 2026, [https://blog.google/innovation-and-ai/technology/developers-tools/gdp-premium-ai-pro-ultra/](https://blog.google/innovation-and-ai/technology/developers-tools/gdp-premium-ai-pro-ultra/)  
21. Billing | Gemini API | Google AI for Developers, použito února 16, 2026, [https://ai.google.dev/gemini-api/docs/billing](https://ai.google.dev/gemini-api/docs/billing)  
22. Manage your AI credits with Google One, použito února 16, 2026, [https://support.google.com/googleone/answer/16287445?hl=en](https://support.google.com/googleone/answer/16287445?hl=en)  
23. 7 Top AI APIs for Developers in 2026 \- Strapi, použito února 16, 2026, [https://strapi.io/blog/ai-apis-developers-comparison](https://strapi.io/blog/ai-apis-developers-comparison)  
24. Claude Subscriptions are up to 36x cheaper than API (and why "Max 5x" is the real sweet spot) : r/ClaudeAI \- Reddit, použito února 16, 2026, [https://www.reddit.com/r/ClaudeAI/comments/1qpcj8q/claude\_subscriptions\_are\_up\_to\_36x\_cheaper\_than/](https://www.reddit.com/r/ClaudeAI/comments/1qpcj8q/claude_subscriptions_are_up_to_36x_cheaper_than/)  
25. did the math on Claude Code pricing. how are other providers surviving this?? \- Reddit, použito února 16, 2026, [https://www.reddit.com/r/ClaudeAI/comments/1qfosa6/did\_the\_math\_on\_claude\_code\_pricing\_how\_are\_other/](https://www.reddit.com/r/ClaudeAI/comments/1qfosa6/did_the_math_on_claude_code_pricing_how_are_other/)  
26. Pricing \- Claude API Docs, použito února 16, 2026, [https://platform.claude.com/docs/en/about-claude/pricing](https://platform.claude.com/docs/en/about-claude/pricing)  
27. Best practices | Android Studio, použito února 16, 2026, [https://developer.android.com/studio/gemini/best-practices](https://developer.android.com/studio/gemini/best-practices)  
28. Chat with Gemini | Android Studio, použito února 16, 2026, [https://developer.android.com/studio/gemini/chat](https://developer.android.com/studio/gemini/chat)  
29. openai/whisper: Robust Speech Recognition via Large-Scale Weak Supervision \- GitHub, použito února 16, 2026, [https://github.com/openai/whisper](https://github.com/openai/whisper)  
30. Install and Run Whisper C++ on an Android phone \- YouTube, použito února 16, 2026, [https://www.youtube.com/watch?v=sLFvyW1r3tk](https://www.youtube.com/watch?v=sLFvyW1r3tk)  
31. I made an open-source Android transcription keyboard using Whisper AI. You can dictate with auto punctuation and translation to many languages. \- Reddit, použito února 16, 2026, [https://www.reddit.com/r/android\_devs/comments/1dgda31/i\_made\_an\_opensource\_android\_transcription/](https://www.reddit.com/r/android_devs/comments/1dgda31/i_made_an_opensource_android_transcription/)  
32. Language.CZECH \- qisdk, použito února 16, 2026, [https://qisdk.softbankrobotics.com/sdk-beta/doc/javadoc/qisdk/com.aldebaran.qi.sdk.object.locale/-language/-c-z-e-c-h.html](https://qisdk.softbankrobotics.com/sdk-beta/doc/javadoc/qisdk/com.aldebaran.qi.sdk.object.locale/-language/-c-z-e-c-h.html)  
33. Building a Smarter Chat History Manager for AI Chatbots (Session-Level Memory & Context Retrieval) : r/Rag \- Reddit, použito února 16, 2026, [https://www.reddit.com/r/Rag/comments/1o3njjv/building\_a\_smarter\_chat\_history\_manager\_for\_ai/](https://www.reddit.com/r/Rag/comments/1o3njjv/building_a_smarter_chat_history_manager_for_ai/)  
34. aldebaran/qisdk-tutorials \- GitHub, použito února 16, 2026, [https://github.com/aldebaran/qisdk-tutorials](https://github.com/aldebaran/qisdk-tutorials)  
35. Mastering Execute — QiSDK, použito února 16, 2026, [https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4\_api/conversation/tuto/execute\_tutorial.html](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/conversation/tuto/execute_tutorial.html)  
36. QiChat Language — QiSDK, použito února 16, 2026, [https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4\_api/conversation/qichat/qichat\_index.html](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/conversation/qichat/qichat_index.html)  
37. Listen — QiSDK, použito února 16, 2026, [https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4\_api/conversation/reference/listen.html](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/conversation/reference/listen.html)  
38. Chat — QiSDK, použito února 16, 2026, [https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4\_api/conversation/reference/chat.html](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/conversation/reference/chat.html)  
39. Android Studio \- Unable to run app on device minSdk(API 23, N) \!= device Sdk(API 22), použito února 16, 2026, [https://stackoverflow.com/questions/36546036/android-studio-unable-to-run-app-on-device-minsdkapi-23-n-device-sdkapi](https://stackoverflow.com/questions/36546036/android-studio-unable-to-run-app-on-device-minsdkapi-23-n-device-sdkapi)  
40. Supported voices and languages | Cloud Text-to-Speech \- Google Cloud Documentation, použito února 16, 2026, [https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types](https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types)  
41. deepseek-chat vs GPT-4o-mini \- LLM Comparison \- AnotherWrapper, použito února 16, 2026, [https://anotherwrapper.com/tools/llm-pricing/deepseek-chat/gpt-4o-mini](https://anotherwrapper.com/tools/llm-pricing/deepseek-chat/gpt-4o-mini)  
42. Build multi-turn conversations (chat) using the Gemini API | Firebase AI Logic \- Google, použito února 16, 2026, [https://firebase.google.com/docs/ai-logic/chat](https://firebase.google.com/docs/ai-logic/chat)