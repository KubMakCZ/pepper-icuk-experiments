package cz.kubmak.rubby_llm_exp.llm;

import cz.kubmak.rubby_llm_exp.llm.models.Content;

import java.util.List;

/**
 * Rozhrani pro LLM sluzby.
 * Umoznuje snadne prepnuti mezi Gemini, DeepSeek, Claude apod.
 */
public interface ILlmService {

    /**
     * Odesle konverzacni historii do LLM a vrati textovou odpoved.
     * POZOR: Tato metoda je synchronni - volat MIMO UI thread!
     *
     * @param history seznam zprav (user/model)
     * @return textova odpoved modelu
     * @throws Exception pri chybe spojeni nebo API
     */
    String generateResponse(List<Content> history) throws Exception;
}
