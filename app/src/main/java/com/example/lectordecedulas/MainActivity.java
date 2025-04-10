package com.example.lectordecedulas;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import okhttp3.ResponseBody;
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
        Button reportButton = findViewById(R.id.reportButton);
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

        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                descargarYEnviarReporte();
            }
        });

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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.13:8000/")
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
                        String mensaje = respuesta.getMensaje();
                        resultText.setText(mensaje);

                        // Aplicar colores según el mensaje
                        if (mensaje.contains("puedes almorzar")) {
                            resultText.setTextColor(Color.GREEN); // Verde para "puedes almorzar"
                        } else if (mensaje.equals("Cédula no registrada o inactiva")) {
                            resultText.setTextColor(Color.RED); // Rojo para "no registrado"
                        } else if (mensaje.equals("Ya almorzaste hoy")) {
                            resultText.setTextColor(Color.YELLOW); // Amarillo para "ya almorzaste"
                        } else {
                            resultText.setTextColor(Color.BLACK); // Color por defecto para otros casos
                        }

                        cedulaInput.setText("");
                    }
                } else {
                    resultText.setText("Error al conectar con el servidor");
                    resultText.setTextColor(Color.BLACK);
                }
            }

            @Override
            public void onFailure(Call<Respuesta> call, Throwable t) {
                resultText.setText("Error de conexión: " + t.getMessage());
                resultText.setTextColor(Color.BLACK);
            }
        });
    }

    private void descargarYEnviarReporte() {
        resultText.setText("Descargando reporte...");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.13:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.descargarReporte();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Guardar el archivo en el directorio raíz de getExternalFilesDir
                        File dir = getExternalFilesDir(null); // Sin subcarpeta
                        if (dir == null || !dir.exists()) {
                            dir.mkdirs(); // Crear directorio si no existe
                        }
                        File file = new File(dir, "reporte_almuerzos_completo.xlsx");

                        // Escribir el archivo
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(response.body().bytes());
                        fos.flush();
                        fos.close();

                        resultText.setText("Reporte descargado: " + file.getAbsolutePath());

                        // Enviar el correo
                        Uri fileUri = FileProvider.getUriForFile(MainActivity.this, "com.example.lectordecedulas.fileprovider", file);
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        emailIntent.setType("vnd.android.cursor.dir/email");
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Reporte Completo de Almuerzos");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte completo de almuerzos registrados.");
                        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        try {
                            startActivity(Intent.createChooser(emailIntent, "Enviar reporte por correo"));
                            resultText.setText("Reporte listo, selecciona un correo");
                        } catch (android.content.ActivityNotFoundException ex) {
                            resultText.setText("No hay apps de correo instaladas");
                        }

                    } catch (IOException e) {
                        resultText.setText("Error al guardar el archivo: " + e.getMessage());
                    }
                } else {
                    resultText.setText("Error al descargar el reporte: Código " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                resultText.setText("Error de conexión: " + t.getMessage());
            }
        });
    }
}