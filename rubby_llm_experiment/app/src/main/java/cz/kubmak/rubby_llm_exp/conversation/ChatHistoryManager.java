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

    public void addUserMessage(String text) {
        history.add(new Content("user", text));
        trimHistory();
    }

    public void addModelMessage(String text) {
        history.add(new Content("model", text));
        trimHistory();
    }

    public List<Content> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }

    private void trimHistory() {
        // Max 2 * MAX_TURNS zprav (user + model striedave)
        int maxMessages = MAX_TURNS * 2;
        while (history.size() > maxMessages) {
            history.remove(0);
        }
    }
}
