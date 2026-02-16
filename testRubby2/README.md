# Pepper Robot - Stabilní Šablona (Czech)

Tento projekt slouží jako **základní a vysoce stabilní šablona** pro všechny další projekty v tomto repozitáři. Je navržen tak, aby demonstroval core funkce robota Pepper v českém jazyce s minimálním rizikem pádu aplikace.

> [!IMPORTANT]
> Pro instrukce k instalaci Android Studia, nastavení emulátoru a řešení běžných chyb (padání Studia, čerý emulátor) se podívejte do **[hlavního README v kořenu repozitáře](../README.md)**.

## 🚀 Implementované ukázky
- **Manuální řeč:** Tlačítko pro pozdrav v češtině pomocí `SayBuilder`.
- **Animace:** Ukázka spuštění tance ze souboru `.qianim` umístěného v `res/raw`.
- **Správa životního cyklu:** Ukázkové ošetření `RobotLifecycleCallbacks` pro bezpečné získání `QiContext`.

## 📂 Důležité soubory v projektu
- `MainActivity.java`: Obsahuje vzorový kód pro asynchronní inicializaci akcí.
- `res/raw/dance_b003.qianim`: Ukázkový taneček.
- `assets/conversation.top`: Základní konverzační soubor pro budoucí rozšíření o chat.

## 🎨 Jak použít tuto šablonu pro nový projekt
1. Zkopírujte celou složku `testRubby2` a přejmenujte ji.
2. V souboru `settings.gradle` a `build.gradle` (v modulu app) upravte názvy projektu.
3. V `res/values/strings.xml` změňte `app_name`.
4. **Layout:** Pro správné zobrazení na tabletu robota si v Layout Editoru nastavte profil:
   - **Resolution:** `1280 x 800` px
   - **Density:** `213` (tvdpi)
   - Viz detailní návod v kořenovém README.

---
*Tento projekt je udržován jako referenční bod pro stabilitu QiSDK v českém prostředí.*
