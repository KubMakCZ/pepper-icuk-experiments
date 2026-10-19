package cz.kubmak.rubby_llm_exp.llm;

import android.util.Log;

import java.util.List;

import cz.kubmak.rubby_llm_exp.llm.models.Content;
import cz.kubmak.rubby_llm_exp.llm.models.GeminiRequest;
import cz.kubmak.rubby_llm_exp.llm.models.GeminiResponse;
import cz.kubmak.rubby_llm_exp.animation.AnimationManager;
import cz.kubmak.rubby_llm_exp.network.NetworkClient;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiService implements ILlmService {

    private static final String TAG = "GeminiService";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    private final GeminiApiInterface api;
    private final String apiKey;

    public GeminiService(String apiKey) {
        this.apiKey = apiKey;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(NetworkClient.getInstance())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.api = retrofit.create(GeminiApiInterface.class);
    }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Override
    public cz.kubmak.rubby_llm_exp.llm.models.LlmResult generateResponse(List<Content> history) throws Exception {
        GeminiRequest request = new GeminiRequest(SYSTEM_PROMPT, history);

        cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).d(TAG, "Odesilam request s " + history.size() + " zpravami");

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Response<GeminiResponse> response = api.generateContent(apiKey, request).execute();

                if (response.isSuccessful()) {
                    GeminiResponse body = response.body();
                    if (body == null) {
                        throw new Exception("Prazdna odpoved z Gemini API");
                    }

                    // Logovani finishReason pro diagnostiku (napr. pokud dojdou tokeny)
                    String finishReason = body.getFinishReason();
                    if (finishReason != null && !finishReason.equals("STOP")) {
                        cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).w(TAG, "Upozorneni (v1): Odpoved ukoncena z duvodu: " + finishReason);
                    }

                    String text = body.getResponseText();
                    if (text == null || text.isEmpty()) {
                        throw new Exception("Gemini API vratila prazdny text");
                    }

                    cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).d(TAG, "Odpoved (" + finishReason + "): " + text);
                    return new cz.kubmak.rubby_llm_exp.llm.models.LlmResult(text, body.getTotalTokens());
                }

                int code = response.code();
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "neznama chyba";
                cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).w(TAG, "API chyba " + code + " (pokus " + attempt + "/" + MAX_RETRIES + "): " + errorBody);

                // Retry jen pro 429 (rate limit) a 503 (service unavailable)
                if ((code == 429 || code == 503) && attempt < MAX_RETRIES) {
                    cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).i(TAG, "Cekam " + RETRY_DELAY_MS + "ms pred dalsim pokusem...");
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                    continue;
                }

                throw new Exception("Gemini API chyba " + code + ": " + errorBody);

            } catch (java.io.IOException e) {
                // Sitova chyba - zkusit znovu
                lastException = e;
                cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).w(TAG, "Sitova chyba (pokus " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        throw lastException != null ? lastException : new Exception("Vsechny pokusy selhaly");
    }
}
