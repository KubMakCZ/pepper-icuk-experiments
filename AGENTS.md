# AGENTS.md – pravidla pro AI asistenty (vibecoding)

Repo: tři samostatné Android/Gradle projekty pro robota Pepper (Android 6, API 23, QiSDK 1.7.5)
pro ICUK Ústí nad Labem. Robot se jmenuje Rubby. Žádný společný root build.
Mapa projektů: docs/PROJEKTY.md. Projektově specifická pravidla: CLAUDE.md / GEMINI.md v podsložkách.

## Pracovní postup
1. Pracuj vždy v JEDNOM projektu (cd do složky). Gradle spouštěj uvnitř něj.
2. Po každé změně zkompiluj: `./gradlew compileDebugJavaWithJavac` (rychlé) nebo `assembleDebug`.
   Při chybě verze Javy nastav JAVA_HOME na JRE Android Studia
   (`C:\Program Files\Android\Android Studio\jre`).
3. Po úspěšné kompilaci commit s popisem v češtině.
4. Rozhodnutí o struktuře: neslučuj projekty, nezaváděj sdílené moduly (rozhodnuto: 3 samostatné projekty).

## Tvrdá omezení (neporušovat)
- minSdk 23, Java 8, žádný Kotlin, žádné Java 8 Streams / moderní Android API.
- NEMĚNIT verze Gradle/AGP (7.0.2/7.0.4 v testRubby2, 7.2/7.1.2 jinde), QiSDK 1.7.5.
- OkHttp max 3.12.x (API 23), nutný TLS 1.2 patch (viz rubby_llm_experiment/network/).
- Veškerý text, řeč, komentáře a commity česky; Locale `new Locale(Language.CZECH, Region.CZECH_REPUBLIC)`.
- Akce robota jen v `onRobotFocusGained(QiContext)`, vždy async, nikdy neblokovat hlavní vlákno; uvolnit v `onRobotFocusLost()`.
- Emulátor: rozpoznávání řeči je nestabilní, testuj řeč na reálném robotovi.

## Tajemství
- API klíče NIKDY do gitu. Patří do `local.properties` (GEMINI_API_KEY, OPENROUTER_API_KEY) -> BuildConfig.
- Soubory `*key*.txt`, `*.log`, screenshoty v rootu projektů jsou v .gitignore.
