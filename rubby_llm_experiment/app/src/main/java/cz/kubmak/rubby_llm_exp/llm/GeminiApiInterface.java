package cz.kubmak.rubby_llm_exp.llm;

import cz.kubmak.rubby_llm_exp.llm.models.GeminiRequest;
import cz.kubmak.rubby_llm_exp.llm.models.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiInterface {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest request
    );
}
