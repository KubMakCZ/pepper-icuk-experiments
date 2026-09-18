package cz.kubmak.rubby_llm_exp.llm.models;

public class LlmResult {
    public String text;
    public int tokensUsed;

    public LlmResult(String text, int tokensUsed) {
        this.text = text;
        this.tokensUsed = tokensUsed;
    }
}
