package com.example.lectordecedulas;

import com.example.lectordecedulas.Respuesta;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("verificar-cedula")
    Call<Respuesta> verificarCedula(@Query("cedula") String cedula);

    @GET("generar-reporte")
    Call<ResponseBody> descargarReporte(); // Nuevo método para descargar el Excel
}