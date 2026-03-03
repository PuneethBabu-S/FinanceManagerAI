package com.financemanagerai.genaisvc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
@Service
public class HuggingFaceService {

    @Value("${hf.api.key}")
    private String apiKey;

    @Value("${hf.api.url}")
    private String apiUrl;

    public String getCompletion(String userQuery) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String jsonBody = """
                {
                    "model": "deepseek-ai/DeepSeek-R1:fastest",
                    "messages": [
                        {"role": "user", "content": "%s"}
                    ]
                }
                """.formatted(userQuery);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "Error calling Hugging Face API: " + e.getMessage();
        }
    }
}