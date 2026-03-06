package cz.kubmak.rubby_llm_exp.llm.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Content {

    @SerializedName("role")
    private String role;

    @SerializedName("parts")
    private List<Part> parts;

    public Content(String role, String text) {
        this.role = role;
        this.parts = new ArrayList<>();
        this.parts.add(new Part(text));
    }

    public String getRole() {
        return role;
    }

    public List<Part> getParts() {
        return parts;
    }

    public String getTextContent() {
        if (parts != null && !parts.isEmpty()) {
            return parts.get(0).getText();
        }
        return "";
    }
}
