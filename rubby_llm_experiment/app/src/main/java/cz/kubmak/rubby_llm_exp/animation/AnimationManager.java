package cz.kubmak.rubby_llm_exp.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import cz.kubmak.rubby_llm_exp.R;

/**
 * Centralni registr animaci pro Pepper robota.
 * Animace jsou organizovany do kategorii, ktere LLM muze spustit pres tag [ANIMACE:kategorie].
 *
 * Pro pridani nove animace:
 * 1. Vloz .qianim soubor do res/raw/
 * 2. Pridej R.raw.nazev do prislusne kategorie nize (nebo vytvor novou)
 */
public class AnimationManager {

    private static final Map<String, int[]> CATEGORIES = new LinkedHashMap<>();
    private static final Random random = new Random();

    static {
        // Tanec
        CATEGORIES.put("dance", new int[]{
                R.raw.dance_b001,
                R.raw.dance_b002,
                R.raw.dance_b003,
                R.raw.dance_b004,
                R.raw.dance_b005
        });

        // Pozdrav
        CATEGORIES.put("hello", new int[]{
                R.raw.hello_a001,
                R.raw.hello_a002
        });

        // Mavani
        CATEGORIES.put("wave", new int[]{
                R.raw.waving_both_hands_b001
        });
    }

    // Idle animace - pouziji se pri beznem mluveni (bez specifickeho pozadavku)
    private static final int[] IDLE_ANIMATIONS = {
            R.raw.hello_a001,
            R.raw.hello_a002,
            R.raw.waving_both_hands_b001
    };

    /**
     * Vrati nahodnou animaci z dane kategorie.
     * Pokud kategorie neexistuje, vrati idle animaci.
     */
    public static int getAnimation(String category) {
        int[] anims = CATEGORIES.get(category.toLowerCase().trim());
        if (anims != null && anims.length > 0) {
            return anims[random.nextInt(anims.length)];
        }
        return getIdleAnimation();
    }

    /**
     * Vrati nahodnou idle animaci (pro bezne mluveni).
     */
    public static int getIdleAnimation() {
        return IDLE_ANIMATIONS[random.nextInt(IDLE_ANIMATIONS.length)];
    }

    /**
     * Zjisti, zda kategorie existuje.
     */
    public static boolean hasCategory(String category) {
        return CATEGORIES.containsKey(category.toLowerCase().trim());
    }

    /**
     * Vrati seznam dostupnych kategorii jako retezec pro system prompt.
     * Format: "dance (tanec), hello (pozdrav), wave (mavani)"
     */
    public static String getCategoriesForPrompt() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> entry : CATEGORIES.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey());
            sb.append(" (");
            sb.append(getCategoryDescription(entry.getKey()));
            sb.append(")");
        }
        return sb.toString();
    }

    private static String getCategoryDescription(String category) {
        switch (category) {
            case "dance": return "tanec";
            case "hello": return "pozdrav";
            case "wave": return "mavani";
            default: return category;
        }
    }
}
