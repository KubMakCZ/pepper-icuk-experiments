# Návod: Virtuální robot Pepper a emulátor na Linuxu (testování doma bez robota)

Tento návod popisuje, jak si na Linuxu (Ubuntu / Debian / Mint / Fedora / Arch) zprovoznit kompletní simulaci robota **Pepper (QiSDK 1.7.5 / API 7)** včetně 3D zobrazení pohybů robota, emulátoru tabletu a napojení na webové dálkové ovládání (`http://localhost:8080`).

---

## 1. Jak simulace Peppera na počítači funguje

Simulace ze SoftBank Robotics SDK se skládá ze **dvou propojených částí**:
1. **Virtuální 3D robot (`naoqi-bin` + `Robot Viewer`)** – samostatný linuxový proces, který simuluje fyzické tělo Peppera, jeho klouby, pojezd (`GoTo`), animace (`.qianim`) a dialogy (`QiChat` / `Say`).
2. **Emulátor tabletu (Android AVD – API 23 x86)** – běží v Android Emulatoru a je síťově propojený s virtuálním 3D robotem. Když v aplikaci zavoláš animaci (např. `[ANIMACE:bowling]` nebo přes webový ovladač), 3D postavička Peppera v okně *Robot Vieweru* pohyb reálně předvede!

> **Tip:** Naše aplikace `rubby_llm_experiment` má navíc **hybridní fallback režim** – to znamená, že ji můžeš spustit i v úplně obyčejném Android emulátoru (nebo na svém Android telefonu) bez QiSDK pluginu, pokud chceš doma rychle ladit jen UI, LLM prompty nebo webový ovladač.

---

## 2. Systémové požadavky a balíčky pro Linux (Ubuntu / Debian)

Otevři terminál a nainstaluj potřebné knihovny pro KVM virtualizaci, 32/64bit kompatibilitu Android SDK a OpenGL/Qt knihovny pro 3D prohlížeč robota:

```bash
sudo apt update
sudo apt install -y \
    openjdk-11-jdk \
    qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils \
    libgl1-mesa-glx libgl1-mesa-dri libxcb-xinerama0 libxcb-icccm4 \
    libxcb-image0 libxcb-keysyms1 libxcb-randr0 libxcb-render-util0 \
    libxcb-shape0 libxkbcommon-x11-0 libpulse0 libasound2

# Přidej svého uživatele do skupiny kvm (po přidání se odhlas a přihlas):
sudo usermod -aG kvm $USER
```

> **Proč OpenJDK 11?** Projekt používá Gradle `7.2` a Android Gradle Plugin `7.1.2`, které vyžadují **JDK 11** (s Javou 17 nebo 21 by Gradle hlásil chybu `Unsupported class file major version`).

---

## 3. Instalace Android Studia a Pepper SDK Pluginu

1. **Android Studio:**
   - Stáhni a rozbal Android Studio (např. do `/opt/android-studio` nebo přes JetBrains Toolbox).
   - V **SDK Manageru** (`Tools -> SDK Manager`) nainstaluj:
     - Záložka **SDK Platforms**: zaškrtni *Show Package Details* a nainstaluj **Android 6.0 (Marshmallow) – API 23** a **Android 11.0 (R) – API 30**.
     - Záložka **SDK Tools**: nainstaluj **Android SDK Build-Tools**, **Android Emulator** a **Android SDK Platform-Tools (`adb`)**.

2. **Pepper SDK Plugin pro Android Studio:**
   - V Android Studiu jdi do `File -> Settings -> Plugins -> Marketplace`.
   - Vyhledej **`Pepper SDK`** (od *SoftBank Robotics*) a klikni na **Install**.
   - *(Poznámka: Pokud používáš nejnovější Android Studio Ladybug/Meerkat, ve kterém už starší plugin v Marketplace není vidět, stáhni si buď starší **Android Studio Dolphin (2021.3.1) / Electric Eel (2022.1.1)** z [archivu Android Studia](https://developer.android.com/studio/archive), které můžeš mít na Linuxu rozbalené vedle sebe ve složce `~/android-studio-pepper`, anebo spouštěj virtuálního robota přímo z příkazové řádky – viz sekce 5 níže!)*

3. **Stažení Robot SDK (API 7):**
   - Po restartu Android Studia otevři `Tools -> Pepper SDK -> SDK Manager`.
   - Zaškrtni a stáhni **API 7** (odpovídá QiSDK `1.7.5`, které používáme v projektu).
   - Na Linuxu se SDK automaticky uloží do:
     ```text
     ~/.local/share/Softbank Robotics/RobotSDK/API 7/
     ```

---

## 4. Konfigurace projektu `rubby_llm_experiment` na Linuxu

1. Naklonuj si repozitář:
   ```bash
   git clone https://github.com/KubMakCZ/pepper-icuk-experiments.git
   cd pepper-icuk-experiments/rubby_llm_experiment
   ```
2. Vytvoř soubor `local.properties` v kořeni `rubby_llm_experiment`:
   ```properties
   sdk.dir=/home/TVUJ_UZIVATEL/Android/Sdk
   GEMINI_API_KEY=tvuj_gemini_api_klic
   OPENROUTER_API_KEY=tvuj_openrouter_api_klic
   ```
3. Ověř kompilaci v terminálu s Javou 11:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
   ./gradlew assembleDebug
   ```

---

## 5. Spuštění 3D Virtuálního robota a Emulátoru tabletu

### Varianta A: Přímo z Android Studia (s Pepper SDK pluginem)
1. V horní liště klikni na ikonu **Pepper Robot Viewer** (3D hlavička robota) – otevře se okno s 3D modelem Peppera a na pozadí se spustí lokální `naoqi-bin`.
2. Klikni na ikonu **Pepper Emulator (Tablet)** – spustí se Android 6.0 (API 23) tablet propojený s 3D robotem.
3. Klikni na zelené tlačítko **Run (`Shift+F10`)** – aplikace `rubby_llm_exp` se nainstaluje do emulátoru tabletu a automaticky získá **Robot Focus** od 3D virtuálního robota!

### Varianta B: Spuštění z terminálu bez nutnosti starého Android Studia
Pokud už máš složku `RobotSDK/API 7` staženou, můžeš 3D virtuálního robota i prohlížeč animací spouštět na Linuxu přímo z terminálu:
```bash
# 1. Spuštění 3D prohlížeče animací (Animation Browser):
"$HOME/.local/share/Softbank Robotics/RobotSDK/API 7/tools/bin/animation_browser"

# 2. Spuštění 3D Robot Vieweru (virtuální Pepper):
"$HOME/.local/share/Softbank Robotics/RobotSDK/API 7/tools/bin/robot_viewer"
```

---

## 6. Jak doma v emulátoru testovat Web Ovladač (`http://localhost:8080`)

Protože Android emulátor běží za virtuálním NATem (`10.0.2.15`), stačí v linuxovém terminálu spustit jeden příkaz `adb forward`, který zpřístupní port `8080` z emulátoru přímo na tvém `localhostu`:

```bash
# Přesměrování portu 8080 z emulátoru do tvého Linuxu:
adb forward tcp:8080 tcp:8080
```

Nyní si na Linuxu otevři v prohlížeči:
👉 **`http://localhost:8080/`**

Odtud můžeš v emulátoru a 3D Robot Vieweru pohodlně testovat:
- **Posílání AI promptů** (*„Představ se divákům a řekni zahajovací řeč“*, *„Ukaž, jak hraješ bowling“*),
- **Přímý Teleprompter** (okamžité přečtení textu),
- **Pojezd a otáčení 3D robota v prostoru (`GoTo`)** – v okně *Robot Vieweru* uvidíš, jak se 3D Pepper rozjede dopředu/dozadu nebo se otočí o 45°/90°,
- **Všech 44 kategorií animací** jedním kliknutím,
- **Vlastní Node.js aplikaci** – ze svého Node.js skriptu na Linuxu prostě posíláš HTTP requesty na `http://localhost:8080/api/...`!

---

## 7. Známá specifika emulátoru oproti reálnému robotovi

1. **Hlasové rozpoznávání (`SpeechRecognizer`) v emulátoru:**
   - Obraz Androidu 6.0 (API 23) pro tablet Peppera nemá předinstalované plné Google Voice Typing služby, takže diktování mikrofonem v emulátoru často nefunguje nebo padá.
   - **Řešení pro testování doma:** Používej buď spodní textové pole v Admin obrazovce, nebo posílej prompty přes **Web Ovladač (`http://localhost:8080`)** – v obou případech projde dotaz úplně stejnou LLM i animační cestou jako při hlasovém povelu na reálném robotovi!
2. **Hlas (TTS) v 3D Robot Vieweru vs. Tablet Emulátoru:**
   - Virtuální robot (`naoqi-bin`) zobrazuje to, co robot říká (`Say`), jako **komiksovou bublinu nad hlavou 3D robota** v okně *Robot Vieweru*, zatímco samotné animace (`.qianim`) a pojezd (`GoTo`) vykresluje v reálném čase přímo na 3D modelu Peppera.
