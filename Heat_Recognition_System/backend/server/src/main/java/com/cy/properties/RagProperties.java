package com.cy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cy.ai.rag")
public class RagProperties {

    /**
     * Whether lightweight local RAG is enabled.
     */
    private boolean enabled = true;

    /**
     * Maximum number of knowledge snippets appended to one prompt.
     */
    private int topK = 3;

    /**
     * Max characters per knowledge snippet to keep prompts compact.
     */
    private int maxSnippetLength = 240;

    /**
     * Classpath resource used as the local knowledge base.
     */
    private String resourcePath = "rag/food-knowledge.json";
}
