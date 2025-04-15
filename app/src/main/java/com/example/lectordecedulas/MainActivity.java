package com.example.lectordecedulas;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cedulaInput = findViewById(R.id.cedulaInput);
        Button verifyButton = findViewById(R.id.verifyButton);
        Button reportButton = findViewById(R.id.reportButton);
        resultText = findViewById(R.id.resultText);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        // Establecer foco inicial en el EditText
        cedulaInput.requestFocus();

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cedula = cedulaInput.getText().toString().trim();
                if (cedula.isEmpty()) {
                    resultText.setText("Por favor, escanea una cédula");
                    resultText.setTextColor(Color.BLACK);
                    cedulaInput.requestFocus();
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
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
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
        loadingIndicator.setVisibility(View.VISIBLE);
        resultText.setText("Verificando cédula...");
        resultText.setTextColor(Color.BLACK);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.13:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Respuesta> call = apiService.verificarCedula(cedula);

        call.enqueue(new Callback<Respuesta>() {
            @Override
            public void onResponse(Call<Respuesta> call, Response<Respuesta> response) {
                loadingIndicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Respuesta respuesta = response.body();
                    if (respuesta != null) {
                        String mensaje = respuesta.getMensaje();
                        resultText.setText(mensaje);

                        // Reproducir sonido según el resultado
                        MediaPlayer mediaPlayer = null;
                        if (mensaje.contains("puedes almorzar")) {
                            resultText.setTextColor(Color.GREEN);
                            mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.acceso_consedido);
                        } else if (mensaje.equals("Cédula no registrada o inactiva")) {
                            resultText.setTextColor(Color.RED);
                            mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.acceso_negado);
                        } else if (mensaje.equals("Ya almorzaste hoy")) {
                            resultText.setTextColor(Color.YELLOW);
                            mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.duplicado);
                        } else {
                            resultText.setTextColor(Color.BLACK);
                        }

                        // Reproducir el sonido si existe
                        if (mediaPlayer != null) {
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(mp -> mp.release());
                        }

                        // Limpiar y devolver foco al EditText
                        cedulaInput.setText("");
                        cedulaInput.requestFocus();
                        cedulaInput.selectAll();
                    }
                } else {
                    resultText.setText("Error al conectar con el servidor");
                    resultText.setTextColor(Color.BLACK);
                    cedulaInput.requestFocus();
                }
            }

            @Override
            public void onFailure(Call<Respuesta> call, Throwable t) {
                loadingIndicator.setVisibility(View.GONE);
                resultText.setText("Error de conexión: " + t.getMessage());
                resultText.setTextColor(Color.BLACK);
                cedulaInput.requestFocus();
            }
        });
    }

    private void descargarYEnviarReporte() {
        loadingIndicator.setVisibility(View.VISIBLE);
        resultText.setText("Generando reporte...");
        resultText.setTextColor(Color.BLACK);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.13:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.descargarReporte();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                loadingIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File dir = getExternalFilesDir(null);
                        if (dir == null || !dir.exists()) {
                            dir.mkdirs();
                        }
                        File file = new File(dir, "reporte_almuerzos_completo.xlsx");

                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(response.body().bytes());
                        fos.flush();
                        fos.close();

                        resultText.setText("Reporte descargado: " + file.getAbsolutePath());

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
                            resultText.setTextColor(Color.BLACK);
                        } catch (android.content.ActivityNotFoundException ex) {
                            resultText.setText("No hay apps de correo instaladas");
                            resultText.setTextColor(Color.BLACK);
                        }

                    } catch (IOException e) {
                        resultText.setText("Error al guardar el archivo: " + e.getMessage());
                        resultText.setTextColor(Color.BLACK);
                    }
                } else {
                    resultText.setText("Error al descargar el reporte: Código " + response.code());
                    resultText.setTextColor(Color.BLACK);
                }
                cedulaInput.requestFocus();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingIndicator.setVisibility(View.GONE);
                resultText.setText("Error de conexión: " + t.getMessage());
                resultText.setTextColor(Color.BLACK);
                cedulaInput.requestFocus();
            }
        });
    }
}