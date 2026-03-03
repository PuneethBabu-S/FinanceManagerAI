package com.financemanagerai.genaisvc.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String modelName;

    @Value("${gemini.api.url}")
    private String apiBaseUrl;

    public String getCompletion(String userQuery) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String jsonBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {"text": "%s"}
                      ]
                    }
                  ]
                }
                """.formatted(userQuery);

            // Build endpoint dynamically based on model
            String endpoint = apiBaseUrl + "/" + modelName + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());

            if (json.has("candidates")) {
                JSONArray candidates = json.getJSONArray("candidates");
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");
                return parts.getJSONObject(0).getString("text");
            } else if (json.has("error")) {
                return "Gemini API error: " + json.getJSONObject("error").getString("message");
            } else {
                return "Unexpected response: " + response.body();
            }

        } catch (Exception e) {
            return "Error calling Gemini API: " + e.getMessage();
        }
    }
}