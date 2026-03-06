package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;

public class Part {

    @SerializedName("text")
    private String text;

    public Part(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
