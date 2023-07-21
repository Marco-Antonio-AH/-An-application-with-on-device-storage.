package com.example.estacionamientoch;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private EditText editTextPlaca;
    private TextView textViewResult;
    private Map<String, String[]> registros;
    private int[] cajonesDisponibles = {6, 6, 4};

    private static final int PRECIO_PISO_1 = 15;
    private static final int PRECIO_PISO_2 = 14;
    private static final int PRECIO_PISO_3 = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextPlaca = findViewById(R.id.editTextPlaca);
        textViewResult = findViewById(R.id.textViewResult);

        registros = new HashMap<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void guardarRegistro(View view) {
        String placa = editTextPlaca.getText().toString();
        String piso = "";
        String cajon = "";

        if (cajonesDisponibles[0] > 0) {
            piso = "1";
            cajon = String.valueOf(7 - cajonesDisponibles[0]);
            cajonesDisponibles[0]--;
        } else if (cajonesDisponibles[1] > 0) {
            piso = "2";
            cajon = String.valueOf(7 - cajonesDisponibles[1]);
            cajonesDisponibles[1]--;
        } else if (cajonesDisponibles[2] > 0) {
            piso = "3";
            cajon = String.valueOf(5 - cajonesDisponibles[2]);
            cajonesDisponibles[2]--;
        } else {
            mostrarMensaje("No hay cajones disponibles.");
            return;
        }

        String fechaHora = obtenerFechaHoraActual();
        String[] registro = {piso, cajon, fechaHora};

        registros.put(placa, registro);

        int precio = obtenerPrecioPorPiso(Integer.parseInt(piso));
        String mensaje =
                "Placa: " + placa + "\n" +
                "Piso: " + piso + "\n" +
                "Cajón: " + cajon + "\n" +
                "Fecha y hora: " + fechaHora + "\n" +
                "Precio por hora: " + precio + " pesos";
        mostrarMensaje(mensaje);

        if (registros.containsKey(placa)) {
            mostrarToast("Se guardó con éxito el registro");
        } else {
            mostrarToast("Error al guardar el registro");
        }

    }
    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    public void buscarRegistro(View view) {
        String placa = editTextPlaca.getText().toString();
        String[] registro = registros.get(placa);

        if (registro != null) {
            String piso = registro[0];
            String cajon = registro[1];
            String fechaHora = registro[2];
            int precio = obtenerPrecioPorPiso(Integer.parseInt(piso));

            String mensaje = "Placa: " + placa + "\n" +
                    "Piso: " + piso + "\n" +
                    "Cajón: " + cajon + "\n" +
                    "Fecha y hora: " + fechaHora + "\n" +
                    "Precio por hora: " + precio + " pesos";

            mostrarMensaje(mensaje);
        } else {
            mostrarMensaje("No se encontró ningún registro para la placa ingresada.");
        }
    }

    public void generarPDF(View view) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(612, 792, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Paint paint = new Paint();
        android.graphics.Canvas canvas = page.getCanvas();

        paint.setTextSize(12);
        paint.setColor(Color.BLACK);

        int y = 30;

        for (Map.Entry<String, String[]> entry : registros.entrySet()) {
            String placa = entry.getKey();
            String piso = entry.getValue()[0];
            String cajon = entry.getValue()[1];
            String fechaHora = entry.getValue()[2];
            int precio = obtenerPrecioPorPiso(Integer.parseInt(piso));

            String registro = "Placa: " + placa + ", Piso: " + piso + ", Cajón: " + cajon + ", Fecha y hora: " + fechaHora + ", Precio por hora: " + precio + " pesos";
            canvas.drawText(registro, 10, y, paint);
            y += 20;
        }

        document.finishPage(page);

        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(directory, "registro_vehiculos.pdf");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            mostrarMensaje("PDF generado exitosamente. Ruta: " + file.getAbsolutePath());
            fos.close();
        } catch (IOException e) {
            mostrarMensaje("Error al generar el PDF.");
            e.printStackTrace();
        }

        document.close();
    }

    public void abrirPDF(View view) {
        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(directory, "registro_vehiculos.pdf");

        Uri pdfUri = FileProvider.getUriForFile(this, "com.example.app.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            mostrarMensaje("No se encontró una aplicación para abrir el PDF.");
        }
    }

    private void mostrarMensaje(String mensaje) {
        TextView textViewResult = findViewById(R.id.textViewResult);
        textViewResult.setText(mensaje);
    }


    private String obtenerFechaHoraActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private int obtenerPrecioPorPiso(int piso) {
        switch (piso) {
            case 1:
                return PRECIO_PISO_1;
            case 2:
                return PRECIO_PISO_2;
            case 3:
                return PRECIO_PISO_3;
            default:
                return 0;
        }
    }
    public void limpiarTextView(View view) {
        editTextPlaca.setText("");
        textViewResult.setText("");
        textViewResult.invalidate();
    }
}
