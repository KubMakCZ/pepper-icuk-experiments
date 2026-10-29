package cz.kubmak.rubby_llm_exp.llm;

import java.util.ArrayList;
import java.util.List;

import cz.kubmak.rubby_llm_exp.llm.models.Content;
import cz.kubmak.rubby_llm_exp.llm.models.OpenRouterMessage;
import cz.kubmak.rubby_llm_exp.llm.models.OpenRouterRequest;
import cz.kubmak.rubby_llm_exp.llm.models.OpenRouterResponse;
import cz.kubmak.rubby_llm_exp.network.NetworkClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenRouterService implements ILlmService {

    private static final String TAG = "OpenRouterService";
    private static final String BASE_URL = "https://openrouter.ai/";

    private final OpenRouterApiInterface api;
    private final String apiKey;
    private volatile String modelName;

    public OpenRouterService(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(NetworkClient.getInstance())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.api = retrofit.create(OpenRouterApiInterface.class);
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Override
    public cz.kubmak.rubby_llm_exp.llm.models.LlmResult generateResponse(List<Content> history) throws Exception {
        List<OpenRouterMessage> messages = new ArrayList<>();
        messages.add(new OpenRouterMessage("system", SYSTEM_PROMPT));

        for (int i = 0; i < history.size(); i++) {
            Content content = history.get(i);
            String role = content.getRole().equals("user") ? "user" : "assistant";
            String text = content.getTextContent();
            // Nektere free modely na OpenRouteru (napr. Liquid LFM nebo Gemma) ignoruji "system" roli,
            // proto k poslednimu dotazu uzivatele pripojime jasnou instrukci identity Rubby na ICUKu:
            if (i == history.size() - 1 && "user".equals(role)) {
                text = "[DŮLEŽITÉ PRAVIDLO: Jsi humanoidní robotka Rubby (robot Pepper) a nacházíš se na ICUKu (Inovační centrum Ústeckého kraje) na ulici Velká Hradební v Ústí nad Labem. " +
                       "NIKDY o sobě neříkej, že jsi LFM, Liquid Foundation Model, Liquid AI ani jazykový model! Vždy vystupuj výhradně jako robotka Rubby z ICUKu. " +
                       "Mluv česky s diakritikou, přátelsky, stručně a bez hvězdiček či emotikonů. " +
                       "Pokud tě uživatel žádá o předvedení pohybu, přidej na konec odpovědi značku [ANIMACE:kategorie]. Dostupné kategorie: " +
                       cz.kubmak.rubby_llm_exp.animation.AnimationManager.getCategoriesForPrompt() + ".]\nDotaz uživatele: " + text;
            }
            messages.add(new OpenRouterMessage(role, text));
        }

        String activeModel = this.modelName;
        cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).d(TAG, "Odesilam request pres OpenRouter s modelem " + activeModel);

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                OpenRouterRequest request = new OpenRouterRequest(activeModel, messages);
                String authHeader = "Bearer " + apiKey;
                Response<OpenRouterResponse> response = api.generateContent(
                        authHeader,
                        "https://github.com/kubmak/rubby_llm_exp",
                        "Rubby LLM Experiment",
                        request
                ).execute();

                if (response.isSuccessful()) {
                    OpenRouterResponse body = response.body();
                    if (body == null) {
                        throw new Exception("Prazdna odpoved z OpenRouter API");
                    }

                    String text = body.getResponseText();
                    if (text == null || text.isEmpty()) {
                        throw new Exception("OpenRouter API vratila prazdny text");
                    }

                    // Pojistka proti hardcoded identite nekterych malych free modelu (napr. Liquid AI LFM)
                    text = text.replaceAll("(?i)Liquid Foundation Model", "robotka Rubby")
                               .replaceAll("(?i)\\bLFM\\b", "Rubby")
                               .replaceAll("(?i)vytvořený týmem Liquid AI|vyvinutý týmem Liquid AI|od společnosti Liquid AI|týmem Liquid AI|Liquid AI", "z Inovačního centra Ústeckého kraje (ICUK)");

                    cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).d(TAG, "Odpoved OpenRouter (" + activeModel + "): " + text);
                    return new cz.kubmak.rubby_llm_exp.llm.models.LlmResult(text, body.getTotalTokens());
                }

                int code = response.code();
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "neznama chyba";
                cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).w(TAG, "API chyba " + code + " (pokus " + attempt + "/" + MAX_RETRIES + "): " + errorBody);

                // Pokud konkretni free model uz neni dostupny (404 / 400), automaticky prepneme na openrouter/free
                if ((code == 404 || code == 400) && !"openrouter/free".equals(activeModel) && attempt < MAX_RETRIES) {
                    cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).i(TAG, "Model " + activeModel + " neni dostupny, prepinam automaticky na openrouter/free...");
                    activeModel = "openrouter/free";
                    continue;
                }

                if ((code == 429 || code == 503) && attempt < MAX_RETRIES) {
                    cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).i(TAG, "Cekam " + RETRY_DELAY_MS + "ms pred dalsim pokusem...");
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                    continue;
                }

                throw new Exception("OpenRouter API chyba " + code + ": " + errorBody);

            } catch (java.io.IOException e) {
                lastException = e;
                cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).w(TAG, "Sitova chyba (pokus " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        throw lastException != null ? lastException : new Exception("Vsechny pokusy pres OpenRouter selhaly");
    }
}
