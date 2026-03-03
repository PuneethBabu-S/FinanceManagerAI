//package com.financemanagerai.genaisvc.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class OpenAiService {
//
//    private final WebClient webClient;
//
//    @Value("${openai.api.key}")
//    private String apiKey;
//
//    @Value("${openai.api.url}")
//    private String apiUrl;
//
//    public OpenAiService(WebClient.Builder builder) {
//        this.webClient = builder.build();
//    }
//
//    public String getCompletion(String userQuery) {
//        String response = webClient.post()
//                .uri(apiUrl)
//                .header("Authorization", "Bearer " + apiKey)
//                .header("Content-Type", "application/json")
//                .bodyValue(Map.of(
//                        "model", "gpt-3.5-turbo",
//                        "messages", List.of(
//                                Map.of("role", "user", "content", userQuery)
//                        )
//                ))
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//        return response; // Later, parse JSON to extract the actual text
//    }
//}