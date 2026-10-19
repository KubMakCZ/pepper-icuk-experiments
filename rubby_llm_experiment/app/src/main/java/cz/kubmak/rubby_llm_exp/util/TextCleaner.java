package cz.kubmak.rubby_llm_exp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cisti text z LLM od Markdown znacek a animacnich tagu.
 * Pepper by jinak hvezdicky, hashtahy a interni tagy rikal nahlas.
 */
public class TextCleaner {

    private static final Pattern ANIMATION_TAG_PATTERN =
            Pattern.compile("\\[ANIMACE:([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);

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

        // Odstraneni code blocku (```...```) - musi byt pred inline kodem
        text = text.replaceAll("```[\\s\\S]*?```", "");

        // Odstraneni inline kodu (`code`)
        text = text.replaceAll("`(.+?)`", "$1");

        // Odstraneni Markdown odkazu [text](url) -> text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");

        // Odstraneni bold/italic (**text**, *text*, __text__, _text_, ~~text~~)
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        text = text.replaceAll("\\*(.+?)\\*", "$1");
        text = text.replaceAll("__(.+?)__", "$1");
        text = text.replaceAll("_(.+?)_", "$1");
        text = text.replaceAll("~~(.+?)~~", "$1");

        // Odstraneni headingu (# ## ### atd.) a citaci (>)
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");
        text = text.replaceAll("(?m)^>\\s*", "");

        // Odstraneni odrazek (- item, * item, • item)
        text = text.replaceAll("(?m)^\\s*[\\-\\*•]\\s+", "");

        // Odstraneni emotikonu a piktogramu (surrogate pairs + bezne symboly)
        text = text.replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "");
        text = text.replaceAll("[\\u2600-\\u27BF]", "");

        // Odstraneni osamocenych hvezdicek a mrizek
        text = text.replace("*", "").replace("#", "");

        // Odstraneni vicenasobnych mezer a prazdnych radku
        text = text.replaceAll("[ \\t]{2,}", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.trim();

        return text;
    }
}
