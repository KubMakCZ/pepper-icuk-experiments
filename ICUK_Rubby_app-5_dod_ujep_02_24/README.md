# 🤖 Rubby — ICUK Promotional App pro Pepper

Interaktivní aplikace pro robota **SoftBank Pepper**, která prezentuje služby a aktivity **ICUK** (Inovační centrum Ústeckého kraje). Robot Rubby mluví česky, reaguje na hlasové příkazy a naviguje uživatele přes dotykový displej.

## ✨ Hlavní funkce

- **Hlasové ovládání v češtině** — uživatel může říct číslo sekce nebo její název a robot se na ni přepne
- **Dotykové menu** — 8 tlačítek na hlavní obrazovce pro rychlou navigaci
- **Automatická řeč** — robot ke každé sekci přednese připravený text (TTS)
- **Splash screen s časovačem** — po 5 minutách nečinnosti se robot vrátí na úvodní obrazovku
- **Animace** — robot umí tleskat a používá fade přechody mezi obrazovkami

## 📋 Sekce aplikace

| # | Sekce | Popis |
|---|-------|-------|
| 1 | Já jsem Rubby | Úvodní představení robota |
| 2 | Inovační centrum Ústeckého kraje | Představení ICUK |
| 3 | Ústecký kraj | Informace o regionu |
| 4 | UJEP | Univerzita J. E. Purkyně |
| 5 | ICUK pro vysokoškoláky | Nabídka pro studenty |
| 6 | ICUK BOOTCAMP | Intenzivní vzdělávací programy |
| 7 | Univerzitní inkubátor | Podpora startupů |
| 8 | Marketing prakticky | Praktické marketingové dovednosti |

## 🏗️ Architektura

```
app/src/main/
├── java/.../pepperapptemplate/
│   ├── MainActivity.java         # Hlavní aktivita, QiSDK lifecycle
│   ├── Executors/
│   │   ├── FragmentExecutor.java  # Přepínání fragmentů přes QiChat
│   │   └── VariableExecutor.java  # Aktualizace UI proměnných
│   ├── Fragments/
│   │   ├── MainFragment.java      # Hlavní menu s 8 tlačítky
│   │   ├── SplashFragment.java    # Úvodní/idle obrazovka
│   │   ├── LoadingFragment.java   # Načítací stav
│   │   └── Screen*Fragment.java   # Jednotlivé obsahové sekce (3–10)
│   └── Utils/
│       ├── ChatData.java          # Konfigurace QiChat, správa témat
│       └── CountDownNoInteraction.java  # Časovač nečinnosti (5 min)
├── res/
│   ├── layout/                    # XML layouty fragmentů
│   ├── raw/                       # QiChat .top soubory (dialogová pravidla)
│   ├── values/                    # Výchozí stringy (EN)
│   └── values-cs/                 # České překlady
└── AndroidManifest.xml
```

### Jak to funguje

1. **Spuštění** → `LoadingFragment` → robot získá fokus (`onRobotFocusGained`)
2. **QiChat se inicializuje** → 13 dialogových témat se načte a spustí
3. **Hlavní menu** → uživatel klikne na tlačítko nebo řekne hlasový příkaz
4. **Navigace** → `FragmentExecutor` přepne fragment + aktivuje příslušné QiChat téma
5. **Nečinnost (5 min)** → automatický návrat na `SplashFragment`

### QiChat dialogová pravidla

Hlasové příkazy jsou definované v `.top` souborech (`res/raw/`):

| Soubor | Účel |
|--------|------|
| `concepts.top` | Sdílené koncepty — pozdravy, příkazy, potvrzení |
| `main.top` | Pravidla hlavního menu + navigace číslem |
| `everything.top` | Globální navigační pravidla podle názvu sekce |
| `screen*.top` | Pravidla specifická pro jednotlivé obrazovky |

**Příklady hlasových příkazů:**
- *„Ahoj"*, *„Čau"*, *„Dobrý den"* → pozdrav
- *„Ukaž jedničku"*, *„Dvojku"*, *„Coworking"* → navigace na sekci
- *„Reset"*, *„Zpět"*, *„Na začátek"* → návrat na hlavní menu
- *„Řekni vtip"* → robot poví IT vtip 😄

## ⚙️ Technologie

| Komponenta | Verze |
|------------|-------|
| Jazyk | Java (hlavní kód) + Kotlin (Gradle) |
| QiSDK | 1.7.5 |
| Android minSdk | 23 (Android 6.0) |
| Android targetSdk | 30 |
| Gradle | 7.2 |
| AGP | 7.1.2 |
| Kotlin | 1.4.21 |

> ⚠️ **Neupgradovat** verze Android Studio, AGP ani Gradle — novější verze rozbíjí Pepper SDK plugin.

## 🚀 Sestavení a nasazení

### Požadavky

- **Android Studio 2021.1.1** (Bumblebee) Patch 3
- **JDK 8**
- Pepper robot nebo Pepper SDK emulátor

### Build

```bash
# Debug build
./gradlew assembleDebug

# Nasazení na připojeného robota / emulátor
./gradlew installDebug
```

> Projekt otevírejte v Android Studiu přímo tuto složku — nikoliv kořen monorepa.

## 📁 Struktura souborů

| Soubor | Popis |
|--------|-------|
| `build.gradle` (root) | Nastavení Gradle pluginů a repozitářů |
| `app/build.gradle` | Závislosti aplikace, SDK verze, ABI split |
| `poznamka.md` | Interní poznámky k fragmentům a QiChat pravidlům |
| `local.properties` | Lokální cesty SDK (gitignored) |

## 🗣️ Lokalizace

Aplikace je primárně v **češtině**. Výchozí stringy (`values/strings.xml`) jsou v angličtině jako fallback, české překlady jsou v `values-cs/strings.xml`. Locale se nastavuje automaticky při startu:

```java
new Locale(Language.CZECH, Region.CZECH_REPUBLIC)
```

## 📝 Poznámky pro vývojáře

- **Nikdy neblokujte hlavní vlákno** — všechny QiSDK akce používejte přes `buildAsync()` / `async()`
- **`qiContext` je dostupný pouze v `onRobotFocusGained()`** — mimo tento callback je `null`
- **Uvolněte zdroje v `onRobotFocusLost()`**
- **Rozpoznávání řeči** funguje spolehlivě pouze na reálném robotovi, ne v emulátoru
- Fragment `ScreenOneFragment` a `ScreenTwoFragment` jsou šablonové/testovací fragmenty, v produkci se nepoužívají
