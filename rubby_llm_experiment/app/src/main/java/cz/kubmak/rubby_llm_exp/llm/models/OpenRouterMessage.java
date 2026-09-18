package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;

public class OpenRouterMessage {
    @SerializedName("role")
    private String role;

    @SerializedName("content")
    private String content;

    public OpenRouterMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
