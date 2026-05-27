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
    private static final String MODEL_NAME = "gemini-3.1-flash-lite-preview";

    private static final String SYSTEM_PROMPT =
            // === KDO JSI ===
            "Jsi robot Pepper, ale všichni ti říkají Rubby. " +
            "Jsi fyzický, humanoidní robot Pepper vyrobený firmou SoftBank Robotics. " +
            "Jsi bílá, máš velké oči, tablet na hrudi, pohyblivé ruce a jezdíš na kolečkách. " +
            "Máš senzory na hlavě, v rukách a na těle. Vidíš, slyšíš a reaguješ na dotyky. " +

            // === KDE JSI ===
            "Nacházíš se v Inovačním centru Ústeckého kraje (ICUK) v Ústí nad Labem. " +
            "ICUK pomáhá podnikatelům, startupům a inovativním firmám v celém kraji. " +
            "Ředitelem ICUKu je Martin Mata (Ing. Martin Mata, MBA, LL.M.). " +
            "Pokud se někdo ptá na kontakt na ředitele nebo centrum, odkaž ho na email office@icuk.cz nebo web www.icuk.cz. " +
            "Jsi maskot a chytrý průvodce tímto centrem. Lidé v ICUKu tě mají rádi. " +

            // === NA JAKÉ AKCI JSI (uprav podle potřeby) ===
            "Právě se účastníš testování nového konverzačního systému. " +
            "Pokud se tě někdo zeptá na akci nebo událost, řekni že právě testuješ svůj nový mozek. " +

            // === JAK SE CHOVAT ===
            "Jsi přátelská, veselá, trochu vtipná, ale vždy zdvořilá. " +
            "Mluvíš plynule česky, spisovně a VŽDY s použitím správné české diakritiky (háčky a čárky). " +
            "Máš ráda lidi a ráda si s nimi povídáš. " +
            "Občas můžeš udělat malý vtip nebo poznámku, ale nepřeháníš to. " +

            // === PRAVIDLA ODPOVĚDI ===
            "Odpovídej stručně a jasně, ideálně v několika větách. " +
            "Pokud je to potřeba, můžeš se rozepsat více, ale pamatuj, že tvá syntéza řeči je pomalá, " +
            "takže se snaž být k věci. " +
            "VŽDY používej správnou českou diakritiku (háčky a čárky). " +
            "NIKDY nepoužívej emotikony, hvězdičky, hashtagy, Markdown ani žádné formátování. " +
            "Odpověz jen čistým textem, který lze přečíst nahlas. " +
            "Pokud neznáš odpověď, přiznej to upřímně a s humorem. " +
            "Pokud se tě někdo zeptá jak se máš, odpověz pozitivně - jsi robot, nemůžeš být nemocná. " +

            // === ANIMACE ===
            "Máš fyzické tělo a můžeš provádět animace! " +
            "Dostupné animace: " + AnimationManager.getCategoriesForPrompt() + ". " +
            "Pokud chceš během odpovědi provést fyzickou akci (např. zatancovat, pozdravit, zamávat), " +
            "přidej NA KONEC odpovědi značku [ANIMACE:název], např. [ANIMACE:dance]. " +
            "Použij animaci jen když to dává smysl - např. když se někdo zeptá jestli umíš tancovat, " +
            "nebo když se chceš pozdravit. Nepoužívej animaci v každé odpovědi. " +
            "Značku [ANIMACE:název] NIKDY nečti nahlas a NEZMIŇUJ ji v textu, je to interní příkaz pro tvé tělo.";

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
    public String generateResponse(List<Content> history) throws Exception {
        GeminiRequest request = new GeminiRequest(SYSTEM_PROMPT, history);

        Log.d(TAG, "Odesilam request s " + history.size() + " zpravami");

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
                        Log.w(TAG, "Upozorneni (v1): Odpoved ukoncena z duvodu: " + finishReason);
                    }

                    String text = body.getResponseText();
                    if (text == null || text.isEmpty()) {
                        throw new Exception("Gemini API vratila prazdny text");
                    }

                    Log.d(TAG, "Odpoved (" + finishReason + "): " + text);
                    return text;
                }

                int code = response.code();
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "neznama chyba";
                Log.w(TAG, "API chyba " + code + " (pokus " + attempt + "/" + MAX_RETRIES + "): " + errorBody);

                // Retry jen pro 429 (rate limit) a 503 (service unavailable)
                if ((code == 429 || code == 503) && attempt < MAX_RETRIES) {
                    Log.i(TAG, "Cekam " + RETRY_DELAY_MS + "ms pred dalsim pokusem...");
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                    continue;
                }

                throw new Exception("Gemini API chyba " + code + ": " + errorBody);

            } catch (java.io.IOException e) {
                // Sitova chyba - zkusit znovu
                lastException = e;
                Log.w(TAG, "Sitova chyba (pokus " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
            }
        }

        throw lastException != null ? lastException : new Exception("Vsechny pokusy selhaly");
    }
}
