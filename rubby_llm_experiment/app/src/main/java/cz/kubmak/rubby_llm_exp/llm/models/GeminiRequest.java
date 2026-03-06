package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class GeminiRequest {

    @SerializedName("system_instruction")
    private SystemInstruction systemInstruction;

    @SerializedName("contents")
    private List<Content> contents;

    @SerializedName("generationConfig")
    private GenerationConfig generationConfig;

    public GeminiRequest(String systemPrompt, List<Content> contents) {
        this.systemInstruction = new SystemInstruction(systemPrompt);
        this.contents = contents;
        this.generationConfig = new GenerationConfig(0.7, 200, 0.9);
    }

    // System instruction - bez role, jen parts
    public static class SystemInstruction {
        @SerializedName("parts")
        private List<Part> parts;

        public SystemInstruction(String text) {
            this.parts = new ArrayList<>();
            this.parts.add(new Part(text));
        }
    }

    public static class GenerationConfig {
        @SerializedName("temperature")
        private double temperature;

        @SerializedName("maxOutputTokens")
        private int maxOutputTokens;

        @SerializedName("topP")
        private double topP;

        public GenerationConfig(double temperature, int maxOutputTokens, double topP) {
            this.temperature = temperature;
            this.maxOutputTokens = maxOutputTokens;
            this.topP = topP;
        }
    }
}
