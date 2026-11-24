package com.support.agent.service;

import com.support.agent.common.exception.BusinessException;
import com.support.agent.common.exception.ErrorCode;
import com.support.agent.config.RagProperties;
import com.support.agent.model.dto.chat.Citation;
import com.support.agent.model.dto.knowledge.ReindexResult;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Retrieval layer for the knowledge agent.
 *
 * <p>When an embedding model and pgvector store are configured it uses semantic search; otherwise
 * it falls back to a deterministic keyword/bigram scorer over the markdown files in
 * {@code src/main/resources/knowledge}. This keeps local development and evaluation runnable
 * without an external model provider.</p>
 */
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final Pattern TEXT_TOKEN = Pattern.compile("[\\p{IsHan}]+|[a-zA-Z0-9]{2,}");
    private static final Pattern PARAGRAPH = Pattern.compile("\\n\\s*\\n");
    private static final int MAX_SNIPPET_LENGTH = 320;

    private final RagProperties properties;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectProvider<EmbeddingStore<TextSegment>> embeddingStoreProvider;
    private final List<KnowledgeDoc> documents = new ArrayList<>();

    @PostConstruct
    void loadDocuments() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:" + properties.getKnowledgePath() + "/*.md");
        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                String filename = resource.getFilename() == null ? "unknown.md" : resource.getFilename();
                documents.add(new KnowledgeDoc(filename, extractTitle(content, filename), content));
            }
        }
        log.info("Loaded {} knowledge documents", documents.size());
    }

    public List<Citation> search(String query, Integer topK) {
        int limit = topK == null ? properties.getTopK() : topK;
        List<Citation> vectorMatches = vectorSearch(query, limit);
        if (!vectorMatches.isEmpty()) {
            return vectorMatches;
        }
        return keywordSearch(query, limit);
    }

    public ReindexResult reindex() {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingModel == null || embeddingStore == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "向量检索未启用，请配置 AI_ENABLED=true 且 RAG_VECTOR_STORE=pgvector");
        }

        List<TextSegment> segments = new ArrayList<>();
        for (KnowledgeDoc doc : documents) {
            for (String paragraph : paragraphs(doc.content())) {
                Metadata metadata = new Metadata()
                        .put("source", doc.source())
                        .put("title", doc.title());
                segments.add(TextSegment.from(paragraph, metadata));
            }
        }
        if (segments.isEmpty()) {
            return new ReindexResult(documents.size(), 0, Instant.now());
        }

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.removeAll();
        embeddingStore.addAll(embeddings, segments);
        log.info("Indexed {} knowledge segments from {} documents", segments.size(), documents.size());
        return new ReindexResult(documents.size(), segments.size(), Instant.now());
    }

    private List<Citation> vectorSearch(String query, int limit) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingModel == null || embeddingStore == null) {
            return List.of();
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(limit)
                    .minScore(properties.getMinScore())
                    .build();
            return embeddingStore.search(request).matches().stream()
                    .map(this::toCitation)
                    .collect(Collectors.toMap(
                            Citation::source,
                            citation -> citation,
                            (first, second) -> first,
                            java.util.LinkedHashMap::new))
                    .values().stream()
                    .limit(limit)
                    .toList();
        } catch (Exception ex) {
            log.warn("Vector search failed, falling back to keyword search: {}", ex.getMessage());
            return List.of();
        }
    }

    private Citation toCitation(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();
        return new Citation(
                metadata == null ? "unknown" : metadata.getString("source"),
                metadata == null ? "知识库" : metadata.getString("title"),
                truncate(segment.text(), MAX_SNIPPET_LENGTH),
                round(match.score()));
    }

    private List<Citation> keywordSearch(String query, int limit) {
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        List<ScoredChunk> scored = new ArrayList<>();
        java.util.Map<String, ScoredChunk> bestBySource = new java.util.LinkedHashMap<>();
        for (KnowledgeDoc doc : documents) {
            for (String paragraph : paragraphs(doc.content())) {
                String cleaned = clean(paragraph);
                if (cleaned.length() < 12) {
                    continue;
                }
                Set<String> chunkTokens = tokenize(doc.title() + " " + cleaned);
                long hits = queryTokens.stream().filter(chunkTokens::contains).count();
                if (hits == 0) {
                    continue;
                }
                double score = (double) hits / Math.max(1, Math.min(queryTokens.size(), 10));
                if (tokenize(doc.title()).stream().anyMatch(queryTokens::contains)) {
                    score += 0.25;
                }
                ScoredChunk chunk = new ScoredChunk(
                        new Citation(doc.source(), doc.title(), truncate(cleaned, MAX_SNIPPET_LENGTH),
                                round(Math.min(1.0, score))),
                        score);
                ScoredChunk current = bestBySource.get(doc.source());
                if (current == null || chunk.score() > current.score()) {
                    bestBySource.put(doc.source(), chunk);
                }
            }
        }
        scored.addAll(bestBySource.values());
        if (scored.isEmpty()) {
            return List.of();
        }

        List<ScoredChunk> ranked = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();
        // Drop weak matches so a slightly related document does not dilute the answer.
        double threshold = Math.max(0.2, ranked.get(0).score() * 0.5);
        return ranked.stream()
                .filter(chunk -> chunk.score() >= threshold)
                .limit(limit)
                .map(ScoredChunk::citation)
                .toList();
    }

    private List<String> paragraphs(String content) {
        List<String> result = new ArrayList<>();
        for (String raw : PARAGRAPH.split(content)) {
            String cleaned = clean(raw);
            if (cleaned.length() >= 12) {
                result.add(cleaned);
            }
        }
        if (result.isEmpty() && StringUtils.hasText(content)) {
            result.add(clean(content));
        }
        return result;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) {
            return tokens;
        }
        Matcher matcher = TEXT_TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.chars().allMatch(Character::isLetterOrDigit) && !containsHan(token)) {
                tokens.add(token);
                continue;
            }
            if (token.length() == 1) {
                tokens.add(token);
                continue;
            }
            for (int i = 0; i < token.length() - 1; i++) {
                tokens.add(token.substring(i, i + 2));
            }
            if (token.length() <= 4) {
                tokens.add(token);
            }
        }
        return tokens.stream().limit(60).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean containsHan(String value) {
        return value.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private String clean(String text) {
        return text.replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^[-*]\\s+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractTitle(String content, String filename) {
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return filename.replace(".md", "");
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record KnowledgeDoc(String source, String title, String content) {
    }

    private record ScoredChunk(Citation citation, double score) {
    }
}
