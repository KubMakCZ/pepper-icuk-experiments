package cz.kubmak.rubby_llm_exp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cisti text z LLM od Markdown znacek a animacnich tagu.
 * Pepper by jinak hvezdicky, hashtahy a interni tagy rikal nahlas.
 */
public class TextCleaner {

    private static final Pattern ANIMATION_TAG_PATTERN =
            Pattern.compile("\\[ANIMACE:(\\w+)\\]", Pattern.CASE_INSENSITIVE);

    /**
     * Extrahuje nazev animacni kategorie z textu, nebo vrati null.
     * Napr. z "[ANIMACE:dance]" vrati "dance".
     */
    public static String extractAnimationTag(String text) {
        if (text == null) return null;
        Matcher matcher = ANIMATION_TAG_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase().trim();
        }
        return null;
    }

    public static String clean(String text) {
        if (text == null) return "";

        // Odstraneni animacnich tagu [ANIMACE:xxx]
        text = ANIMATION_TAG_PATTERN.matcher(text).replaceAll("");

        // Odstraneni bold/italic (**text**, *text*, __text__, _text_)
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        text = text.replaceAll("\\*(.+?)\\*", "$1");
        text = text.replaceAll("__(.+?)__", "$1");
        text = text.replaceAll("_(.+?)_", "$1");

        // Odstraneni headingu (# ## ### atd.)
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");

        // Odstraneni inline kodu (`code`)
        text = text.replaceAll("`(.+?)`", "$1");

        // Odstraneni code blocku (```...```)
        text = text.replaceAll("```[\\s\\S]*?```", "");

        // Odstraneni odrazek (- item, * item)
        text = text.replaceAll("(?m)^[\\-\\*]\\s+", "");

        // Odstraneni vicenasobnych mezer a prazdnych radku
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.trim();

        return text;
    }
}
