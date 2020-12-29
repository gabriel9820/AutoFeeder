package com.android.gabriel.autofeeder.api;

import com.android.gabriel.autofeeder.model.Usuario;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UsuarioApi {
    @FormUrlEncoded
    @POST("Token")
    Call<Usuario> login(@Field("username") String email,
                        @Field("password") String senha,
                        @Field("grant_type") String grantType);

    @POST("Account/ChangePassword")
    Call<Void> changePassword(@Header("Authorization") String token,
                              @Body Usuario usuario);

    @POST("Account/ForgotPassword")
    Call<Void> forgotPassword(@Body Usuario usuario);

    @POST("Account/Register")
    Call<Void> register(@Body Usuario usuario);
}