package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeminiResponse {

    @SerializedName("candidates")
    private List<Candidate> candidates;

    @SerializedName("error")
    private ApiError error;

    /**
     * Ziska textovy obsah prvni odpovedi.
     * @return text odpovedi nebo null pokud neni k dispozici
     */
    public String getResponseText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.content != null) {
                return candidate.content.getTextContent();
            }
        }
        return null;
    }

    /**
     * Ziska duvod ukonceni generovani (napr. STOP, MAX_TOKENS).
     */
    public String getFinishReason() {
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0).finishReason;
        }
        return "UNKNOWN";
    }

    public ApiError getError() {
        return error;
    }

    public static class Candidate {
        @SerializedName("content")
        Content content;

        @SerializedName("finishReason")
        String finishReason;
    }

    public static class ApiError {
        @SerializedName("message")
        public String message;

        @SerializedName("code")
        public int code;
    }

    @SerializedName("usageMetadata")
    private UsageMetadata usageMetadata;

    public int getTotalTokens() {
        if (usageMetadata != null) return usageMetadata.totalTokenCount;
        return 0;
    }

    public static class UsageMetadata {
        @SerializedName("totalTokenCount")
        public int totalTokenCount;
    }
}
