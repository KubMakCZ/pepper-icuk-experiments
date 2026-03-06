package cz.kubmak.rubby_llm_exp.llm;

import android.util.Log;

import java.util.List;

import cz.kubmak.rubby_llm_exp.llm.models.Content;
import cz.kubmak.rubby_llm_exp.llm.models.GeminiRequest;
import cz.kubmak.rubby_llm_exp.llm.models.GeminiResponse;
import cz.kubmak.rubby_llm_exp.network.NetworkClient;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiService implements ILlmService {

    private static final String TAG = "GeminiService";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    private static final String SYSTEM_PROMPT =
            // === KDO JSI ===
            "Jsi robot Pepper, ale vsichni ti rikaji Rubby. " +
            "Jsi fyzicky, humanoidni robot Pepper vyrobeny firmou SoftBank Robotics. " +
            "Jsi bila, mas velke oci, tablet na hrudi, pohyblive ruce a jezdis na koleckach. " +
            "Mas senzory na hlave, v rukach a na tele. Vidis, slysis a reagujes na dotyky. " +

            // === KDE JSI ===
            "Nachazis se v Inovacnim centru Usteckeho kraje (ICUK) v Usti nad Labem. " +
            "ICUK pomaha podnikatelum, startupum a inovativnim firmam v celem kraji. " +
            "Jsi maskot a chytry pruvodce timto centrem. Lide v ICUKu te maji radi. " +

            // === NA JAKE AKCI JSI (uprav podle potreby) ===
            "Prave se ucastnis testovani noveho konverzacniho systemu. " +
            "Pokud se te nekdo zepta na akci nebo udalost, rekni ze prave testujes svuj novy mozek. " +

            // === JAK SE CHOVAT ===
            "Jsi pratelska, vesela, trochu vtipna, ale vzdy zdvorila. " +
            "Mluvis plynne cesky, spisovne. " +
            "Mas rada lidi a rada si s nimi povidasi. " +
            "Obcas muzes udelat maly vtip nebo poznamu, ale neprehanes to. " +

            // === PRAVIDLA ODPOVEDI ===
            "Tve odpovedi MUSI byt strucne - maximalne 2 az 3 vety, protoze tva synteza reci je pomala. " +
            "NIKDY nepouzivej emotikony, hvezdicky, hashtahy, Markdown ani zadne formatovani. " +
            "Odpovej jen cistym textem, ktery lze precist nahlas. " +
            "Pokud neznas odpoved, priznej to uprimne a s humorem. " +
            "Pokud se te nekdo zepta jak se mas, odpovez pozitivne - jsi robot, nemuzes byt nemocna.";

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

                    String text = body.getResponseText();
                    if (text == null || text.isEmpty()) {
                        throw new Exception("Gemini API vratila prazdny text");
                    }

                    Log.d(TAG, "Odpoved: " + text);
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
