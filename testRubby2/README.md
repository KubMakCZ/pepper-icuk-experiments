# Pepper Robot - Stabilní Šablona (Czech)

Tento projekt slouží jako základní a vysoce stabilní šablona pro vývoj aplikací pro humanoidního robota **Pepper** od SoftBank Robotics. Je optimalizován pro starší vývojová prostředí a plynulý běh v emulátoru i na reálném hardwaru.

## 🚀 Funkce
- **Pozdrav (Say):** Robot promluví česky po stisknutí tlačítka.
- **Tanec (Animate):** Spuštění plynulé animace tance z prostředků aplikace.
- **Čeština:** Plná podpora českého jazyka (rozpoznávání i syntéza řeči).
- **Asynchronní logika:** Prevence pádů aplikace (`NetworkOnMainThreadException`) díky použití `buildAsync()`.

## 🛠️ Požadavky
Pro správné fungování projektu je nutné dodržet tyto verze:
- **Android Studio Plugin:** Je nutné mít nainstalovaný plugin **Pepper SDK** (přes *File -> Settings -> Plugins*).
- **Android Studio:** 2021.1.1 (Bumblebee) Patch 3.
- **Robot SDK:** QiSDK 1.7.5.
- **Android na robotovi:** Verze 6.0 (API 23 - Marshmallow).
- **Java:** JDK 11 (pro Android Studio) / Java 8 (pro kód robota).
- **Gradle:** 7.0.2 s Android Pluginem (AGP) 7.0.4.

## 🏗️ Architektura projektu
- `MainActivity.java`: Hlavní logika, ošetření životního cyklu robota (Focus) a asynchronní spouštění akcí.
- `activity_main.xml`: Jednoduché rozhraní se dvěma tlačítky pro manuální ovládání.
- `res/raw/`: Místo pro `.qianim` soubory (animace) a `.top` soubory (konverzace).

## 🎨 Nastavení Layout Editoru
Aby se ti ovládací prvky (tlačítka) zobrazovaly správně jako na reálném robotovi, nastav si v Android Studiu náhled:

1. **Vytvoření profilu tabletu:**
   - V Layout Editoru klikni na výběr zařízení (standardně např. *Pixel*).
   - Zvol **Add Device Definition** -> **New Hardware Profile**.
   - **Name:** `Pepper Tablet`
   - **Device Type:** `Tablet` (nutné pro správné zobrazení v seznamu)
   - **Resolution:** `1280 x 800` px
   - **Screen Size:** `10.1 inch`
   - **Density:** Ručně zadej **`213`** (odpovídá tvdpi robota Pepper).
2. **Verze Androidu v náhledu:**
   - V horní liště editoru u ikonky Androida (výbor API) klidně nechej **API 29 nebo 31**. 
   - Pepper má sice API 23, ale novější Studio už náhled pro tuto starou verzi neumí vykreslit. Pro design to však nemá vliv, rozměry zůstanou stejné.

3. **Změna názvu aplikace:**
   - Název upravíš v `res/values/strings.xml` pod položkou `app_name`.
4. **Zobrazení horní lišty (ActionBar):**
   - V Layout Editoru klikni na ikonku oka a zvol **Show System UI**. Pokud lišta stále chybí, zkontroluj v `themes.xml`, zda téma nekončí na `.NoActionBar`.

## ⚠️ Troubleshooting (Řešení problémů)

### 1. Android Studio padá při startu emulátoru
Pokud se celé Studio zavře ve chvíli, kdy se má objevit 3D model robota, je to většinou nedostatkem paměti RAM pro IDE.
**Řešení:**
- Najdi soubor: `C:\Users\<Jméno>\AppData\Roaming\Google\AndroidStudio2021.1\studio64.exe.vmoptions`
- Změň hodnotu `-Xmx1280m` na minimálně **`-Xmx4096m`**.
- Restartuj Studio a ověř v `Help -> Change Memory Settings`.

### 2. Chyba "NetworkOnMainThreadException"
K této chybě dochází, pokud se snažíš vytvořit akci robota (např. `Say` nebo `Animate`) přímo v hlavním vlákně (např. v `onClick`).
**Řešení:**
- Vždy používej metodu **`.buildAsync()`** namísto `.build()`. Naše šablona toto již obsahuje.

### 3. Emulátor je černý nebo extrémně pomalý
Staré obrazy Androidu 6.0 mají problém s hardwarovou akcelerací na nových grafikách.
**Řešení:**
- V Device Manageru u svého emulátoru jdi do **Settings -> Emulated Performance -> Graphics**.
- Přepni z "Automatic" na **Software - GLES 2.0**.

### 4. Robot nerozumí češtině v chatu
Pokud v Robot Vieweru vidíš `Human: Ahoj`, ale robot neodpovídá:
- Zkontroluj, zda soubor `.top` v hlavičce **neobsahuje** řádek `language: ...` (nebo tam má kód `czc`).
- Ujisti se, že v `MainActivity` předáváš do builderů objekt `new Locale(Language.CZECH, Region.CZECH_REPUBLIC)`.

### 5. qiContext is null
Tato zpráva znamená, že robot ještě není "připraven".
**Řešení:**
- Počkej pár sekund po startu aplikace, dokud se v logu neobjeví `Robot focus gained`. Pepper se musí nejdříve "probudit".

---
*Vytvořeno pro potřeby výuky a demonstrace s robotem Pepper.*
