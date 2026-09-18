package cz.kubmak.rubby_llm_exp.llm;

import cz.kubmak.rubby_llm_exp.llm.models.Content;
import cz.kubmak.rubby_llm_exp.llm.models.LlmResult;

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
     * @return vysledek s odpovedi a poctem tokenu
     * @throws Exception pri chybe spojeni nebo API
     */
    LlmResult generateResponse(List<Content> history) throws Exception;
}
