package com.test.demo.dalle.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class DalleImageController {

    public static final String IMAGE_PAGE = "image";

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private OpenAiApiClient client;

    private String drawImageWithDallE(String prompt) throws Exception {
        GenerationRequest generation = GenerationRequest.defaultWith(prompt);
        String postBodyJson = jsonMapper.writeValueAsString(generation);
        String responseBody = client.postToOpenAiApi(postBodyJson, OpenAiApiClient.OpenAiService.DALL_E);
        GenerationResponse completionResponse = jsonMapper.readValue(responseBody, GenerationResponse.class);
        return completionResponse.firstImageUrl().orElseThrow();
    }

    @GetMapping(IMAGE_PAGE)
    public String paintImage() {
        return IMAGE_PAGE;
    }

    @PostMapping(IMAGE_PAGE)
    public String drawImage(Model model, FormInputDTO dto) throws Exception {
        model.addAttribute("request", dto.prompt());
        model.addAttribute("imageUri", drawImageWithDallE(dto.prompt()));
        return IMAGE_PAGE;
    }

}

record GenerationRequest(String prompt, int n, String size, String response_format) {

    public static GenerationRequest defaultWith(String prompt) {
        return new GenerationRequest(prompt, 1, "1024x1024", "url");
    }

}

record GenerationResponse(List<ImageUrl> data) {

    public Optional<String> firstImageUrl() {
        if (data == null || data.isEmpty())
            return Optional.empty();
        return Optional.of(data.get(0).url());
    }

    record ImageUrl(String url) {}

}

record FormInputDTO(String prompt) {}

@Component
class OpenAiApiClient {

    public enum OpenAiService {
        DALL_E, GPT_3;
    }

    @Value("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")
    private String openaiApiKey;

    private final HttpClient client = HttpClient.newHttpClient();

    public String postToOpenAiApi(String requestBodyAsJson, OpenAiService service)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(selectUri(service))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyAsJson))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private URI selectUri(OpenAiService service) {
        return URI.create(switch (service) {
            case DALL_E -> "https://api.openai.com/v1/images/generations";
            case GPT_3 -> "https://api.openai.com/v1/completions";
        });
    }

}
