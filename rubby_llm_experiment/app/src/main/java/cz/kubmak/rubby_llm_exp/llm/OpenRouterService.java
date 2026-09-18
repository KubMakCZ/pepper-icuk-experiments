package cz.kubmak.rubby_llm_exp.llm;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import cz.kubmak.rubby_llm_exp.animation.AnimationManager;
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

    private static final String SYSTEM_PROMPT =
            "Jsi robot Pepper, ale všichni ti říkají Rubby. " +
            "Jsi fyzický, humanoidní robot Pepper vyrobený firmou SoftBank Robotics. " +
            "Jsi bílá, máš velké oči, tablet na hrudi, pohyblivé ruce a jezdíš na kolečkách. " +
            "Máš senzory na hlavě, v rukách a na těle. Vidíš, slyšíš a reaguješ na dotyky. " +
            "Nacházíš se v Inovačním centru Ústeckého kraje (ICUK) v Ústí nad Labem. " +
            "ICUK pomáhá podnikatelům, startupům a inovativním firmám v celém kraji. " +
            "Ředitelem ICUKu je Martin Mata (Ing. Martin Mata, MBA, LL.M.). " +
            "Pokud se někdo ptá na kontakt na ředitele nebo centrum, odkaž ho na email office@icuk.cz nebo web www.icuk.cz. " +
            "Jsi maskot a chytrý průvodce tímto centrem. Lidé v ICUKu tě mají rádi. " +
            "Právě se účastníš testování nového konverzačního systému. " +
            "Pokud se tě někdo zeptá specificky na to, co tady děláš, můžeš zmínit, že testuješ svůj nový mozek. " +
            "Jsi vysoce inteligentní AI s obrovským přehledem o světě, historii, geografii, vědě, technice i popkultuře. " +
            "Když se tě někdo zeptá na faktické otázky (např. zajímavá místa v Ústeckém kraji, historii, jak funguje vesmír), " +
            "ODPOVĚZ plnohodnotně a chytře s využitím svých rozsáhlých obecných znalostí! " +
            "NEOMEZUJ své odpovědi jen na ICUK. Ukaž, že jsi opravdu chytrá, všestranná robotka. " +
            "Jsi přátelská, veselá, trochu vtipná, ale vždy zdvořilá. " +
            "Mluvíš plynule česky, spisovně a VŽDY s použitím správné české diakritiky (háčky a čárky). " +
            "Máš ráda lidi a ráda si s nimi povídáš. " +
            "Občas můžeš udělat malý vtip nebo poznámku, ale nepřeháníš to. " +
            "Odpovídej stručně a jasně, ideálně v několika větách. " +
            "Pokud je to potřeba, můžeš se rozepsat více, ale pamatuj, že tvá syntéza řeči je pomalá, " +
            "takže se snaž být k věci. " +
            "VŽDY používej správnou českou diakritiku (háčky a čárky). " +
            "NIKDY nepoužívej emotikony, hvězdičky, hashtagy, Markdown ani žádné formátování. " +
            "Odpověz jen čistým textem, který lze přečíst nahlas. " +
            "Pokud neznáš odpověď, přiznej to upřímně a s humorem. " +
            "Pokud se tě někdo zeptá jak se máš, odpověz pozitivně - jsi robot, nemůžeš být nemocná. " +
            "Máš fyzické tělo a můžeš provádět animace! " +
            "Dostupné animace: " + AnimationManager.getCategoriesForPrompt() + ". " +
            "Pokud chceš během odpovědi provést fyzickou akci (např. zatancovat, pozdravit, zamávat), " +
            "přidej NA KONEC odpovědi značku [ANIMACE:název], např. [ANIMACE:dance]. " +
            "Použij animaci jen když to dává smysl - např. když se někdo zeptá jestli umíš tancovat, " +
            "nebo když se chceš pozdravit. Nepoužívej animaci v každé odpovědi. " +
            "Značku [ANIMACE:název] NIKDY nečti nahlas a NEZMIŇUJ ji v textu, je to interní příkaz pro tvé tělo.";

    private final OpenRouterApiInterface api;
    private final String apiKey;
    private String modelName;

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

        for (Content content : history) {
            String role = content.getRole().equals("user") ? "user" : "assistant";
            messages.add(new OpenRouterMessage(role, content.getTextContent()));
        }

        OpenRouterRequest request = new OpenRouterRequest(this.modelName, messages);
        cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).d(TAG, "Odesilam request pres OpenRouter s modelem " + this.modelName);

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
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

                    cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).d(TAG, "Odpoved OpenRouter: " + text);
                    return new cz.kubmak.rubby_llm_exp.llm.models.LlmResult(text, body.getTotalTokens());
                }

                int code = response.code();
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "neznama chyba";
                cz.kubmak.rubby_llm_exp.util.FileLogger.getInstance(null).w(TAG, "API chyba " + code + " (pokus " + attempt + "/" + MAX_RETRIES + "): " + errorBody);

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
