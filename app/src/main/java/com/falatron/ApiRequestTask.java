package com.falatron;

import android.os.AsyncTask;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiRequestTask extends AsyncTask<String, Void, JSONObject> {

    private String apiKey;

    public ApiRequestTask(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected JSONObject doInBackground(String... urls) {
        String urlString = urls[0];
        HttpURLConnection urlConnection = null;
        JSONObject jsonResponse = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("x-api-key", apiKey);
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            jsonResponse = new JSONObject(response.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return jsonResponse;
    }

    // Método para cancelar a requisição
    public void cancelRequest() {
        cancel(true);
    }
}