package cz.kubmak.rubby_llm_exp.llm;

import cz.kubmak.rubby_llm_exp.llm.models.OpenRouterRequest;
import cz.kubmak.rubby_llm_exp.llm.models.OpenRouterResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouterApiInterface {
    @POST("api/v1/chat/completions")
    Call<OpenRouterResponse> generateContent(
            @Header("Authorization") String authorization,
            @Header("HTTP-Referer") String referer,
            @Header("X-Title") String title,
            @Body OpenRouterRequest request
    );
}
