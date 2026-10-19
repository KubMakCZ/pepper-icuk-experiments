package cz.kubmak.rubby_llm_exp.conversation;

import java.util.ArrayList;
import java.util.List;

import cz.kubmak.rubby_llm_exp.llm.models.Content;

/**
 * FIFO buffer konverzacni historie.
 * Drzi maximalne MAX_TURNS vymem (kazda vymena = 1 user + 1 model zprava).
 */
public class ChatHistoryManager {

    private static final int MAX_TURNS = 10;

    private final List<Content> history = new ArrayList<>();

    public synchronized void addUserMessage(String text) {
        // Ochrana proti dvema "user" zpravam za sebou (napr. po predchozi chybe site),
        // ktere by v Gemini API zpusobily chybu 400 Bad Request
        if (!history.isEmpty() && "user".equals(history.get(history.size() - 1).getRole())) {
            history.remove(history.size() - 1);
        }
        history.add(new Content("user", text));
        trimHistory();
    }

    public synchronized void addModelMessage(String text) {
        history.add(new Content("model", text));
        trimHistory();
    }

    public synchronized void removeLastUserMessage() {
        if (!history.isEmpty() && "user".equals(history.get(history.size() - 1).getRole())) {
            history.remove(history.size() - 1);
        }
    }

    public synchronized List<Content> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void clear() {
        history.clear();
    }

    private void trimHistory() {
        // Max 2 * MAX_TURNS zprav (user + model striedave)
        int maxMessages = MAX_TURNS * 2;
        while (history.size() > maxMessages) {
            history.remove(0);
        }
        // Historie musi vzdy zacinat zpravou od "user"
        while (!history.isEmpty() && !"user".equals(history.get(0).getRole())) {
            history.remove(0);
        }
    }
}
