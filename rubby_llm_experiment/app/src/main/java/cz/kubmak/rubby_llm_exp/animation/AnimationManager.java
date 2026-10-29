package cz.kubmak.rubby_llm_exp.animation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import cz.kubmak.rubby_llm_exp.R;

/**
 * Centralni registr animaci pro Pepper robota (93 animaci ve 44 kategoriich).
 * Animace jsou organizovany do kategorii, ktere LLM muze spustit pres tag [ANIMACE:kategorie].
 */
public class AnimationManager {

    private static final Map<String, int[]> CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();
    private static final Random random = new Random();

    private static void register(String key, String description, int... resources) {
        CATEGORIES.put(key, resources);
        DESCRIPTIONS.put(key, description);
    }

    static {
        // === TANEC A HUDBA ===
        register("dance", "tanec",
                R.raw.dance_b001, R.raw.dance_b002, R.raw.dance_b003, R.raw.dance_b004, R.raw.dance_b005);
        register("disco", "disko tanec", R.raw.disco_a001);
        register("headbang", "rockovy headbanging", R.raw.headbang_a001);
        register("taichi", "cviceni tai-chi", R.raw.taichichuan_a001);
        register("guitar", "hra na kytaru", R.raw.guitar_a001);
        register("saxophone", "hra na saxofon", R.raw.saxophone_a001);
        register("drums", "bubnovani na bubny", R.raw.drumroll_b001);

        // === SPORTY ===
        register("bowling", "hrani bowlingu", R.raw.bowling_a001);
        register("basketball", "basketbal / hod na kos", R.raw.basketball_a001);
        register("baseball", "baseball / odpal palkou", R.raw.baseball_a001);
        register("golf", "hrani golfu", R.raw.golf_a001);
        register("football", "fotbalovy brankar / chytani mice", R.raw.goalkeeper_a001);
        register("kungfu", "bojove umeni kung-fu / karate", R.raw.kungfu_a001);
        register("meditation", "meditace / zen", R.raw.meditation_a001);
        register("muscles", "ukazani svalu / silak", R.raw.showmuscles_a001, R.raw.showmuscles_a002);
        register("ski", "lyzovani", R.raw.ski_a001);
        register("surf", "surfovani na vlne", R.raw.surf_a001);
        register("swim", "plavani", R.raw.swim_a001, R.raw.swim_b001);
        register("scuba", "potapeni pod vodou", R.raw.scuba_a001);
        register("run", "beh na miste", R.raw.walk_run_b001);

        // === ZVIRATA ===
        register("dog", "napodobeni pejska", R.raw.dog_a001);
        register("elephant", "napodobeni slona s chobotem", R.raw.elephant_a001);
        register("cat", "napodobeni kocky / selmy", R.raw.feline_a001);
        register("gorilla", "napodobeni gorily / buseni do hrudi", R.raw.gorilla_a001, R.raw.gorilla_b001);
        register("mouse", "napodobeni mysky", R.raw.mouse_a001);
        register("wolf", "napodobeni vlka / vyti", R.raw.wolf_a001);

        // === ZABAVNE AKCE ===
        register("photo", "vyfoceni fotky / fotograf", R.raw.take_pic_b001, R.raw.take_pic_b002);
        register("drink", "piti napoje", R.raw.drink_right_b001, R.raw.drink_left_b001, R.raw.open_drink_right_b001);
        register("watch", "kontrola hodinek / kolik je hodin", R.raw.check_time_right_b001, R.raw.check_time_left_b001);
        register("sleep", "spanek / zdrimnuti", R.raw.napping_right_b001, R.raw.napping_left_b001);

        // === POZDRAVY A GESTA ===
        register("hello", "pozdrav",
                R.raw.hello_a001, R.raw.hello_a002, R.raw.hello_a003, R.raw.hello_a004, R.raw.hello_a005);
        register("wave", "mavani rukama",
                R.raw.waving_both_hands_b001, R.raw.waving_left_b001, R.raw.waving_right_b001);
        register("bow", "poklona / uklona", R.raw.bowing_b001, R.raw.bow_a001);
        register("salute", "zasalutovani", R.raw.salute_right_b001, R.raw.salute_left_b001);
        register("yes", "souhlas / prikyvnuti",
                R.raw.affirmation_a001, R.raw.affirmation_a002, R.raw.certain_b001);
        register("no", "nesouhlas / zavrteni hlavou",
                R.raw.negation_shake_head_b001, R.raw.negation_both_hands_a001);

        // === EMOCE ===
        register("clap", "tleskani / potlesk", R.raw.clapping_b001);
        register("celebrate", "oslava / radost / vitezstvi",
                R.raw.cheers_b001, R.raw.yeah_b001, R.raw.both_hands_high_b001);
        register("kiss", "vzdusny polibek / pusa", R.raw.kisses_a001);
        register("laugh", "smich / legrace / lechtani",
                R.raw.funny_a001, R.raw.tickling_a001, R.raw.tickling_a002, R.raw.nicereaction_a001, R.raw.nicereaction_a002);
        register("shy", "stydlivost / ostych", R.raw.shy_b001, R.raw.shy_b002);
        register("think", "premysleni / dumani", R.raw.thinking_a001, R.raw.thinking_a002);
        register("search", "hledani / rozhlizeni do dalky",
                R.raw.searching_a001, R.raw.looking_up_right_hand_b001);
        register("sad", "smutek / plac", R.raw.sad_a001, R.raw.sad_a002, R.raw.saddened_b001);
        register("angry", "zlost / rozcileni", R.raw.furious_a001, R.raw.annoyed_b001);
        register("scared", "strach / leknuti / prekvapeni", R.raw.fear_a001, R.raw.surprised_a001);
        register("confused", "zmateni / drbani na hlave",
                R.raw.confused_a001, R.raw.scratch_top_of_head_right_b001);
        register("dizzy", "zavrat / motani hlavy", R.raw.dizzy_a001);
        register("bored", "nuda / odpocinek", R.raw.bored_a001, R.raw.chill_a001);
        register("sneeze", "kychnuti / zakaslani", R.raw.sneeze_b001, R.raw.coughing_right_b001);
    }

    // Prirozene konverzacni animace pro bezne mluveni (kdyz LLM nevybere specifickou akci)
    private static final int[] IDLE_ANIMATIONS = {
            R.raw.affirmation_a001,
            R.raw.affirmation_a002,
            R.raw.exclamation_both_hands_a001,
            R.raw.exclamation_both_hands_a002,
            R.raw.question_both_hand_a001,
            R.raw.question_both_hand_a002,
            R.raw.hello_a001,
            R.raw.nicereaction_a001
    };

    /**
     * Vrati nahodnou animaci z dane kategorie.
     * Pokud kategorie neexistuje, vrati idle animaci.
     */
    public static int getAnimation(String category) {
        if (category == null) return getIdleAnimation();
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
        if (category == null) return false;
        return CATEGORIES.containsKey(category.toLowerCase().trim());
    }

    /**
     * Vrati seznam dostupnych kategorii jako retezec pro system prompt.
     */
    public static String getCategoriesForPrompt() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : DESCRIPTIONS.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey())
              .append(" (")
              .append(entry.getValue())
              .append(")");
        }
        return sb.toString();
    }
}
