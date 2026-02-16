Fragmenty:
    1 - Say Template
    2 - QiVariable (ponechat nepouzivat)
    XXXX
    3 - Má úvodní řeč
    4 - Eventový sál
    5 - Coworking
    6 - Kanceláře a zasedací místnosti k pronájmu
    7 - Virtuální sídlo
    8 - Pod jednou střechou
    9 - Co chystáme v Icuk Space Hradební
    10- Co dělá ICUK


    Fragmenty:
    3 - Má úvodní řeč

    4 - Eventový sál
        - fronc@icuk.cz
    5 - Coworking
        soukupova@icuk.cz.
    6 - Kanceláře a zasedací místnosti k pronájmu
        soukupova@icuk.cz 
    7 - Virtuální sídlo
        cavdarova@icuk.cz
    8 - Pod jednou střechou

    9 - Co chystáme v Icuk Space Hradební

    10- Co dělá ICUK
        icuk.cz
        space.icuk.cz
        usmartzone.cz
        research.icuk.cz


u:(~zobrazit "Má úvodní řeč") ~confirmation ^execute(FragmentExecutor, frag_screen_three)

u:(~zobrazit "Eventový sál") ~confirmation ^execute(FragmentExecutor, frag_screen_four)

u:(~zobrazit ["Coworking" "Koworking" "Kowrkin" "Kovrking"]) ~confirmation ^execute(FragmentExecutor, frag_screen_five)

u:(~zobrazit ["Kaceláře" "Kanceláře a zasedací místnosti k pronájmu"]) ~confirmation ^execute(FragmentExecutor, frag_screen_sex)

u:(~zobrazit "Virtuální sídlo") ~confirmation ^execute(FragmentExecutor, frag_screen_seven)

u:(~zobrazit "Pod jednou střechou") ~confirmation ^execute(FragmentExecutor, frag_screen_eight)

u:(~zobrazit ["Co chystáme v Icuk Space Hradební" "Co chystáme v Icuk spejs Hradební"]) ~confirmation ^execute(FragmentExecutor, frag_screen_nine)

u:(~zobrazit Co dělá ICUK) ~confirmation ^execute(FragmentExecutor, frag_screen_ten)

u:(~reset ~bye) ~confirmation ^execute(FragmentExecutor, frag_main)