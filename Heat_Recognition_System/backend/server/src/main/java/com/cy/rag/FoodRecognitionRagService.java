package com.cy.rag;

import com.cy.properties.RagProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRecognitionRagService {

    private static final String ALL = "ALL";

    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;

    private volatile List<RagKnowledgeDocument> knowledgeBase = List.of();

    @PostConstruct
    public void init() {
        if (!ragProperties.isEnabled()) {
            log.info("Food RAG is disabled by configuration");
            knowledgeBase = List.of();
            return;
        }

        ClassPathResource resource = new ClassPathResource(ragProperties.getResourcePath());
        if (!resource.exists()) {
            log.warn("Food RAG knowledge file not found: {}", ragProperties.getResourcePath());
            knowledgeBase = List.of();
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            List<RagKnowledgeDocument> documents = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<RagKnowledgeDocument>>() {}
            );
            if (documents == null) {
                documents = List.of();
            }

            List<RagKnowledgeDocument> normalized = new ArrayList<>();
            for (RagKnowledgeDocument document : documents) {
                if (document == null) {
                    continue;
                }
                normalizeDocument(document);
                if (StringUtils.hasText(document.getTitle()) && StringUtils.hasText(document.getContent())) {
                    normalized.add(document);
                }
            }

            knowledgeBase = normalized;
            log.info("Loaded {} local RAG knowledge documents", knowledgeBase.size());
        } catch (IOException e) {
            log.error("Failed to load local RAG knowledge base", e);
            knowledgeBase = List.of();
        }
    }

    public String augmentPrompt(String basePrompt, String mealType, String extraInfo) {
        String prompt = basePrompt;

        List<RagKnowledgeDocument> hits = retrieve(mealType, extraInfo);
        if (!hits.isEmpty()) {
            prompt = prompt + "\n\n" + buildKnowledgeBlock(hits);
        }

        if (StringUtils.hasText(extraInfo)) {
            prompt = prompt + "\n补充信息：" + extraInfo.trim();
        }

        return prompt;
    }

    private List<RagKnowledgeDocument> retrieve(String mealType, String extraInfo) {
        if (!ragProperties.isEnabled() || knowledgeBase.isEmpty()) {
            return List.of();
        }

        String normalizedMealType = normalizeMealType(mealType);
        String queryText = buildQueryText(normalizedMealType, extraInfo);
        int topK = Math.max(1, ragProperties.getTopK());

        List<RagKnowledgeDocument> results = knowledgeBase.stream()
                .map(document -> new ScoredDocument(document, score(document, normalizedMealType, queryText)))
                .filter(scoredDocument -> scoredDocument.score() > 0)
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed()
                        .thenComparing(scoredDocument -> scoredDocument.document().getTitle()))
                .limit(topK)
                .map(ScoredDocument::document)
                .toList();

        if (!results.isEmpty()) {
            log.debug("Food RAG hit docs: {}", results.stream().map(RagKnowledgeDocument::getTitle).toList());
            return results;
        }

        return knowledgeBase.stream()
                .filter(RagKnowledgeDocument::isAlwaysInclude)
                .limit(topK)
                .toList();
    }

    private int score(RagKnowledgeDocument document, String mealType, String queryText) {
        int score = 0;

        if (document.isAlwaysInclude()) {
            score += 18;
        }

        if (matchesMealType(document, mealType)) {
            score += 16;
        } else if (containsMealType(document, ALL)) {
            score += 8;
        }

        for (String keyword : safeList(document.getKeywords())) {
            String normalizedKeyword = normalize(keyword);
            if (StringUtils.hasText(normalizedKeyword) && queryText.contains(normalizedKeyword)) {
                score += 7;
            }
        }

        if (!StringUtils.hasText(queryText) && (document.isAlwaysInclude() || containsMealType(document, ALL))) {
            score += 4;
        }

        return score;
    }

    private String buildKnowledgeBlock(List<RagKnowledgeDocument> hits) {
        StringBuilder builder = new StringBuilder();
        builder.append("以下是与当前识别任务相关的参考知识，仅用于辅助估算：\n")
                .append("- 如果参考知识与图片可见内容冲突，以图片实际内容为准\n")
                .append("- 如果没有完全匹配的知识片段，请继续基于常见份量做合理估算\n")
                .append("- 最终仍然只输出既定 JSON，不要解释引用来源\n");

        for (int i = 0; i < hits.size(); i++) {
            RagKnowledgeDocument document = hits.get(i);
            builder.append("\n[参考")
                    .append(i + 1)
                    .append("] ")
                    .append(document.getTitle())
                    .append('\n')
                    .append(abbreviate(document.getContent()))
                    .append('\n');
        }

        return builder.toString().trim();
    }

    private String buildQueryText(String mealType, String extraInfo) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(mealType)) {
            builder.append(mealType).append(' ').append(toMealLabel(mealType)).append(' ');
        }
        if (StringUtils.hasText(extraInfo)) {
            builder.append(extraInfo);
        }
        return normalize(builder.toString());
    }

    private void normalizeDocument(RagKnowledgeDocument document) {
        document.setTitle(document.getTitle() == null ? "" : document.getTitle().trim());
        document.setContent(document.getContent() == null ? "" : document.getContent().trim());
        document.setMealTypes(safeList(document.getMealTypes()).stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .toList());
        document.setKeywords(safeList(document.getKeywords()).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList());
    }

    private boolean matchesMealType(RagKnowledgeDocument document, String mealType) {
        return StringUtils.hasText(mealType) && containsMealType(document, mealType);
    }

    private boolean containsMealType(RagKnowledgeDocument document, String mealType) {
        return safeList(document.getMealTypes()).contains(mealType);
    }

    private String abbreviate(String content) {
        int maxSnippetLength = Math.max(80, ragProperties.getMaxSnippetLength());
        if (content.length() <= maxSnippetLength) {
            return content;
        }
        return content.substring(0, maxSnippetLength) + "...";
    }

    private String normalizeMealType(String mealType) {
        if (!StringUtils.hasText(mealType)) {
            return "";
        }
        return mealType.trim().toUpperCase(Locale.ROOT);
    }

    private String toMealLabel(String mealType) {
        return switch (mealType) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            case "SNACK" -> "加餐";
            case "CUSTOM" -> "自定义餐次";
            default -> "";
        };
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replace('`', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replace('，', ' ')
                .replace('。', ' ')
                .replace('、', ' ')
                .replace('：', ' ')
                .replace('；', ' ')
                .replace('（', ' ')
                .replace('）', ' ')
                .replace('(', ' ')
                .replace(')', ' ')
                .replace('/', ' ')
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ScoredDocument(RagKnowledgeDocument document, int score) {
    }
}
