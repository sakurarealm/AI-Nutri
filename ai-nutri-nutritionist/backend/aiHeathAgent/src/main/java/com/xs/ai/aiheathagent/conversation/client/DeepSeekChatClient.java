package com.xs.ai.aiheathagent.conversation.client;

import com.xs.ai.aiheathagent.conversation.model.ConversationMessage;
import com.xs.ai.aiheathagent.conversation.model.ConversationRole;
import com.xs.ai.aiheathagent.conversation.model.MessageContentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DeepSeekChatClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String lightModel;
    private final String lightApiKey;
    private final String lightBaseUrl;
    private final double temperature;
    private final int maxOutputTokens;
    private final int lightMaxOutputTokens;

    public DeepSeekChatClient(
            RestTemplate restTemplate,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:}") String baseUrl,
            @Value("${deepseek.model:}") String model,
            @Value("${deepseek.light-model:}") String lightModel,
            @Value("${deepseek.light-api-key:}") String lightApiKey,
            @Value("${deepseek.light-base-url:}") String lightBaseUrl,
            @Value("${deepseek.temperature:0.7}") double temperature,
            @Value("${deepseek.max-output-tokens:0}") int maxOutputTokens,
            @Value("${deepseek.light-max-output-tokens:0}") int lightMaxOutputTokens
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.lightModel = (lightModel == null || lightModel.isBlank()) ? model : lightModel;
        this.lightApiKey = lightApiKey;
        this.lightBaseUrl = lightBaseUrl;
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.lightMaxOutputTokens = lightMaxOutputTokens;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && model != null && !model.isBlank();
    }

    public String complete(List<ConversationMessage> messages) {
        return complete(messages, null, null);
    }

    public String complete(List<ConversationMessage> messages, String modelOverride, Integer maxTokensOverride) {
        if (!isConfigured()) {
            throw new IllegalStateException("DeepSeek 未配置");
        }
        List<Map<String, Object>> payloadMessages = new ArrayList<>();
        for (ConversationMessage m : messages) {
            if (m == null || m.content() == null || m.content().type() != MessageContentType.TEXT) {
                continue;
            }
            String role = toRole(m.role());
            String content = String.valueOf(m.content().text() == null ? "" : m.content().text()).trim();
            if (content.isEmpty()) {
                continue;
            }
            payloadMessages.add(Map.of("role", role, "content", content));
        }

        Map<String, Object> requestBody = new HashMap<>();
        String selectedModel = String.valueOf(modelOverride == null ? "" : modelOverride).trim();
        if (selectedModel.isEmpty()) {
            selectedModel = model;
        }
        requestBody.put("model", selectedModel);
        requestBody.put("messages", payloadMessages);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", false);
        Integer maxTokens = resolveMaxTokens(maxTokensOverride, selectedModel);
        if (maxTokens != null && maxTokens > 0) {
            requestBody.put("max_tokens", maxTokens);
        }

        String selectedBaseUrl = resolveBaseUrl(selectedModel);
        String selectedApiKey = resolveApiKey(selectedModel);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (selectedApiKey != null && !selectedApiKey.isBlank()) {
            headers.setBearerAuth(selectedApiKey);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String apiUrl = String.valueOf(selectedBaseUrl == null ? "" : selectedBaseUrl).replaceAll("/+$", "") + "/v1/chat/completions";
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("DeepSeek API调用失败: " + response.getStatusCode());
        }
        return extractReply(response.getBody());
    }

    public String lightComplete(List<ConversationMessage> messages) {
        return complete(messages, lightModel, lightMaxOutputTokens > 0 ? lightMaxOutputTokens : null);
    }

    public String streamComplete(List<ConversationMessage> messages, Consumer<String> onDelta) {
        return streamComplete(messages, null, null, onDelta);
    }

    public String streamComplete(List<ConversationMessage> messages, String modelOverride, Integer maxTokensOverride, Consumer<String> onDelta) {
        if (!isConfigured()) {
            throw new IllegalStateException("DeepSeek 未配置");
        }
        Consumer<String> deltaConsumer = onDelta == null ? (s) -> {} : onDelta;

        List<Map<String, Object>> payloadMessages = new ArrayList<>();
        for (ConversationMessage m : messages) {
            if (m == null || m.content() == null || m.content().type() != MessageContentType.TEXT) {
                continue;
            }
            String role = toRole(m.role());
            String content = String.valueOf(m.content().text() == null ? "" : m.content().text()).trim();
            if (content.isEmpty()) {
                continue;
            }
            payloadMessages.add(Map.of("role", role, "content", content));
        }

        Map<String, Object> requestBody = new HashMap<>();
        String selectedModel = String.valueOf(modelOverride == null ? "" : modelOverride).trim();
        if (selectedModel.isEmpty()) {
            selectedModel = model;
        }
        requestBody.put("model", selectedModel);
        requestBody.put("messages", payloadMessages);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", true);
        Integer maxTokens = resolveMaxTokens(maxTokensOverride, selectedModel);
        if (maxTokens != null && maxTokens > 0) {
            requestBody.put("max_tokens", maxTokens);
        }

        String selectedBaseUrl = resolveBaseUrl(selectedModel);
        String selectedApiKey = resolveApiKey(selectedModel);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
        if (selectedApiKey != null && !selectedApiKey.isBlank()) {
            headers.setBearerAuth(selectedApiKey);
        }

        String apiUrl = String.valueOf(selectedBaseUrl == null ? "" : selectedBaseUrl).replaceAll("/+$", "") + "/v1/chat/completions";

        byte[] payloadBytes;
        try {
            payloadBytes = objectMapper.writeValueAsBytes(requestBody);
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 请求序列化失败");
        }

        StringBuilder full = new StringBuilder();
        ResponseExtractor<Void> extractor = (response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("DeepSeek API调用失败: " + response.getStatusCode());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String t = String.valueOf(line).trim();
                    if (t.isEmpty()) continue;
                    if (t.startsWith("data:")) {
                        String data = t.substring(5).trim();
                        if (data.isEmpty()) continue;
                        if ("[DONE]".equals(data)) break;
                        String delta = extractStreamDelta(data);
                        if (!delta.isEmpty()) {
                            full.append(delta);
                            try {
                                deltaConsumer.accept(delta);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                throw ex;
            }
            return null;
        };

        restTemplate.execute(
                apiUrl,
                HttpMethod.POST,
                (req) -> {
                    req.getHeaders().putAll(headers);
                    req.getBody().write(payloadBytes);
                    req.getBody().flush();
                },
                extractor
        );
        return full.toString();
    }

    private String extractStreamDelta(String jsonLine) {
        try {
            JsonNode root = objectMapper.readTree(String.valueOf(jsonLine));
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return "";
            JsonNode first = choices.get(0);
            String delta = first.path("delta").path("content").asText("");
            if (delta == null || delta.isEmpty()) {
                delta = first.path("message").path("content").asText("");
            }
            return String.valueOf(delta == null ? "" : delta);
        } catch (Exception ignored) {
            return "";
        }
    }

    private Integer resolveMaxTokens(Integer override, String selectedModel) {
        if (override != null) {
            return override;
        }
        String m = String.valueOf(selectedModel == null ? "" : selectedModel).trim();
        boolean isLight = !m.isEmpty() && m.equals(lightModel);
        int v = isLight ? lightMaxOutputTokens : maxOutputTokens;
        return v > 0 ? v : null;
    }

    private String resolveBaseUrl(String selectedModel) {
        String m = String.valueOf(selectedModel == null ? "" : selectedModel).trim();
        boolean isLight = !m.isEmpty() && m.equals(lightModel);
        String alt = isLight ? String.valueOf(lightBaseUrl == null ? "" : lightBaseUrl).trim() : "";
        if (!alt.isEmpty()) return alt;
        return baseUrl;
    }

    private String resolveApiKey(String selectedModel) {
        String m = String.valueOf(selectedModel == null ? "" : selectedModel).trim();
        boolean isLight = !m.isEmpty() && m.equals(lightModel);
        String alt = isLight ? String.valueOf(lightApiKey == null ? "" : lightApiKey).trim() : "";
        if (!alt.isEmpty()) return alt;
        return apiKey;
    }

    private String toRole(ConversationRole role) {
        if (role == null) {
            return "user";
        }
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }

    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> result) {
        if (result == null) {
            return "";
        }
        Object choicesObj = result.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return "";
        }
        Object messageObj = firstMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object contentObj = messageMap.get("content");
        return contentObj == null ? "" : String.valueOf(contentObj);
    }
}

