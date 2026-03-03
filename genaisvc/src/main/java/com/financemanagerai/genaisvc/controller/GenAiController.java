package com.financemanagerai.genaisvc.controller;

import com.financemanagerai.genaisvc.service.HuggingFaceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/genai")
public class GenAiController {

    private final HuggingFaceService hfService;

    public GenAiController(HuggingFaceService hfService) {
        this.hfService = hfService;
    }

    @PostMapping("/query")
    public Map<String, String> handleQuery(@RequestBody Map<String, String> request) {
        String userQuery = request.get("query");
        String aiResponse = hfService.getCompletion(userQuery);

        return Map.of(
                "query", userQuery,
                "response", aiResponse
        );
    }
}