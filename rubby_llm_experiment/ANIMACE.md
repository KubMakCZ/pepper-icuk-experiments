# Katalog animací robotky Rubby (Pepper QiSDK)

V aplikaci je naimportováno **93 oficiálních pohybových animací** (`res/raw/*.qianim`) ze SoftBank Robotics Pepper SDK, rozdělených do **44 tematických kategorií** ve třídě [`AnimationManager`](app/src/main/java/cz/kubmak/rubby_llm_exp/animation/AnimationManager.java).

Všechny modely (Google Gemini i OpenRouter) mají v systémovém promptu ([`ILlmService.SYSTEM_PROMPT`](app/src/main/java/cz/kubmak/rubby_llm_exp/llm/ILlmService.java)) kompletní seznam těchto kategorií a instrukci, aby na jakoukoliv prosbu o předvedení pohybu nikdy neodpovídaly *„jsem jen AI a nemám tělo“*, ale nadšeně souhlasily a na konec odpovědi připojily značku `[ANIMACE:kategorie]`.

---

## 1. Tanec a hudba

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Počet variant | Příklady hlasových povelů (co říct Rubby) |
| :--- | :--- | :---: | :--- |
| `dance` | Taneční kreace (boggie, robotický tanec, sestavy) | 3 | *„Zatancuj mi něco!“*, *„Ukaž, jak umíš tancovat.“* |
| `disco` | Disco tanec | 1 | *„Zatanči disco!“*, *„Předveď disco styl.“* |
| `headbang` | Rockový headbang (házení hlavou) | 1 | *„Zapař jako na rockovém koncertě!“*, *„Ukaž headbang.“* |
| `taichi` | Pomalé, plynulé cvičení Tai-Chi | 1 | *„Zacvič mi Tai-Chi.“*, *„Ukaž nějaké uklidňující cvičení.“* |
| `guitar` | Hra na elektrickou / vzdušnou kytaru | 1 | *„Zahraj mi na kytaru!“*, *„Ukaž kytarové sólo.“* |
| `saxophone` | Hra na saxofon | 1 | *„Zahraj na saxofon!“*, *„Předveď jazzového saxofonistu.“* |
| `drums` | Hra na bicí soupravu | 1 | *„Zabubnuj mi!“*, *„Ukaž, jak bys hrála na bicí.“* |

---

## 2. Sporty a cvičení

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Počet variant | Příklady hlasových povelů (co říct Rubby) |
| :--- | :--- | :---: | :--- |
| `bowling` | Rozběh a hod bowlingovou koulí | 1 | *„Můžeš mi ukázat, jak bys hrála bowling?“*, *„Hoď strike v bowlingu!“* |
| `basketball` | Dribling a hod na basketbalový koš | 1 | *„Ukaž, jak hraješ basketbal!“*, *„Hoď trojku na koš.“* |
| `baseball` | Odpálení míčku baseballovou pálkou | 1 | *„Předveď baseballový odpal!“*, *„Jak se hraje baseball?“* |
| `golf` | Golfový švih a sledování míčku | 1 | *„Ukaž golfový odpal!“*, *„Zahraj si golf.“* |
| `football` | Fotbalová akce / hlavička | 1 | *„Ukaž, jak hraješ fotbal!“*, *„Dej gól!“* |
| `kungfu` | Bojové umění Kung-Fu / Karate | 2 | *„Ukaž mi kung-fu!“*, *„Předveď nějaký karate chvat.“* |
| `meditation` | Meditace a jóga | 1 | *„Zamedituj si se mnou.“*, *„Ukaž, jak medituješ.“* |
| `muscles` | Napínání bicepsů (kulturista) | 2 | *„Ukaž svaly!“*, *„Jakou máš sílu?“* |
| `ski` | Sjezdové lyžování s hůlkami | 1 | *„Ukaž, jak lyžuješ!“*, *„Předveď jízdu na lyžích.“* |
| `surf` | Jízda na surfu na vlně | 1 | *„Ukaž, jak surfuješ na vlnách!“* |
| `swim` | Plavání (prsa / kraul) | 2 | *„Předveď, jak plaveš!“*, *„Umíš plavat?“* |
| `scuba` | Potápění se šnorchlem / bombou | 1 | *„Ukaž, jak se potápíš pod vodou!“* |
| `run` | Běh na místě | 1 | *„Ukaž, jak rychle běháš!“*, *„Zaběhej si.“* |

---

## 3. Zvířata

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Počet variant | Příklady hlasových povelů (co říct Rubby) |
| :--- | :--- | :---: | :--- |
| `dog` | Pejsek (prosba, škrábání) | 2 | *„Předveď pejska!“*, *„Jak dělá pes?“* |
| `elephant` | Slon (chobot z ruky) | 2 | *„Ukaž slona!“*, *„Předveď, jak vypadá slon.“* |
| `cat` | Kočka (myšička / čistění tlapkou) | 1 | *„Předveď kočičku!“*, *„Jak dělá kočka?“* |
| `gorilla` | Gorila (bušení do hrudi) | 2 | *„Předveď gorilu!“*, *„Ukaž opici.“* |
| `mouse` | Myška | 2 | *„Ukaž malou myšku!“* |
| `wolf` | Vlk (vytí na měsíc) | 2 | *„Předveď vlka!“*, *„Zavyj jako vlk na měsíc.“* |

---

## 4. Zábavné akce a scénky

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Počet variant | Příklady hlasových povelů (co říct Rubby) |
| :--- | :--- | :---: | :--- |
| `photo` | Focení fotoaparátem | 1 | *„Vyfoť mě!“*, *„Udělej mi fotku.“* |
| `drink` | Pití nápoje z kelímku/skleničky | 1 | *„Dej si něco k pití!“*, *„Ukaž, jak piješ kávu.“* |
| `watch` | Kontrola hodinek na zápěstí | 1 | *„Kolik je hodin? Podívej se na hodinky.“* |
| `sleep` | Usínání / zdřímnutí | 1 | *„Jdi spát!“*, *„Ukaž, jak spíš.“* |
| `sneeze` | Kýchnutí (*„Hepčík!“*) | 1 | *„Zkus si kýchnout!“*, *„Nejsi nastydlá?“* |

---

## 5. Pozdravy, souhlas a gesta

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Počet variant | Příklady hlasových povelů (co říct Rubby) |
| :--- | :--- | :---: | :--- |
| `hello` / `wave` | Přátelské zamávání rukou | 4 | *„Ahoj Rubby, zamávej mi!“*, *„Pozdrav návštěvníky.“* |
| `bow` | Zdvořilá úklona | 2 | *„Ukloň se publiku!“*, *„Předveď poklonu.“* |
| `salute` | Vojenské zasalutování | 2 | *„Zasalutuj!“*, *„Pozdrav jako voják.“* |
| `yes` | Přikývnutí na souhlas | 2 | *„Souhlasíš se mnou?“*, *„Zakývej hlavou na souhlas.“* |
| `no` | Zavrtění hlavou / odmítnutí | 2 | *„Ukaž, jak říkáš ne.“* |
| `clap` | Potlesk | 1 | *„Zatleskej mi!“*, *„Zatleskej našemu týmu.“* |
| `celebrate` | Vítězné gesto / oslava (*„Jupí!“*) | 3 | *„Oslav to s námi!“*, *„Ukaž radost z vítězství.“* |
| `kiss` | Poslání vzdušného polibku | 2 | *„Pošli mi pusu!“*, *„Rozluč se vzdušným polibkem.“* |

---

## 6. Emoce a výrazy

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Počet variant | Příklady hlasových povelů (co říct Rubby) |
| :--- | :--- | :---: | :--- |
| `laugh` | Smích a chichotání | 4 | *„Zasměj se!“*, *„Řekni vtip a zasměj se.“* |
| `shy` | Stydlivé gesto (zakrývání obličeje) | 3 | *„Ty se stydíš?“*, *„Ukaž, jak se červenáš.“* |
| `think` | Hluboké zamyšlení (ruka na bradě) | 3 | *„Zamysli se nad tím.“*, *„Ukaž, jak přemýšlíš.“* |
| `search` | Rozhlížení se kolem sebe (ruka nad očima) | 2 | *„Rozhlédni se kolem sebe!“*, *„Koho tady hledáš?“* |
| `sad` | Smutek a svěšená hlava | 3 | *„Ukaž, jak vypadáš, když jsi smutná.“* |
| `angry` | Rozzlobené gesto | 2 | *„Ukaž, jak se zlobíš!“*, *„Předveď vztek.“* |
| `scared` | Leknutí / strach | 2 | *„Baf! Lekla ses?“*, *„Ukaž, jak se bojíš.“* |
| `confused` | Zmatené pokrčení rameny | 2 | *„Ukaž, jak vypadáš, když jsi zmatená.“* |
| `dizzy` | Motání hlavy | 1 | *„Točí se ti hlava?“* |
| `bored` | Nuda / zívání | 1 | *„Ukaž, jak vypadáš, když se nudíš.“* |

---

## 7. Automatická konverzační gesta (`idle`)

Pokud si uživatel s Rubby jen běžně povídá a nežádá konkrétní předvedení, Rubby během mluvení automaticky spouští **přirozená konverzační gesta** z pole `IDLE_ANIMATIONS` (jemná gesta rukama, přikyvování, vysvětlovací pohyby), aby nepůsobila strnule:
- `affirmation_a001`, `affirmation_a002`
- `exclamation_both_hands_a001`, `exclamation_left_hand_a001`
- `question_both_hand_a001`, `question_left_hand_a001`
- `undetermined_a001`
- `thinking_a001`, `thinking_a002`
- `hello_a001`, `hello_a010`
- `laugh_a001`, `laugh_a002`
- `happy_a001`
