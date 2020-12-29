package com.android.gabriel.autofeeder.api;

import com.android.gabriel.autofeeder.model.Refeicao;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RefeicaoApi {
    @GET("Refeicoes/GetAll")
    Call<List<Refeicao>> getAll(@Header("Authorization") String token,
                                @Query("userId") String userId);

    @GET("Refeicoes/GetProximaRefeicao")
    Call<Refeicao> getProximaRefeicao(@Header("Authorization") String token,
                                      @Query("userId") String userId);

    @PUT("Refeicoes/Put/{id}")
    Call<Refeicao> put(@Header("Authorization") String token,
                       @Path("id") int id,
                       @Body Refeicao refeicao);
}