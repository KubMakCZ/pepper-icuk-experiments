package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenRouterRequest {
    @SerializedName("model")
    private String model;

    @SerializedName("messages")
    private List<OpenRouterMessage> messages;
    
    @SerializedName("temperature")
    private double temperature;

    @SerializedName("max_tokens")
    private int maxTokens;

    @SerializedName("top_p")
    private double topP;

    public OpenRouterRequest(String model, List<OpenRouterMessage> messages) {
        this.model = model;
        this.messages = messages;
        this.temperature = 0.7;
        this.maxTokens = 1024;
        this.topP = 0.9;
    }
}
