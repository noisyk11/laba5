package com.example.laba5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "AppPreferences"; // Имя файла настроек
    private static final String KEY_SHOW_POPUP = "ShowPopup"; // Ключ для хранения состояния показа всплывающего окна

    private String fileName; // Имя загруженного файла

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Скрываем кнопки "Открыть" и "Удалить" при запуске приложения
        findViewById(R.id.button_view).setVisibility(View.GONE);
        findViewById(R.id.button_delete).setVisibility(View.GONE);

        // Загружаем настройки и проверяем, нужно ли показывать всплывающее окно
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        boolean showPopup = preferences.getBoolean(KEY_SHOW_POPUP, true);

        if (showPopup) {
            // Используем отложенный вызов всплывающего окна, чтобы избежать ошибок при запуске
            findViewById(android.R.id.content).post(this::showPopupWindow);
        }
    }

    // Метод для отображения всплывающего окна
    private void showPopupWindow() {
        // Загружаем макет всплывающего окна
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        // Создаем PopupWindow с параметрами ширины и высоты
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // Делает окно модальным (не позволяет взаимодействовать с остальными элементами)
        );

        // Показываем окно в центре экрана
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        // Настраиваем элементы интерфейса всплывающего окна
        CheckBox checkBox = popupView.findViewById(R.id.checkBox); // Чекбокс для сохранения предпочтений
        Button buttonOk = popupView.findViewById(R.id.buttonOk);   // Кнопка подтверждения

        // Обработчик нажатия кнопки "ОК"
        buttonOk.setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                // Если чекбокс выбран, сохраняем в настройках, чтобы не показывать окно в будущем
                SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(KEY_SHOW_POPUP, false);
                editor.apply(); // Сохраняем изменения
            }
            popupWindow.dismiss(); // Закрываем окно
        });
    }

    // Метод для начала загрузки файла
    public void downloadFile(View view) {
        EditText editText = findViewById(R.id.editTextJournalId);
        String journalId = editText.getText().toString().trim(); // Получаем идентификатор журнала

        if (!journalId.isEmpty()) {
            // Формируем URL для загрузки файла
            String url = "https://ntv.ifmo.ru/file/journal/" + journalId + ".pdf";
            new DownloadFileTask().execute(url); // Запускаем асинхронную задачу загрузки
        } else {
            Toast.makeText(this, "Введите ID журнала", Toast.LENGTH_SHORT).show(); // Сообщение об ошибке
        }
    }

    // Асинхронная задача для загрузки файла
    private class DownloadFileTask extends AsyncTask<String, Void, Boolean> {
        private File downloadedFile; // Файл для сохранения

        @Override
        protected Boolean doInBackground(String... urls) {
            String url = urls[0]; // Получаем URL из параметров
            try {
                URL fileUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect(); // Устанавливаем соединение

                // Проверяем тип контента
                String contentType = connection.getContentType();
                if (contentType != null && contentType.contains("pdf")) {
                    InputStream inputStream = fileUrl.openStream(); // Поток для чтения данных
                    fileName = url.substring(url.lastIndexOf("/") + 1); // Извлекаем имя файла из URL

                    // Создаем директорию для сохранения файлов
                    File directory = new File(getExternalFilesDir(null), "DownloadedJournals");
                    if (!directory.exists()) {
                        directory.mkdirs(); // Создаем папку, если она не существует
                    }

                    downloadedFile = new File(directory, fileName); // Создаем файл в указанной директории
                    FileOutputStream outputStream = new FileOutputStream(downloadedFile); // Поток для записи данных

                    byte[] buffer = new byte[1024]; // Буфер для чтения данных
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length); // Записываем данные в файл
                    }

                    outputStream.close();
                    inputStream.close();
                    return true; // Успешная загрузка
                } else {
                    return false; // Тип контента не подходит
                }
            } catch (IOException e) {
                e.printStackTrace(); // Логируем ошибку
                return false; // Ошибка загрузки
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                // Если загрузка успешна, показываем кнопки для работы с файлом
                MainActivity.this.fileName = downloadedFile.getName();
                findViewById(R.id.button_view).setVisibility(View.VISIBLE);
                findViewById(R.id.button_delete).setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Файл загружен: " + downloadedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Ошибка загрузки файла", Toast.LENGTH_SHORT).show(); // Сообщение об ошибке
            }
        }
    }

    // Метод для открытия файла
    public void viewFile(View view) {
        File file = new File(getExternalFilesDir(null), "DownloadedJournals/" + fileName); // Путь к файлу
        if (file.exists()) {
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file); // Генерируем URI
            Intent intent = new Intent(Intent.ACTION_VIEW); // Создаем Intent для открытия файла
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent); // Запускаем активность для просмотра файла
            } catch (Exception e) {
                Toast.makeText(this, "Нет приложения для открытия PDF", Toast.LENGTH_SHORT).show(); // Сообщение, если нет подходящего приложения
            }
        } else {
            Toast.makeText(this, "Файл не существует", Toast.LENGTH_SHORT).show(); // Сообщение, если файл отсутствует
        }
    }

    // Метод для удаления файла
    public void deleteFile(View view) {
        File file = new File(getExternalFilesDir(null), "DownloadedJournals/" + fileName); // Путь к файлу
        if (file.exists()) {
            boolean deleted = file.delete(); // Удаляем файл
            if (deleted) {
                Toast.makeText(this, "Файл удалён", Toast.LENGTH_SHORT).show();
                findViewById(R.id.button_view).setVisibility(View.GONE);
                findViewById(R.id.button_delete).setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Не удалось удалить файл", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
        }
    }
}
