# Mapa projektů

| Složka | Package / appId | Gradle / AGP | SDK | Role |
|--------|-----------------|--------------|-----|------|
| testRubby2 | cz.kubmak.testrubby2 | 7.0.2 / 7.0.4 | 31 | Šablona pro nové projekty: řeč, animace, QiChat, lifecycle |
| rubby_llm_experiment | cz.kubmak.rubby_llm_exp | 7.2 / 7.1.2 | 30 | LLM konverzace (Gemini 2.5, OpenRouter), wakeword, animace, FileLogger, deploy.ps1 |
| ICUK_Rubby_app-5_dod_ujep_02_24 | com.kubmak.icuk_dod | 7.2 / 7.1.2 | 30 | Produkční uvítací/prezentační robot, 10 fragmentů + QiChat (.top) |

Rozhodnutí: projekty zůstávají samostatné. Znovupoužití = kopie vzoru (TLS patch, FileLogger,
wakeword.top, AnimationManager z rubby_llm_experiment).

## Známé dluhy
- ICUK app používá nepřejmenovaný package `com.softbankrobotics.pepperapptemplate`.
- Dokumentace AI asistentů je roztroušená (CLAUDE.md, GEMINI.md, WORKFLOW_PEPPER_LLM.md, navod.md) – kandidát na sloučení.
- testRubby2/0material je stažená HTML dokumentace (velká, nízká hodnota).

## Nápady (potenciál robota)
- Vložit LLM režim do ICUK app jako volitelný dotazový mód (až bude rozhodnuto).
- Detekce lidí / zapojení pohledu, tablet jako UI pro odpovědi, offline fallback odpovědí.
