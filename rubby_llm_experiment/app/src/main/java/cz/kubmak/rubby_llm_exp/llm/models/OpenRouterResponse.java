package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenRouterResponse {
    @SerializedName("choices")
    private List<Choice> choices;

    public String getResponseText() {
        if (choices != null && !choices.isEmpty()) {
            Choice firstChoice = choices.get(0);
            if (firstChoice != null && firstChoice.message != null) {
                return firstChoice.message.content;
            }
        }
        return null;
    }

    public static class Choice {
        @SerializedName("message")
        public Message message;
    }

    public static class Message {
        @SerializedName("content")
        public String content;
    }

    @SerializedName("usage")
    private Usage usage;

    public int getTotalTokens() {
        if (usage != null) return usage.totalTokens;
        return 0;
    }

    public static class Usage {
        @SerializedName("total_tokens")
        public int totalTokens;
    }
}
