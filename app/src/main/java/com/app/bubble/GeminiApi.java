package com.app.bubble;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class GeminiApi {

    // The correct endpoint for the Gemini 2.0 Flash model, as you specified.
    private static final String API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    // A private constructor because this is a utility class and should not be instantiated.
    private GeminiApi() {}

    /**
     * Sends text to the Gemini API to be refined.
     * @param textToRefine The raw translated text from the app.
     * @param targetLanguage The language of the text to be refined (e.g., "Malayalam", "Spanish").
     * @param apiKey The user's personal Gemini API key from settings.
     * @return The refined text as a String, or null if an error occurs.
     */
    public static String refine(String textToRefine, String targetLanguage, String apiKey) {
        try {
            // 1. Construct the full URL with the user's API key.
            URL url = new URL(API_ENDPOINT + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // 2. Create a more specific prompt for the AI.
            // This prompt now explicitly states the target language for refinement to avoid ambiguity.
            String prompt = "You are an expert language assistant. Your task is to refine the following machine-translated text which is in " + targetLanguage + ". " +
                "Make it sound more natural, fluent, and grammatically perfect in " + targetLanguage + ", as if a native speaker wrote it. " +
                "Do not change the original meaning. Only provide the refined text as your answer, with no extra explanations or introductory phrases. " +
                "Here is the text: \"" + textToRefine + "\"";

            // 3. Build the JSON request body required by the Gemini API.
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contentsArray);

            String jsonInputString = requestBody.toString();

            // 4. Send the request to the API.
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 5. Read the response from the API.
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            connection.disconnect();

            // 6. Parse the JSON response to extract the refined text.
            JSONObject jsonResponse = new JSONObject(response.toString());
            String refinedText = jsonResponse.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

            return refinedText.trim();

        } catch (Exception e) {
            // If anything goes wrong (invalid API key, network error, etc.), log the error and return null.
            e.printStackTrace();
            return null;
        }
    }
}

