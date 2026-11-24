package com.support.agent.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads prompt templates from classpath:prompts/*.md at startup.
 */
@Component
public class PromptLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);
    private final Map<String, String> prompts = new LinkedHashMap<>();

    @PostConstruct
    void load() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:prompts/*.md");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                prompts.put(filename.replace(".md", ""), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        log.info("Loaded {} prompt templates: {}", prompts.size(), prompts.keySet());
    }

    public String get(String name) {
        String prompt = prompts.get(name);
        if (prompt == null) {
            log.warn("Prompt '{}' not found, using empty prompt", name);
            return "";
        }
        return prompt;
    }
}
