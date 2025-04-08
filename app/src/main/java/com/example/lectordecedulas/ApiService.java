package com.example.lectordecedulas;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("verificar-cedula")
    Call<Respuesta> verificarCedula(@Query("cedula") String cedula);
}