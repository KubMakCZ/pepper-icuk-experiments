# Katalog animací robotky Rubby (Pepper QiSDK)

V aplikaci je naimportováno **93 oficiálních pohybových animací** (`app/src/main/res/raw/*.qianim`) ze SoftBank Robotics Pepper SDK (`ide-tools/animations`), rozdělených do **44 tematických kategorií** ve třídě [`AnimationManager.java`](app/src/main/java/cz/kubmak/rubby_llm_exp/animation/AnimationManager.java).

> **Technická poznámka k `.anim` vs `.qianim`:** Soubory `.anim` ve složce `Softbank Robotics\RobotSDK\API 7\tools\share\ide-tools\animations` mají identickou vnitřní XML strukturu (`<Animation typeVersion="2.0" xmlns:editor="http://www.aldebaran.com/animation/editor">`) jako soubory `.qianim`, které vytváří `animation_browser.exe` (ten slouží jako grafický prohlížeč a při importu pouze zkopíruje vybraný `.anim` soubor do `res/raw/` s příponou `.qianim`). Všech 93 animací je tak již přímo připraveno a zkompilováno v aplikaci.

Pokud má kategorie **více variant** (např. `dance` má 5 různých tanců, `hello` má 5 pozdravů, `wave` má 3 způsoby mávání), [`AnimationManager.getAnimation()`](app/src/main/java/cz/kubmak/rubby_llm_exp/animation/AnimationManager.java#L113-L120) při každém zavolání **náhodně vybere jednu z variant**, takže Rubby nepůsobí repetitivně.

---

## 1. Tanec a hudba

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Zahrnuté varianty (`res/raw/*.qianim`) | Co říct Rubby (příklady vět) |
| :--- | :--- | :--- | :--- |
| `dance` | Taneční kreace (5 variant – vybírá se náhodně) | `dance_b001`, `dance_b002`, `dance_b003`, `dance_b004`, `dance_b005` | *„Zatancuj mi něco!“*, *„Ukaž, jak umíš tancovat.“* |
| `disco` | Disco tanec | `disco_a001` | *„Zatanči disco!“*, *„Předveď disco styl.“* |
| `headbang` | Rockový headbanging (házení hlavou) | `headbang_a001` | *„Zapař jako na rockovém koncertě!“*, *„Ukaž headbanging.“* |
| `taichi` | Pomalé cvičení Tai-Chi | `taichichuan_a001` | *„Zacvič mi Tai-Chi.“*, *„Ukaž nějaké uklidňující cvičení.“* |
| `guitar` | Hra na elektrickou / vzdušnou kytaru | `guitar_a001` | *„Zahraj mi na kytaru!“*, *„Ukaž kytarové sólo.“* |
| `saxophone` | Hra na saxofon | `saxophone_a001` | *„Zahraj na saxofon!“*, *„Předveď jazzového saxofonistu.“* |
| `drums` | Bubnování na bicí | `drumroll_b001` | *„Zabubnuj mi na bubny!“*, *„Ukaž, jak bys hrála na bicí.“* |

---

## 2. Sporty a cvičení

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Zahrnuté varianty (`res/raw/*.qianim`) | Co říct Rubby (příklady vět) |
| :--- | :--- | :--- | :--- |
| `bowling` | Rozběh a hod bowlingovou koulí | `bowling_a001` | *„Můžeš mi ukázat, jak bys hrála bowling?“*, *„Hoď strike v bowlingu!“* |
| `basketball` | Dribling a hod na basketbalový koš | `basketball_a001` | *„Ukaž, jak hraješ basketbal!“*, *„Hoď míč na koš.“* |
| `baseball` | Odpálení míčku baseballovou pálkou | `baseball_a001` | *„Předveď baseballový odpal!“*, *„Jak se hraje baseball?“* |
| `golf` | Golfový švih a sledování míčku | `golf_a001` | *„Ukaž golfový odpal!“*, *„Zahraj si golf.“* |
| `football` | Fotbalový brankář / chytání míče | `goalkeeper_a001` | *„Ukaž, jak chytáš ve fotbale!“*, *„Předveď fotbalového brankáře.“* |
| `kungfu` | Bojové umění Kung-Fu / Karate | `kungfu_a001` | *„Ukaž mi kung-fu!“*, *„Předveď nějaký karate chvat.“* |
| `meditation` | Meditace a zen | `meditation_a001` | *„Zamedituj si se mnou.“*, *„Ukaž, jak medituješ.“* |
| `muscles` | Napínání bicepsů / silák (2 varianty) | `showmuscles_a001`, `showmuscles_a002` | *„Ukaž svaly!“*, *„Jakou máš sílu?“* |
| `ski` | Sjezdové lyžování s hůlkami | `ski_a001` | *„Ukaž, jak lyžuješ!“*, *„Předveď jízdu na lyžích.“* |
| `surf` | Jízda na surfu na vlně | `surf_a001` | *„Ukaž, jak surfuješ na vlnách!“* |
| `swim` | Plavání (2 varianty) | `swim_a001`, `swim_b001` | *„Předveď, jak plaveš!“*, *„Umíš plavat?“* |
| `scuba` | Potápění pod vodou | `scuba_a001` | *„Ukaž, jak se potápíš pod vodou!“* |
| `run` | Běh na místě | `walk_run_b001` | *„Ukaž, jak rychle běháš!“*, *„Zaběhej si na místě.“* |

---

## 3. Zvířata

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Zahrnuté varianty (`res/raw/*.qianim`) | Co říct Rubby (příklady vět) |
| :--- | :--- | :--- | :--- |
| `dog` | Napodobení pejska | `dog_a001` | *„Předveď pejska!“*, *„Jak dělá pes?“* |
| `elephant` | Napodobení slona s chobotem | `elephant_a001` | *„Ukaž slona!“*, *„Předveď slona s chobotem.“* |
| `cat` | Napodobení kočky / šelmy | `feline_a001` | *„Předveď kočičku!“*, *„Jak dělá kočka?“* |
| `gorilla` | Gorila – bušení do hrudi (2 varianty) | `gorilla_a001`, `gorilla_b001` | *„Předveď gorilu!“*, *„Zabuši si do hrudi jako opice.“* |
| `mouse` | Napodobení myšky | `mouse_a001` | *„Ukaž malou myšku!“* |
| `wolf` | Napodobení vlka / vytí na měsíc | `wolf_a001` | *„Předveď vlka!“*, *„Zavyj jako vlk na měsíc.“* |

---

## 4. Zábavné akce a scénky

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Zahrnuté varianty (`res/raw/*.qianim`) | Co říct Rubby (příklady vět) |
| :--- | :--- | :--- | :--- |
| `photo` | Focení fotoaparátem (2 varianty) | `take_pic_b001`, `take_pic_b002` | *„Vyfoť mě!“*, *„Udělej mi fotku.“* |
| `drink` | Pití nápoje (3 varianty – pravá/levá ruka, otevření) | `drink_right_b001`, `drink_left_b001`, `open_drink_right_b001` | *„Dej si něco k pití!“*, *„Ukaž, jak piješ limonádu.“* |
| `watch` | Kontrola hodinek na zápěstí (2 varianty) | `check_time_right_b001`, `check_time_left_b001` | *„Kolik je hodin? Podívej se na hodinky.“* |
| `sleep` | Usínání / zdřímnutí (2 varianty) | `napping_right_b001`, `napping_left_b001` | *„Jdi spát!“*, *„Ukaž, jak spíš.“* |
| `sneeze` | Kýchnutí nebo zakašlání (2 varianty) | `sneeze_b001`, `coughing_right_b001` | *„Zkus si kýchnout!“*, *„Nejsi nastydlá?“* |

---

## 5. Pozdravy, souhlas a gesta

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Zahrnuté varianty (`res/raw/*.qianim`) | Co říct Rubby (příklady vět) |
| :--- | :--- | :--- | :--- |
| `hello` | Přátelský pozdrav (5 variant) | `hello_a001`, `hello_a002`, `hello_a003`, `hello_a004`, `hello_a005` | *„Ahoj Rubby, pozdrav nás!“* |
| `wave` | Mávání rukama – oběma, levou nebo pravou (3 varianty) | `waving_both_hands_b001`, `waving_left_b001`, `waving_right_b001` | *„Zamávej mi!“*, *„Zamávej návštěvníkům na rozloučenou.“* |
| `bow` | Zdvořilá poklona / úklona (2 varianty) | `bowing_b001`, `bow_a001` | *„Ukloň se publiku!“*, *„Předveď poklonu.“* |
| `salute` | Vojenské zasalutování (2 varianty – pravá/levá) | `salute_right_b001`, `salute_left_b001` | *„Zasalutuj!“*, *„Pozdrav jako voják.“* |
| `yes` | Přikývnutí na souhlas (3 varianty) | `affirmation_a001`, `affirmation_a002`, `certain_b001` | *„Souhlasíš se mnou? Přikývni.“* |
| `no` | Zavrtění hlavou / odmítnutí (2 varianty) | `negation_shake_head_b001`, `negation_both_hands_a001` | *„Ukaž, jak vrtíš hlavou, když říkáš ne.“* |

---

## 6. Emoce a výrazy

| Kategorie (`[ANIMACE:...]`) | Co Rubby předvede | Zahrnuté varianty (`res/raw/*.qianim`) | Co říct Rubby (příklady vět) |
| :--- | :--- | :--- | :--- |
| `clap` | Tleskání / potlesk | `clapping_b001` | *„Zatleskej mi!“*, *„Zatleskej našemu týmu.“* |
| `celebrate` | Vítězné gesto / oslava (3 varianty) | `cheers_b001`, `yeah_b001`, `both_hands_high_b001` | *„Oslav to s námi!“*, *„Ukaž radost z vítězství.“* |
| `kiss` | Poslání vzdušného polibku | `kisses_a001` | *„Pošli mi pusu!“*, *„Rozluč se vzdušným polibkem.“* |
| `laugh` | Smích, legrace a lechtání (5 variant) | `funny_a001`, `tickling_a001`, `tickling_a002`, `nicereaction_a001`, `nicereaction_a002` | *„Zasměj se!“*, *„Řekni vtip a zasměj se.“* |
| `shy` | Stydlivost / ostych (2 varianty) | `shy_b001`, `shy_b002` | *„Ty se stydíš?“*, *„Ukaž, jak se stydíš.“* |
| `think` | Hluboké zamyšlení / dumání (2 varianty) | `thinking_a001`, `thinking_a002` | *„Zamysli se nad tím.“*, *„Ukaž, jak přemýšlíš.“* |
| `search` | Hledání / rozhlížení do dálky (2 varianty) | `searching_a001`, `looking_up_right_hand_b001` | *„Rozhlédni se kolem sebe!“*, *„Koho tady vyhlížíš?“* |
| `sad` | Smutek / pláč (3 varianty) | `sad_a001`, `sad_a002`, `saddened_b001` | *„Ukaž, jak vypadáš, když jsi smutná.“* |
| `angry` | Zlost / rozčilení (2 varianty) | `furious_a001`, `annoyed_b001` | *„Ukaž, jak se zlobíš!“*, *„Předveď vztek.“* |
| `scared` | Strach / leknutí / překvapení (2 varianty) | `fear_a001`, `surprised_a001` | *„Baf! Lekla ses?“*, *„Ukaž, jak se bojíš.“* |
| `confused` | Zmatení / drbání na hlavě (2 varianty) | `confused_a001`, `scratch_top_of_head_right_b001` | *„Ukaž, jak vypadáš, když jsi zmatená.“* |
| `dizzy` | Závrať / motání hlavy | `dizzy_a001` | *„Točí se ti hlava?“*, *„Ukaž, jak se ti motá hlava.“* |
| `bored` | Nuda / odpočinek (2 varianty) | `bored_a001`, `chill_a001` | *„Ukaž, jak vypadáš, když se nudíš.“* |

---

## 7. Automatická konverzační gesta při běžném mluvení (`IDLE_ANIMATIONS`)

Pokud si s Rubby jen běžně povídáš a nežádáš konkrétní předvedení, Rubby během odpovědi automaticky náhodně vybírá přirozená konverzační gesta z pole `IDLE_ANIMATIONS`:
- `affirmation_a001`, `affirmation_a002` (přikyvování)
- `exclamation_both_hands_a001`, `exclamation_both_hands_a002` (vysvětlovací gesta oběma rukama)
- `question_both_hand_a001`, `question_both_hand_a002` (otevřená gesta při dotazu)
- `hello_a001`, `nicereaction_a001` (přátelské gesto)
