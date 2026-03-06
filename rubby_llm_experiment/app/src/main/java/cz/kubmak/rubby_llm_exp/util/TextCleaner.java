package cz.kubmak.rubby_llm_exp.util;

/**
 * Cisti text z LLM od Markdown znacek.
 * Pepper by jinak hvezdicky a hashtahy rikal nahlas.
 */
public class TextCleaner {

    public static String clean(String text) {
        if (text == null) return "";

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
