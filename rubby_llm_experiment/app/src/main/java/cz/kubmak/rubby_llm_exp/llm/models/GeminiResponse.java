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
}
