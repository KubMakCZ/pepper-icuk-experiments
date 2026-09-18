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

    public OpenRouterRequest(String model, List<OpenRouterMessage> messages) {
        this.model = model;
        this.messages = messages;
        this.temperature = 0.7; // Defaultni hodnota
    }
}
