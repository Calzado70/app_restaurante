package com.example.lectordecedulas;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lectordecedulas.ApiService;
import com.example.lectordecedulas.R;
import com.example.lectordecedulas.Respuesta;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText cedulaInput;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cedulaInput = findViewById(R.id.cedulaInput);
        Button verifyButton = findViewById(R.id.verifyButton);
        resultText = findViewById(R.id.resultText);

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cedula = cedulaInput.getText().toString().trim();
                if (cedula.isEmpty()) {
                    resultText.setText("Por favor, escanea una cédula");
                } else {
                    verificarCedula(cedula);
                }
            }
        });

        // Opcional: Detectar cuando el lector termina de ingresar (si envía un "Enter")
        cedulaInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                    String cedula = cedulaInput.getText().toString().trim();
                    if (!cedula.isEmpty()) {
                        verificarCedula(cedula);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void verificarCedula(String cedula) {
        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.0.22:8000/") // IP de tu PC
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Respuesta> call = apiService.verificarCedula(cedula);

        call.enqueue(new Callback<Respuesta>() {
            @Override
            public void onResponse(Call<Respuesta> call, Response<Respuesta> response) {
                if (response.isSuccessful()) {
                    Respuesta respuesta = response.body();
                    if (respuesta != null) {
                        resultText.setText(respuesta.getMensaje());
                        cedulaInput.setText(""); // Limpiar el campo
                    }
                } else {
                    resultText.setText("Error al conectar con el servidor");
                }
            }

            @Override
            public void onFailure(Call<Respuesta> call, Throwable t) {
                resultText.setText("Error de conexión: " + t.getMessage());
            }
        });
    }
}