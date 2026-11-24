package com.support.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.agent.config.PromptLoader;
import com.support.agent.model.enums.Intent;
import com.support.agent.service.LlmClient;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Uses the LLM as supervisor when available, otherwise falls back to the deterministic router.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridIntentClassifier implements IntentClassifier {

    private final LlmClient llmClient;
    private final RuleBasedRouter ruleBasedRouter;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    @Override
    public RoutingDecision classify(String message) {
        if (llmClient.available()) {
            Optional<String> llmOutput = llmClient.complete(promptLoader.get("supervisor"), message);
            if (llmOutput.isPresent()) {
                Optional<RoutingDecision> parsed = parse(llmOutput.get());
                if (parsed.isPresent()) {
                    return parsed.get();
                }
                log.debug("Could not parse supervisor output, falling back to rules: {}", llmOutput.get());
            }
        }
        return ruleBasedRouter.route(message);
    }

    private Optional<RoutingDecision> parse(String raw) {
        String json = stripCodeFence(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            String intentText = node.path("intent").asText("").toUpperCase(Locale.ROOT);
            if (!StringUtils.hasText(intentText)) {
                return Optional.empty();
            }
            Intent intent = Intent.valueOf(intentText);
            double confidence = Math.max(0.0, Math.min(1.0, node.path("confidence").asDouble(0.85)));
            String reason = node.path("reason").asText("llm");
            return Optional.of(new RoutingDecision(intent, confidence, "llm:" + reason));
        } catch (Exception ex) {
            log.debug("Invalid supervisor JSON: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String stripCodeFence(String raw) {
        String value = raw.trim();
        if (value.startsWith("```")) {
            int firstLineEnd = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLineEnd > 0 && lastFence > firstLineEnd) {
                value = value.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return value;
    }
}
